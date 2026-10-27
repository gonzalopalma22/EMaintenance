import os

RABBIT_HOST = os.getenv("RABBIT_HOST", "localhost")
RABBIT_PORT = int(os.getenv("RABBIT_PORT", "5672"))
RABBIT_USER = os.environ["RABBIT_USER"]
RABBIT_PASS = os.environ["RABBIT_PASS"]

QUEUE_ENTRADA = "mantenimiento.prediccion-entrada.queue"
EXCHANGE_EVENTOS = "mantenimiento.eventos.exchange"
RK_PREDICCION = "prediccion.generada"

MODELO_PATH = "modelo/modelo_anomalias_v2.joblib"
META_PATH = "modelo/modelo_meta_v2.json"

# Ciclos seguidos marcados como anomalía antes de avisar (con 3 s por ciclo, ~9 s)
CONSECUTIVAS = int(os.getenv("ANOMALIAS_CONSECUTIVAS", "3"))
# Las 3 lecturas de un mismo ciclo deben llegar dentro de esta ventana (segundos)
TOLERANCIA_CICLO_S = float(os.getenv("TOLERANCIA_CICLO_S", "1.5"))