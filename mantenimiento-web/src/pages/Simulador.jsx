import { useCallback, useEffect, useRef, useState } from "react";
import { useRoles } from "../useRoles";
import { useApi } from "../useApi";

const REFRESCO_MS = 3000;

function esVerdadero(v) {
  if (typeof v === "boolean") return v;
  if (typeof v === "string") return ["true", "falla", "en_falla"].includes(v.toLowerCase());
  return Boolean(v);
}

function normalizar(data) {
  if (!data) return [];
  if (Array.isArray(data)) {
    return data.map((item, i) => ({
      equipoId: item.equipoId ?? item.equipo ?? item.id ?? `equipo-${i}`,
      enFalla: esVerdadero(item.enFalla ?? item.falla ?? item.estado),
    }));
  }
  if (typeof data === "object") {
    return Object.entries(data).map(([equipoId, v]) => ({
      equipoId,
      enFalla: esVerdadero(v),
    }));
  }
  return [];
}

export default function Simulador() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const { esAdmin } = useRoles();

  const [equipos, setEquipos] = useState([]);
  const [error, setError] = useState(null);
  const [trabajando, setTrabajando] = useState(null);

  const cargar = useCallback(async () => {
    try {
      const data = await apiRef.current.get("/api/simulador/estado");
      setEquipos(normalizar(data));
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

  const accionar = async (equipoId, tipo) => {
    setTrabajando(equipoId);
    try {
      await apiRef.current.post(`/api/simulador/${tipo}/${encodeURIComponent(equipoId)}`);
      await cargar();
    } catch (e) {
      setError(e);
    } finally {
      setTrabajando(null);
    }
  };

  return (
    <section>
      <h2>Simulador de sensores</h2>
      {!esAdmin && (
        <p style={{ color: "#666" }}>
          Solo lectura: los botones de falla y normal requieren el rol Admin.
        </p>
      )}
      {error && <p style={{ color: "crimson" }}>Error: {error.message}</p>}

      <table style={{ borderCollapse: "collapse", width: "100%" }}>
        <thead>
          <tr>
            <th style={{ textAlign: "left", borderBottom: "1px solid #888", padding: "6px 10px" }}>
              Equipo
            </th>
            <th style={{ textAlign: "left", borderBottom: "1px solid #888", padding: "6px 10px" }}>
              Estado
            </th>
            {esAdmin && (
              <th style={{ textAlign: "left", borderBottom: "1px solid #888", padding: "6px 10px" }}>
                Acciones
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {equipos.map((e) => (
            <tr key={e.equipoId}>
              <td style={{ borderBottom: "1px solid #ddd", padding: "6px 10px" }}>{e.equipoId}</td>
              <td
                style={{
                  borderBottom: "1px solid #ddd",
                  padding: "6px 10px",
                  color: e.enFalla ? "crimson" : "green",
                  fontWeight: "bold",
                }}
              >
                {e.enFalla ? "EN FALLA" : "Normal"}
              </td>
              {esAdmin && (
                <td style={{ borderBottom: "1px solid #ddd", padding: "6px 10px" }}>
                  <span style={{ display: "flex", gap: 6 }}>
                    <button
                      disabled={trabajando === e.equipoId || e.enFalla}
                      onClick={() => accionar(e.equipoId, "falla")}
                    >
                      Falla
                    </button>
                    <button
                      disabled={trabajando === e.equipoId || !e.enFalla}
                      onClick={() => accionar(e.equipoId, "normal")}
                    >
                      Normal
                    </button>
                  </span>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
      {equipos.length === 0 && !error && <p>Sin datos.</p>}
    </section>
  );
}