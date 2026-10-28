import { useCallback, useEffect, useRef, useState } from "react";
import { useApi } from "../useApi";
import { useRoles } from "../useRoles";
import DataTable from "../components/DataTable";

const REFRESCO_MS = 5000;
const ESTADOS = ["ABIERTA", "RECONOCIDA"];
const UNIDADES = { temperatura: "°C", vibracion: "mm/s", presion: "bar" };
const NOMBRES_TIPO = {
  temperatura: "Temperatura",
  vibracion: "Vibración",
  presion: "Presión",
  ANOMALIA: "Anomalía (ML)",
};

// En las alertas ANOMALIA el sensorId viene como "modelo-ml/<sensor>"
function sensorCausante(a) {
  return String(a.sensorId ?? "").split("/")[1] || "varios sensores";
}

function unidadDe(a) {
  const sensor = a.tipo === "ANOMALIA" ? sensorCausante(a) : a.tipo;
  return UNIDADES[sensor] ?? "";
}

function mensajeDe(e) {
  const m = /"detail":"([^"]*)"/.exec(e?.message ?? "");
  return m ? m[1] : (e?.message ?? "Error desconocido");
}

const COLUMNAS = [
  { key: "generadaEn", titulo: "Generada", render: (v) => (v ? new Date(v).toLocaleString("es-CL") : "") },
  { key: "equipoId", titulo: "Equipo" },
  { key: "tipo", titulo: "Tipo", render: (v) => NOMBRES_TIPO[v] ?? v },
  {
    key: "sensorId",
    titulo: "Sensor",
    render: (v, a) => (a.tipo === "ANOMALIA" ? `Modelo ML · ${sensorCausante(a)}` : v),
  },
  {
    key: "valor",
    titulo: "Valor",
    render: (v, a) => (v == null ? "" : `${Number(v).toFixed(1)} ${unidadDe(a)}`.trim()),
  },
  {
    key: "umbral",
    titulo: "Umbral",
    render: (v, a) => {
      if (v == null) return "";
      // En las anomalías el "umbral" guardado es el score del modelo, no un valor del sensor
      return a.tipo === "ANOMALIA"
        ? `score ${Number(v).toFixed(2)}`
        : `${Number(v).toFixed(1)} ${unidadDe(a)}`.trim();
    },
  },
  { key: "severidad", titulo: "Severidad" },
  { key: "estado", titulo: "Estado" },
];

export default function Alertas() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const { esAdmin } = useRoles();

  const [estado, setEstado] = useState("");
  const [alertas, setAlertas] = useState([]);
  const [errorCarga, setErrorCarga] = useState(null);
  const [errorAccion, setErrorAccion] = useState(null);
  const [trabajando, setTrabajando] = useState(null);

  const cargar = useCallback(async () => {
    try {
      const qs = estado ? `?estado=${encodeURIComponent(estado)}` : "";
      const data = await apiRef.current.get(`/api/alertas${qs}`);
      setAlertas(Array.isArray(data) ? data : []);
      setErrorCarga(null);
    } catch (e) {
      setErrorCarga(e);
    }
  }, [estado]);

  useEffect(() => {
    cargar();
    const id = setInterval(cargar, REFRESCO_MS);
    return () => clearInterval(id);
  }, [cargar]);

  const reconocer = async (id) => {
    setTrabajando(id);
    setErrorAccion(null);
    try {
      await apiRef.current.patch(`/api/alertas/${id}/reconocer`);
      await cargar();
    } catch (e) {
      setErrorAccion(e);
    } finally {
      setTrabajando(null);
    }
  };

  const yaReconocida = (a) => /reconoc/i.test(String(a.estado ?? ""));

  return (
    <section>
      <h2>Alertas</h2>
      <div style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 12 }}>
        <label>
          Estado:{" "}
          <select value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todas</option>
            {ESTADOS.map((e) => (
              <option key={e} value={e}>
                {e}
              </option>
            ))}
          </select>
        </label>
        <button onClick={cargar}>Actualizar</button>
      </div>

      {errorCarga && (
        <p style={{ color: "crimson" }}>No se pudieron cargar las alertas: {mensajeDe(errorCarga)}</p>
      )}
      {errorAccion && (
        <p style={{ color: "crimson" }}>
          No se pudo reconocer la alerta: {mensajeDe(errorAccion)}{" "}
          <button onClick={() => setErrorAccion(null)}>Cerrar</button>
        </p>
      )}

      <DataTable
        rows={alertas}
        columns={COLUMNAS}
        renderActions={
          esAdmin
            ? (a) =>
                yaReconocida(a) ? (
                  "—"
                ) : (
                  <button disabled={trabajando === a.id} onClick={() => reconocer(a.id)}>
                    {trabajando === a.id ? "..." : "Reconocer"}
                  </button>
                )
            : undefined
        }
      />
    </section>
  );
}