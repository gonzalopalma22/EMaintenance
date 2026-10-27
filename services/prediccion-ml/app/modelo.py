import json
import time

import joblib
import pandas as pd

from app import config


class Predictor:
    def __init__(self):
        self.modelo = joblib.load(config.MODELO_PATH)
        with open(config.META_PATH) as f:
            meta = json.load(f)
        self.features = meta["features"]
        self.sensores = meta["sensores"]
        self.umbral = meta["umbral"]
        self.version = meta["version"]
        self.media = meta["media"]
        self.desviacion = meta["desviacion"]

        # equipo -> {tipo: (valor, evento_id, instante_de_llegada)}
        self.pendientes: dict[str, dict[str, tuple]] = {}
        # equipo -> {tipo: valor} del último ciclo completo
        self.ultimo_ciclo: dict[str, dict[str, float]] = {}
        # equipo -> ciclos seguidos marcados como anomalía
        self.consecutivas: dict[str, int] = {}

    def agregar(self, equipo_id: str, tipo: str, valor: float, evento_id: str | None):
        """Acumula la lectura. Devuelve un resultado solo cuando completa un ciclo de los 3 sensores."""
        if tipo not in self.sensores:
            return None

        ahora = time.monotonic()
        pend = self.pendientes.setdefault(equipo_id, {})

        # Lecturas que quedaron esperando de un ciclo anterior (por ejemplo, una inválida rechazada)
        for t in [t for t, (_, _, ts) in pend.items() if ahora - ts > config.TOLERANCIA_CICLO_S]:
            del pend[t]
        # Si el mismo sensor llega otra vez, empezó un ciclo nuevo
        if tipo in pend:
            pend.clear()

        pend[tipo] = (valor, evento_id, ahora)
        if len(pend) < len(self.sensores):
            return None

        ciclo = {s: pend[s][0] for s in self.sensores}
        ids = {s: pend[s][1] for s in self.sensores}
        pend.clear()

        # Features: valores y cambio respecto al ciclo anterior (0 en el primero, como en el entrenamiento)
        previo = self.ultimo_ciclo.get(equipo_id)
        fila = dict(ciclo)
        for s in self.sensores:
            fila[f"d_{s}"] = 0.0 if previo is None else ciclo[s] - previo[s]
        self.ultimo_ciclo[equipo_id] = ciclo

        X = pd.DataFrame([[fila[f] for f in self.features]], columns=self.features)
        score = float(self.modelo.decision_function(X)[0])

        # Anomalía solo tras N ciclos seguidos: un falso positivo aislado no alerta
        cruda = score < self.umbral
        n = self.consecutivas.get(equipo_id, 0) + 1 if cruda else 0
        self.consecutivas[equipo_id] = n
        anomalia = n >= config.CONSECUTIVAS

        # Sensor que más se aleja de lo normal
        z = {s: abs(ciclo[s] - self.media[s]) / self.desviacion[s] for s in self.sensores}
        causante = max(z, key=z.get)

        return {
            "eventoId": ids[causante],
            "valor": ciclo[causante],
            "score": round(score, 4),
            "delta": round(fila[f"d_{causante}"], 2),
            "anomalia": anomalia,
            "modeloVersion": self.version,
            "sensorCausante": causante,
        }