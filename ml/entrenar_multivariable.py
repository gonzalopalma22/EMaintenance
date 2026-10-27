import json

import joblib
import pandas as pd
from sklearn.ensemble import IsolationForest

SENSORES = ["temperatura", "vibracion", "presion"]

# Umbrales de alerta: los mismos de alertas.reglas en el application.yml de alertas
UMBRALES = {"temperatura": 100.0, "vibracion": 7.0, "presion": 8.0}

# Límite de lo "sano" para entrenar: sobre el ruido normal (83 / 3.5 / 6.3)
# y bajo los umbrales de alerta, para no aprender la degradación temprana como normal
NORMAL_MAX = {"temperatura": 90.0, "vibracion": 4.5, "presion": 7.0}

# ---------- 1. Cargar ----------
df = pd.read_csv("lecturas.csv")
df["timestamp"] = pd.to_datetime(df["timestamp"], utc=True)
df = df[df["tipo"].isin(SENSORES)]
print("Lecturas por tipo:")
print(df.groupby("tipo").size(), "\n")


# ---------- 2. Un registro por equipo y ciclo con los 3 sensores ----------
def serie(tipo):
    return (df[df["tipo"] == tipo][["equipo_id", "timestamp", "valor"]]
            .rename(columns={"valor": tipo})
            .sort_values("timestamp"))


ciclos = serie("temperatura")
for tipo in ["vibracion", "presion"]:
    ciclos = pd.merge_asof(ciclos, serie(tipo), on="timestamp", by="equipo_id",
                           direction="nearest", tolerance=pd.Timedelta("1500ms"))
ciclos = ciclos.dropna().sort_values(["equipo_id", "timestamp"]).reset_index(drop=True)

# ---------- 3. Features: valores y cambio respecto al ciclo anterior ----------
FEATURES = list(SENSORES)
for s in SENSORES:
    ciclos[f"d_{s}"] = ciclos.groupby("equipo_id")[s].diff().fillna(0)
    FEATURES.append(f"d_{s}")

# ---------- 4. Entrenar SOLO con ciclos sanos ----------
es_normal = pd.Series(True, index=ciclos.index)
for s in SENSORES:
    es_normal &= ciclos[s] <= NORMAL_MAX[s]
normales = ciclos[es_normal]

print("Ciclos completos:", len(ciclos), "| sanos para entrenar:", len(normales))
if len(normales) < 200:
    raise SystemExit("Muy pocos ciclos sanos (< 200). Deja correr más el simulador y exporta de nuevo.")

modelo = IsolationForest(n_estimators=200, contamination="auto", random_state=42)
modelo.fit(normales[FEATURES])

# Puntaje: menor = más anómalo. Umbral = percentil 1 de los puntajes sanos
ciclos["score"] = modelo.decision_function(ciclos[FEATURES])
umbral = float(pd.Series(modelo.decision_function(normales[FEATURES])).quantile(0.01))
ciclos["anomalia"] = ciclos["score"] < umbral

# ---------- 5. Evaluación ----------
sobre_umbral = pd.Series(False, index=ciclos.index)
for s in SENSORES:
    sobre_umbral |= ciclos[s] > UMBRALES[s]

fallas = ciclos[sobre_umbral]
previas = ciclos[~es_normal & ~sobre_umbral]   # zona intermedia: degradándose, aún sin alerta por umbral
falsos_pos = ciclos[es_normal & ciclos["anomalia"]]

print("\nUmbral de anomalía:", round(umbral, 4))
print("Anomalías detectadas:", int(ciclos["anomalia"].sum()))
print(ciclos[ciclos["anomalia"]].groupby("equipo_id")[SENSORES].agg(["count"]).iloc[:, :1])

if len(fallas):
    print(f"Ciclos sobre algún umbral de alerta: {len(fallas)} | detectados por el modelo: {fallas['anomalia'].mean():.0%}")
if len(previas):
    print(f"Ciclos en degradación (bajo umbrales): {len(previas)} | detectados: {previas['anomalia'].mean():.0%}  <- detección temprana")
print("Falsos positivos (ciclos sanos marcados):", len(falsos_pos))

print("\n10 ciclos más anómalos:")
print(ciclos.nsmallest(10, "score")[["equipo_id"] + SENSORES + ["score"]])

# ---------- 6. Guardar ----------
meta = {
    "version": "2",
    "features": FEATURES,
    "sensores": SENSORES,
    "umbral": umbral,
    "media": normales[FEATURES].mean().to_dict(),
    "desviacion": normales[FEATURES].std().replace(0, 1e-9).to_dict(),
}
joblib.dump(modelo, "modelo_anomalias_v2.joblib")
with open("modelo_meta_v2.json", "w") as f:
    json.dump(meta, f, indent=2)
print("\nGuardado: modelo_anomalias_v2.joblib y modelo_meta_v2.json")