import { useCallback, useEffect, useRef, useState } from "react";
import { useMsal } from "@azure/msal-react";
import { useApi } from "../useApi";
import DataTable from "../components/DataTable";

const REFRESCO_MS = 5000;
const ESTADOS = ["ABIERTA", "ASIGNADA", "EN_CURSO", "REALIZADA", "CERRADA"];

const ACCIONES_ADMIN = {
  ABIERTA: [
    { estado: "ASIGNADA", etiqueta: "Asignar" },
    { estado: "CERRADA", etiqueta: "Cancelar" },
  ],
  ASIGNADA: [{ estado: "CERRADA", etiqueta: "Cancelar" }],
  EN_CURSO: [],
  REALIZADA: [
    { estado: "CERRADA", etiqueta: "Cerrar" },
    { estado: "EN_CURSO", etiqueta: "Devolver" },
  ],
  CERRADA: [],
};

export default function Ordenes() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const { accounts } = useMsal();
  const usuarioActual = accounts[0]?.username ?? "";

  const [estado, setEstado] = useState("");
  const [ordenes, setOrdenes] = useState([]);
  const [error, setError] = useState(null);
  const [trabajando, setTrabajando] = useState(null);

  const cargar = useCallback(async () => {
    try {
      const qs = estado ? `?estado=${encodeURIComponent(estado)}` : "";
      const data = await apiRef.current.get(`/api/ordenes${qs}`);
      setOrdenes(Array.isArray(data) ? data : []);
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

  const cambiarEstado = async (orden, nuevoEstado) => {
    const body = { estado: nuevoEstado };

    if (nuevoEstado === "ASIGNADA") {
      const nombre = window.prompt(
        "Asignar a (correo exacto del empleado):",
        orden.asignadoA ?? usuarioActual
      );
      if (nombre === null) return;
      if (!nombre.trim()) {
        setError(new Error("Debes indicar a quién asignar la orden."));
        return;
      }
      body.asignadoA = nombre.trim();
    }

    setTrabajando(orden.id);
    try {
      await apiRef.current.patch(`/api/ordenes/${orden.id}/estado`, body);
      await cargar();
    } catch (e) {
      setError(e);
    } finally {
      setTrabajando(null);
    }
  };

  return (
    <section>
      <h2>Órdenes de trabajo</h2>
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

      {error && <p style={{ color: "crimson" }}>Error: {error.message}</p>}
      <DataTable
        rows={ordenes}
        renderActions={(o) => {
          const acciones = ACCIONES_ADMIN[o.estado] ?? [];
          if (acciones.length === 0) return "—";
          return (
            <span style={{ display: "flex", gap: 6 }}>
              {acciones.map((a) => (
                <button
                  key={a.estado}
                  disabled={trabajando === o.id}
                  onClick={() => cambiarEstado(o, a.estado)}
                >
                  {a.etiqueta}
                </button>
              ))}
            </span>
          );
        }}
      />
    </section>
  );
}