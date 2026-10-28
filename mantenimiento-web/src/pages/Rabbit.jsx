import { useCallback, useEffect, useRef, useState } from "react";
import { useRoles } from "../useRoles";
import { useApi } from "../useApi";
import DataTable from "../components/DataTable";

const VISTAS = {
  queues: { titulo: "Colas", ruta: "/api/queues" },
  exchanges: { titulo: "Exchanges", ruta: "/api/exchanges" },
  bindings: { titulo: "Bindings", ruta: "/api/bindings" },
  dlq: { titulo: "DLQ", ruta: null },
};

function aFilas(data) {
  if (!Array.isArray(data)) return [];
  return data.map((x) =>
    x !== null && typeof x === "object" ? x : { valor: String(x) }
  );
}

export default function Rabbit() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const { esAdmin } = useRoles();

  const [vista, setVista] = useState("queues");
  const [filas, setFilas] = useState([]);
  const [cola, setCola] = useState("");
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [trabajando, setTrabajando] = useState(false);

  const cargarLista = useCallback(async () => {
    const ruta = VISTAS[vista].ruta;
    if (!ruta) return;
    try {
      const data = await apiRef.current.get(ruta);
      setFilas(aFilas(data));
      setError(null);
    } catch (e) {
      setError(e);
    }
  }, [vista]);

  useEffect(() => {
    setFilas([]);
    setError(null);
    setInfo(null);
    if (VISTAS[vista].ruta) cargarLista();
  }, [vista, cargarLista]);

  const verMensajes = async () => {
    if (!cola.trim()) return;
    setTrabajando(true);
    setInfo(null);
    try {
      const data = await apiRef.current.get(
        `/api/dlq/${encodeURIComponent(cola.trim())}/mensajes`
      );
      setFilas(aFilas(data));
      setError(null);
    } catch (e) {
      setError(e);
    } finally {
      setTrabajando(false);
    }
  };

  const reprocesar = async () => {
    if (!cola.trim()) return;
    if (!window.confirm(`¿Reprocesar los mensajes de ${cola.trim()}?`)) return;
    setTrabajando(true);
    try {
      const r = await apiRef.current.post(
        `/api/dlq/${encodeURIComponent(cola.trim())}/reprocesar`
      );
      setInfo(r ? `Respuesta: ${typeof r === "string" ? r : JSON.stringify(r)}` : "Reprocesado.");
      setError(null);
      await verMensajes();
    } catch (e) {
      setError(e);
    } finally {
      setTrabajando(false);
    }
  };

  if (!esAdmin) {
    return <p>Esta sección requiere el rol Admin.</p>;
  }

  return (
    <section>
      <h2>Administración de RabbitMQ</h2>

      <nav style={{ display: "flex", gap: 8, marginBottom: 12 }}>
        {Object.entries(VISTAS).map(([clave, { titulo }]) => (
          <button key={clave} onClick={() => setVista(clave)} disabled={vista === clave}>
            {titulo}
          </button>
        ))}
      </nav>

      {vista === "dlq" ? (
        <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 12 }}>
          <input
            value={cola}
            onChange={(e) => setCola(e.target.value)}
            placeholder="Nombre de la cola DLQ"
            style={{ minWidth: 260 }}
          />
          <button onClick={verMensajes} disabled={trabajando || !cola.trim()}>
            Ver mensajes
          </button>
          <button onClick={reprocesar} disabled={trabajando || !cola.trim()}>
            Reprocesar
          </button>
        </div>
      ) : (
        <div style={{ marginBottom: 12 }}>
          <button onClick={cargarLista}>Actualizar</button>
        </div>
      )}

      {info && <p style={{ color: "green" }}>{info}</p>}
      {error && <p style={{ color: "crimson" }}>Error: {error.message}</p>}
      <DataTable rows={filas} />
    </section>
  );
}