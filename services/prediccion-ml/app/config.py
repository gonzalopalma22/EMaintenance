import os

RABBIT_HOST = os.getenv("RABBIT_HOST", "localhost")
RABBIT_PORT = int(os.getenv("RABBIT_PORT", "5672"))
RABBIT_USER = os.environ["RABBIT_USER"]
RABBIT_PASS = os.environ["RABBIT_PASS"]

QUEUE_ENTRADA = "mantenimiento.prediccion-entrada.queue"
EXCHANGE_EVENTOS = "mantenimiento.eventos.exchange"
RK_PREDICCION = "prediccion.generada"

MODELO_PATH = "modelo/modelo_anomalias.joblib"
META_PATH = "modelo/modelo_meta.json"