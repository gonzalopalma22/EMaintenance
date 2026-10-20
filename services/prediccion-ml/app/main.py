import asyncio
import json
import logging
from contextlib import asynccontextmanager
from datetime import datetime, timezone

import aio_pika
from fastapi import FastAPI, Depends

from app import config
from app.modelo import Predictor

# Importamos nuestro validador de Entra ID
from app.security import require_roles

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)s %(name)s : %(message)s")
log = logging.getLogger("prediccion-ml")

predictor = Predictor()
estado = {"conectado": False, "procesadas": 0, "rechazadas": 0}

async def procesar(message: aio_pika.IncomingMessage, exchange: aio_pika.Exchange):
    # requeue=False: si falla, RabbitMQ lo manda al DLX configurado en la cola
    async with message.process(requeue=False, ignore_processed=True):
        try:
            lectura = json.loads(message.body)
            equipo = lectura["equipoId"]
            valor = float(lectura["valor"])
            if lectura.get("tipo") != "temperatura":
                return
            resultado = predictor.predecir(equipo, valor)
        except (KeyError, ValueError, TypeError) as e:
            estado["rechazadas"] += 1
            log.warning("Lectura invalida -> DLQ. motivo=%s", e)
            raise  # sale del contexto: rechaza sin reencolar

        evento = {
            "lecturaEventId": lectura.get("eventId"),
            "equipoId": equipo,
            "valor": valor,
            "generadaEn": datetime.now(timezone.utc).isoformat(),
            **resultado,
        }
        await exchange.publish(
            aio_pika.Message(body=json.dumps(evento).encode(),
                             content_type="application/json",
                             delivery_mode=aio_pika.DeliveryMode.PERSISTENT),
            routing_key=config.RK_PREDICCION,
        )
        estado["procesadas"] += 1
        if resultado["anomalia"]:
            log.warning("ANOMALIA equipo=%s valor=%s score=%s",
                        equipo, valor, resultado["score"])
        else:
            log.info("OK equipo=%s valor=%s score=%s", equipo, valor, resultado["score"])


async def consumir():
    conn = await aio_pika.connect_robust(
        host=config.RABBIT_HOST, port=config.RABBIT_PORT,
        login=config.RABBIT_USER, password=config.RABBIT_PASS)
    channel = await conn.channel()
    await channel.set_qos(prefetch_count=10)
    exchange = await channel.get_exchange(config.EXCHANGE_EVENTOS)
    # passive: la cola la creó admin-rabbitmq, aquí solo se usa
    queue = await channel.declare_queue(config.QUEUE_ENTRADA, passive=True)
    estado["conectado"] = True
    log.info("Escuchando %s", config.QUEUE_ENTRADA)
    await queue.consume(lambda m: procesar(m, exchange))
    return conn


@asynccontextmanager
async def lifespan(app: FastAPI):
    conn = await consumir()
    yield
    await conn.close()


app = FastAPI(title="prediccion-ml", lifespan=lifespan)

# 1. Endpoint PÚBLICO (No pide token, igual que en Spring Boot)
@app.get("/health")
def health():
    return {"status": "UP" if estado["conectado"] else "DOWN", **estado,
            "modeloVersion": predictor.version}


# 2. Endpoint PROTEGIDO (Exige el token de Entra ID)
@app.get("/api/predicciones/estado")
async def estado_predicciones(usuario_token: dict = Depends(require_roles("admin", "user"))):
    # Si llega a esta línea, require_roles ya confirmó que el JWT de Microsoft es legítimo
    nombre = usuario_token.get("name", "Usuario")
    roles = usuario_token.get("roles", [])
    
    return {
        "mensaje": f"Acceso autorizado para {nombre}",
        "roles": roles,
        "estadisticas_motor": estado,
        "version_modelo": predictor.version
    }