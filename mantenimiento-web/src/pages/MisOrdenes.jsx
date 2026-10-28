import { useCallback, useEffect, useRef, useState } from "react";
import { useMsal } from "@azure/msal-react";
import { useApi } from "../useApi";
import DataTable from "../components/DataTable";

const REFRESCO_MS = 5000;

const ACCIONES_EMPLEADO = {
  ASIGNADA: [{ estado: "EN_CURSO", etiqueta: "Iniciar" }],
  EN_CURSO: [{ estado: "REALIZADA", etiqueta: "Marcar realizada" }],
};

export default function MisOrdenes() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const { accounts } = useMsal();
  const usuario = (accounts[0]?.username ?? "").trim().toLowerCase();

  const [ordenes, setOrdenes] = useState([]);
  const [error, setError] = useState(null);
  const [trabajando, setTrabajando] = useState(null);

  const cargar = useCallback(async () => {
    try {
      const data = await apiRef.current.get("/api/ordenes");
      const todas = Array.isArray(data) ? data : [];
      setOrdenes(
        todas.filter((o) => (o.asignadoA ?? "").trim().toLowerCase() === usuario)
      );
      setError(null);
    } catch (e) {
      setError(e);
    }
  }, [usuario]);

  useEffect(() => {
    cargar();
    const id = setInterval(cargar, REFRESCO_MS);
    return () => clearInterval(id);
  }, [cargar]);

  const cambiarEstado = async (orden, nuevoEstado) => {
    setTrabajando(orden.id);
    try {
      await apiRef.current.patch(`/api/ordenes/${orden.id}/estado`, {
        estado: nuevoEstado,
      });
      await cargar();
    } catch (e) {
      setError(e);
    } finally {
      setTrabajando(null);
    }
  };

  return (
    <section>
      <h2>Mis órdenes</h2>
      <div style={{ marginBottom: 12 }}>
        <button onClick={cargar}>Actualizar</button>
      </div>

      {error && <p style={{ color: "crimson" }}>Error: {error.message}</p>}
      {ordenes.length === 0 && !error && <p>No tienes órdenes asignadas.</p>}
      <DataTable
        rows={ordenes}
        renderActions={(o) => {
          const acciones = ACCIONES_EMPLEADO[o.estado] ?? [];
          if (acciones.length === 0) {
            return o.estado === "REALIZADA" ? "Esperando cierre" : "—";
          }
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