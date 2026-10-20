import json

import joblib
import pandas as pd
from sklearn.ensemble import IsolationForest

df = pd.read_csv("lecturas.csv", parse_dates=["timestamp"])
df = df[df["tipo"] == "temperatura"].sort_values(["equipo_id", "timestamp"])

# features: valor y su cambio respecto a la lectura anterior del mismo equipo
df["delta"] = df.groupby("equipo_id")["valor"].diff().fillna(0)
FEATURES = ["valor", "delta"]
X = df[FEATURES]

# Entrenar SOLO con datos normales (<= 100) para que lo anómalo destaque
normales = df[df["valor"] <= 100]
modelo = IsolationForest(n_estimators=200, contamination="auto", random_state=42)
modelo.fit(normales[FEATURES])

# Puntaje: menor = más anómalo
df["score"] = modelo.decision_function(X)

# Umbral: percentil 1 de los puntajes de datos normales
umbral = float(pd.Series(modelo.decision_function(normales[FEATURES])).quantile(0.01))
df["anomalia"] = df["score"] < umbral

print("Filas totales:", len(df), "| normales usadas:", len(normales))
print("Umbral de anomalía:", round(umbral, 4))
print("Anomalías detectadas:", int(df["anomalia"].sum()))
print(df[df["anomalia"]].groupby("equipo_id")["valor"].agg(["count", "min", "max"]))

# De las lecturas > 100 (fallas reales), cuántas detectó
fallas = df[df["valor"] > 100]
if len(fallas):
    recall = fallas["anomalia"].mean()
    print(f"Fallas reales (>100): {len(fallas)} | detectadas: {recall:.0%}")

# Falsos positivos: lecturas <= 100 marcadas como anomalía
fp = df[(df["valor"] <= 100) & df["anomalia"]]
print("Falsos positivos (<=100):", len(fp))

print("\n10 lecturas más anómalas:")
print(df.nsmallest(10, "score")[["equipo_id", "valor", "delta", "score"]])

joblib.dump(modelo, "modelo_anomalias.joblib")
with open("modelo_meta.json", "w") as f:
    json.dump({"features": FEATURES, "umbral": umbral, "version": "1"}, f)
print("\nGuardado: modelo_anomalias.joblib y modelo_meta.json")