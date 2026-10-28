import { useCallback, useEffect, useRef, useState } from "react";
import { useApi } from "../useApi";
import DataTable from "../components/DataTable";

const EQUIPOS = ["camion-01", "camion-02", "camion-03"];
const REFRESCO_MS = 3000;

export default function Lecturas() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const [equipoId, setEquipoId] = useState(EQUIPOS[0]);
  const [lecturas, setLecturas] = useState([]);
  const [error, setError] = useState(null);
  const [auto, setAuto] = useState(true);

  const cargar = useCallback(async () => {
    try {
      const data = await apiRef.current.get(
        `/api/lecturas?equipoId=${encodeURIComponent(equipoId)}`
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

  return (
    <section>
      <h2>Lecturas</h2>
      <div style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 12 }}>
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

      {error && <p style={{ color: "crimson" }}>Error: {error.message}</p>}
      <DataTable rows={lecturas} />
    </section>
  );
}