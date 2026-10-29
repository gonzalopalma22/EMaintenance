import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useApi } from "../useApi";
import DataTable from "../components/DataTable";

const EQUIPOS = ["camion-01", "camion-02", "camion-03"];
const REFRESCO_MS = 3000;
const VENTANA = 300; // lecturas que se piden al backend (unas 100 por sensor)
const PUNTOS_GRAFICO = 60;
const FILAS_TABLA = 50;

// Los umbrales son una copia de alertas.reglas (application.yml del servicio alertas).
// Si cambias allá un umbral, cámbialo también aquí.
const SENSORES = [
  { tipo: "temperatura", nombre: "Temperatura", unidad: "°C", umbral: 100 },
  { tipo: "vibracion", nombre: "Vibración", unidad: "mm/s", umbral: 7 },
  { tipo: "presion", nombre: "Presión", unidad: "bar", umbral: 8 },
];
const SENSOR_POR_TIPO = Object.fromEntries(SENSORES.map((s) => [s.tipo, s]));

function mensajeDe(e) {
  const m = /"detail":"([^"]*)"/.exec(e?.message ?? "");
  return m ? m[1] : (e?.message ?? "Error desconocido");
}

function Tendencia({ valores, umbral }) {
  if (valores.length < 2) return null;
  const W = 220;
  const H = 50;
  const P = 4;
  let min = Math.min(...valores, umbral);
  let max = Math.max(...valores, umbral);
  if (max === min) max = min + 1;
  const x = (i) => P + (i * (W - 2 * P)) / (valores.length - 1);
  const y = (v) => H - P - ((v - min) * (H - 2 * P)) / (max - min);
  const puntos = valores.map((v, i) => `${x(i).toFixed(1)},${y(v).toFixed(1)}`).join(" ");

  return (
    <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={H} role="img" aria-label="Tendencia">
      <line
        x1={P}
        x2={W - P}
        y1={y(umbral)}
        y2={y(umbral)}
        stroke="var(--danger)"
        strokeDasharray="4 3"
        strokeWidth="1"
      />
      <polyline
        points={puntos}
        fill="none"
        stroke="var(--primary)"
        strokeWidth="1.8"
        strokeLinejoin="round"
        strokeLinecap="round"
      />
    </svg>
  );
}

const COLUMNAS = [
  {
    key: "timestamp",
    titulo: "Fecha",
    render: (v) => (v ? new Date(v).toLocaleString("es-CL") : ""),
  },
  { key: "tipo", titulo: "Sensor", render: (v) => SENSOR_POR_TIPO[v]?.nombre ?? v },
  {
    key: "valor",
    titulo: "Valor",
    render: (v, l) =>
      v == null ? "" : `${Number(v).toFixed(1)} ${SENSOR_POR_TIPO[l.tipo]?.unidad ?? ""}`.trim(),
  },
];

export default function Lecturas() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const [equipoId, setEquipoId] = useState(EQUIPOS[0]);
  const [tipo, setTipo] = useState("");
  const [lecturas, setLecturas] = useState([]);
  const [error, setError] = useState(null);
  const [auto, setAuto] = useState(true);

  const cargar = useCallback(async () => {
    try {
      const data = await apiRef.current.get(
        `/api/lecturas?equipoId=${encodeURIComponent(equipoId)}&limite=${VENTANA}`
      );
      setLecturas(Array.isArray(data) ? data : []);
      setError(null);
    } catch (e) {
      setError(e);
    }
  }, [equipoId]);

  useEffect(() => {
    cargar();
    if (!auto) return;
    const id = setInterval(cargar, REFRESCO_MS);
    return () => clearInterval(id);
  }, [cargar, auto]);

  // El backend devuelve de la más nueva a la más antigua
  const porSensor = useMemo(() => {
    const grupos = Object.fromEntries(SENSORES.map((s) => [s.tipo, []]));
    for (const l of lecturas) grupos[l.tipo]?.push(l);
    return grupos;
  }, [lecturas]);

  const filasTabla = (tipo ? lecturas.filter((l) => l.tipo === tipo) : lecturas).slice(
    0,
    FILAS_TABLA
  );

  return (
    <section>
      <h2>Lecturas</h2>
      <div
        style={{
          display: "flex",
          gap: 12,
          alignItems: "center",
          flexWrap: "wrap",
          marginBottom: 12,
        }}
      >
        <label>
          Equipo:{" "}
          <select value={equipoId} onChange={(e) => setEquipoId(e.target.value)}>
            {EQUIPOS.map((e) => (
              <option key={e} value={e}>
                {e}
              </option>
            ))}
          </select>
        </label>
        <label>
          <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} />{" "}
          Refresco automático ({REFRESCO_MS / 1000} s)
        </label>
        <button onClick={cargar}>Actualizar</button>
      </div>

      {error && (
        <p style={{ color: "crimson" }}>No se pudieron cargar las lecturas: {mensajeDe(error)}</p>
      )}

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
          gap: 12,
          marginBottom: 16,
        }}
      >
        {SENSORES.map((s) => {
          const filas = porSensor[s.tipo];
          const ultima = filas[0];
          const valores = filas
            .slice(0, PUNTOS_GRAFICO)
            .map((l) => Number(l.valor))
            .reverse();
          const sobreUmbral = ultima && Number(ultima.valor) > s.umbral;

          return (
            <div
              key={s.tipo}
              style={{
                padding: 12,
                border: "1px solid var(--border)",
                borderRadius: 8,
                background: "var(--bg)",
              }}
            >
              <div style={{ color: "var(--muted)", fontSize: 13 }}>{s.nombre}</div>
              <div
                style={{
                  fontSize: 28,
                  fontWeight: 700,
                  color: sobreUmbral ? "var(--danger)" : "var(--text)",
                }}
              >
                {ultima ? `${Number(ultima.valor).toFixed(1)} ${s.unidad}` : "—"}
              </div>
              <div style={{ color: "var(--muted)", fontSize: 12, marginBottom: 6 }}>
                {ultima
                  ? `Umbral de alerta: ${s.umbral} ${s.unidad} · ${new Date(
                      ultima.timestamp
                    ).toLocaleTimeString("es-CL")}`
                  : "Sin lecturas"}
              </div>
              <Tendencia valores={valores} umbral={s.umbral} />
            </div>
          );
        })}
      </div>

      <div style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 12 }}>
        <label>
          Sensor:{" "}
          <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
            <option value="">Todos</option>
            {SENSORES.map((s) => (
              <option key={s.tipo} value={s.tipo}>
                {s.nombre}
              </option>
            ))}
          </select>
        </label>
        <span style={{ color: "var(--muted)" }}>Últimas {FILAS_TABLA} lecturas</span>
      </div>

      <DataTable rows={filasTabla} columns={COLUMNAS} />
    </section>
  );
}