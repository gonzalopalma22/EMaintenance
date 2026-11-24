import { useCallback, useEffect, useRef, useState } from "react";
import { useApi } from "../useApi";
import DataTable from "../components/DataTable";

const REFRESCO_MS = 5000;
const UNIDADES = { temperatura: "°C", vibracion: "mm/s", presion: "bar" };
const NOMBRES_TIPO = {
  temperatura: "Temperatura",
  vibracion: "Vibración",
  presion: "Presión",
};

function mensajeDe(e) {
  const m = /"detail":"([^"]*)"/.exec(e?.message ?? "");
  return m ? m[1] : (e?.message ?? "Error desconocido");
}

function conUnidad(v, r) {
  if (v == null) return "";
  return `${Number(v).toFixed(1)} ${UNIDADES[r.tipo] ?? ""}`.trim();
}

const COLUMNAS = [
  { key: "equipoId", titulo: "Equipo" },
  { key: "tipo", titulo: "Sensor", render: (v) => NOMBRES_TIPO[v] ?? v },
  { key: "muestras", titulo: "Muestras" },
  { key: "promedio", titulo: "Promedio", render: conUnidad },
  { key: "minimo", titulo: "Mínimo", render: conUnidad },
  { key: "maximo", titulo: "Máximo", render: conUnidad },
  { key: "ultimo", titulo: "Último", render: conUnidad },
  {
    key: "calculadoEn",
    titulo: "Calculado",
    render: (v) => (v ? new Date(v).toLocaleTimeString("es-CL") : ""),
  },
];

export default function Analitica() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const [resumenes, setResumenes] = useState([]);
  const [estado, setEstado] = useState(null);
  const [equipo, setEquipo] = useState("");
  const [error, setError] = useState(null);

  const cargar = useCallback(async () => {
    try {
      const [res, est] = await Promise.all([
        apiRef.current.get("/api/analitica/resumen"),
        apiRef.current.get("/api/analitica/estado"),
      ]);
      setResumenes(Array.isArray(res) ? res : []);
      setEstado(est);
      setError(null);
    } catch (e) {
      setError(e);
    }
  }, []);

  useEffect(() => {
    cargar();
    const id = setInterval(cargar, REFRESCO_MS);
    return () => clearInterval(id);
  }, [cargar]);

  const equipos = [...new Set(resumenes.map((r) => r.equipoId))].sort();
  const filas = equipo ? resumenes.filter((r) => r.equipoId === equipo) : resumenes;
  const ventana = resumenes[0]?.ventanaSegundos ?? estado?.ventanaSegundos;

  return (
    <section>
      <h2>Analítica en streaming (Kafka)</h2>
      <p style={{ color: "var(--muted)" }}>
        Estadísticas de los últimos {ventana ? Math.round(ventana / 60) : "—"} minutos por equipo y
        sensor, calculadas a partir del tópico <code>telemetria.lecturas</code>.
      </p>

      <div style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 12 }}>
        <label>
          Equipo:{" "}
          <select value={equipo} onChange={(e) => setEquipo(e.target.value)}>
            <option value="">Todos</option>
            {equipos.map((e) => (
              <option key={e} value={e}>
                {e}
              </option>
            ))}
          </select>
        </label>
        <button onClick={cargar}>Actualizar</button>
      </div>

      {estado && (
        <p style={{ color: "var(--muted)" }}>
          Lecturas consumidas: <strong>{estado.lecturasConsumidas}</strong> · Descartadas (DLT):{" "}
          <strong>{estado.lecturasDescartadas}</strong> · Resúmenes publicados:{" "}
          <strong>{estado.resumenesPublicados}</strong>
        </p>
      )}

      {error && (
        <p style={{ color: "crimson" }}>No se pudo cargar la analítica: {mensajeDe(error)}</p>
      )}

      <DataTable rows={filas} columns={COLUMNAS} />
    </section>
  );
}