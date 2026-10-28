import { useCallback, useEffect, useRef, useState } from "react";
import { useApi } from "../useApi";
import { useRoles } from "../useRoles";
import DataTable from "../components/DataTable";

const REFRESCO_MS = 5000;

export default function Alertas() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const { esAdmin } = useRoles();

  const [estado, setEstado] = useState("");
  const [alertas, setAlertas] = useState([]);
  const [error, setError] = useState(null);
  const [trabajando, setTrabajando] = useState(null);

  const cargar = useCallback(async () => {
    try {
      const qs = estado ? `?estado=${encodeURIComponent(estado)}` : "";
      const data = await apiRef.current.get(`/api/alertas${qs}`);
      setAlertas(Array.isArray(data) ? data : []);
      setError(null);
    } catch (e) {
      setError(e);
    }
  }, [estado]);

  useEffect(() => {
    cargar();
    const id = setInterval(cargar, REFRESCO_MS);
    return () => clearInterval(id);
  }, [cargar]);

  const reconocer = async (id) => {
    setTrabajando(id);
    try {
      await apiRef.current.patch(`/api/alertas/${id}/reconocer`);
      await cargar();
    } catch (e) {
      setError(e);
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
          Estado (filtro):{" "}
          <input
            value={estado}
            onChange={(e) => setEstado(e.target.value)}
            placeholder="vacío = todas"
          />
        </label>
        <button onClick={cargar}>Actualizar</button>
      </div>

      {error && <p style={{ color: "crimson" }}>Error: {error.message}</p>}
      <DataTable
        rows={alertas}
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