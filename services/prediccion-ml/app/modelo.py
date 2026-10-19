import json

import joblib
import pandas as pd

from app import config


class Predictor:
    def __init__(self):
        self.modelo = joblib.load(config.MODELO_PATH)
        with open(config.META_PATH) as f:
            meta = json.load(f)
        self.features = meta["features"]
        self.umbral = meta["umbral"]
        self.version = meta["version"]
        self.ultimo_valor: dict[str, float] = {}

    def predecir(self, equipo_id: str, valor: float) -> dict:
        previo = self.ultimo_valor.get(equipo_id)
        delta = 0.0 if previo is None else valor - previo
        self.ultimo_valor[equipo_id] = valor

        X = pd.DataFrame([[valor, delta]], columns=self.features)
        score = float(self.modelo.decision_function(X)[0])
        return {
            "score": round(score, 4),
            "delta": round(delta, 2),
            "anomalia": score < self.umbral,
            "modeloVersion": self.version,
        }