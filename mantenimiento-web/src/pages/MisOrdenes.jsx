import { useCallback, useEffect, useRef, useState } from "react";
import { useApi } from "../useApi";
import DataTable from "../components/DataTable";

const REFRESCO_MS = 5000;

const ACCIONES_EMPLEADO = {
  ASIGNADA: [{ estado: "EN_CURSO", etiqueta: "Iniciar" }],
  EN_CURSO: [{ estado: "REALIZADA", etiqueta: "Marcar realizada" }],
};

const fecha = (v) => (v ? new Date(v).toLocaleString("es-CL") : "—");

function mensajeDe(e) {
  const m = /"detail":"([^"]*)"/.exec(e?.message ?? "");
  return m ? m[1] : (e?.message ?? "Error desconocido");
}

const COLUMNAS = [
  { key: "creadaEn", titulo: "Creada", render: fecha },
  { key: "equipoId", titulo: "Equipo" },
  { key: "descripcion", titulo: "Descripción" },
  { key: "prioridad", titulo: "Prioridad" },
  { key: "estado", titulo: "Estado" },
  { key: "cerradaEn", titulo: "Cerrada", render: fecha },
];

export default function MisOrdenes() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const [ordenes, setOrdenes] = useState([]);
  const [errorCarga, setErrorCarga] = useState(null);
  const [errorAccion, setErrorAccion] = useState(null);
  const [trabajando, setTrabajando] = useState(null);
  const [cargado, setCargado] = useState(false);

  const cargar = useCallback(async () => {
    try {
      // El backend ya devuelve solo las órdenes asignadas al usuario del token
      const data = await apiRef.current.get("/api/ordenes");
      setOrdenes(Array.isArray(data) ? data : []);
      setErrorCarga(null);
    } catch (e) {
      setErrorCarga(e);
    } finally {
      setCargado(true);
    }
  }, []);

  useEffect(() => {
    cargar();
    const id = setInterval(cargar, REFRESCO_MS);
    return () => clearInterval(id);
  }, [cargar]);

  const cambiarEstado = async (orden, nuevoEstado) => {
    setTrabajando(orden.id);
    setErrorAccion(null);
    try {
      await apiRef.current.patch(`/api/ordenes/${orden.id}/estado`, {
        estado: nuevoEstado,
      });
      await cargar();
    } catch (e) {
      setErrorAccion(e);
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

      {errorCarga && (
        <p style={{ color: "crimson" }}>No se pudieron cargar tus órdenes: {mensajeDe(errorCarga)}</p>
      )}
      {errorAccion && (
        <p style={{ color: "crimson" }}>
          No se pudo actualizar la orden: {mensajeDe(errorAccion)}{" "}
          <button onClick={() => setErrorAccion(null)}>Cerrar</button>
        </p>
      )}
      {cargado && ordenes.length === 0 && !errorCarga && <p>No tienes órdenes asignadas.</p>}

      <DataTable
        rows={ordenes}
        columns={COLUMNAS}
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