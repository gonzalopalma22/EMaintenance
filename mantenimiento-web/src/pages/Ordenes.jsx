import { useCallback, useEffect, useRef, useState } from "react";
import { useApi } from "../useApi";
import DataTable from "../components/DataTable";

const REFRESCO_MS = 5000;
const ESTADOS = ["ABIERTA", "ASIGNADA", "EN_CURSO", "REALIZADA", "CERRADA"];

// Mismas transiciones que permite el backend para el Admin
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

const fecha = (v) => (v ? new Date(v).toLocaleString("es-CL") : "—");

function mensajeDe(e) {
  const m = /"detail":"([^"]*)"/.exec(e?.message ?? "");
  return m ? m[1] : (e?.message ?? "Error desconocido");
}

export default function Ordenes() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const [estado, setEstado] = useState("");
  const [ordenes, setOrdenes] = useState([]);
  const [errorCarga, setErrorCarga] = useState(null);
  const [errorAccion, setErrorAccion] = useState(null);
  const [trabajando, setTrabajando] = useState(null);

  const [tecnicos, setTecnicos] = useState([]);
  const [errorTecnicos, setErrorTecnicos] = useState(null);
  const [asignando, setAsignando] = useState(null);
  const [tecnicoSel, setTecnicoSel] = useState("");

  const cargar = useCallback(async () => {
    try {
      const qs = estado ? `?estado=${encodeURIComponent(estado)}` : "";
      const data = await apiRef.current.get(`/api/ordenes${qs}`);
      setOrdenes(Array.isArray(data) ? data : []);
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

  useEffect(() => {
    apiRef.current
      .get("/api/ordenes/tecnicos")
      .then((d) => setTecnicos(Array.isArray(d) ? d : []))
      .catch((e) => setErrorTecnicos(e));
  }, []);

  const nombrePorCorreo = new Map(
    tecnicos.map((t) => [String(t.correo).toLowerCase(), t.nombre])
  );

  const columnas = [
    { key: "creadaEn", titulo: "Creada", render: fecha },
    { key: "equipoId", titulo: "Equipo" },
    { key: "descripcion", titulo: "Descripción" },
    { key: "prioridad", titulo: "Prioridad" },
    { key: "estado", titulo: "Estado" },
    {
      key: "asignadoA",
      titulo: "Asignada a",
      render: (v) => (v ? nombrePorCorreo.get(String(v).toLowerCase()) || v : "—"),
    },
    { key: "cerradaEn", titulo: "Cerrada", render: fecha },
  ];

  const enviarCambio = async (orden, body) => {
    setTrabajando(orden.id);
    setErrorAccion(null);
    try {
      await apiRef.current.patch(`/api/ordenes/${orden.id}/estado`, body);
      await cargar();
    } catch (e) {
      setErrorAccion(e);
    } finally {
      setTrabajando(null);
    }
  };

  const accion = (orden, nuevoEstado, etiqueta) => {
    if (nuevoEstado === "ASIGNADA") {
      setAsignando(orden);
      setTecnicoSel(orden.asignadoA ?? "");
      return;
    }
    if (etiqueta === "Cancelar" && !window.confirm("¿Cancelar esta orden? Pasará a CERRADA.")) {
      return;
    }
    enviarCambio(orden, { estado: nuevoEstado });
  };

  const confirmarAsignacion = async () => {
    if (!tecnicoSel) return;
    const orden = asignando;
    setAsignando(null);
    await enviarCambio(orden, { estado: "ASIGNADA", asignadoA: tecnicoSel });
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

      {asignando && (
        <div
          style={{
            display: "flex",
            gap: 12,
            alignItems: "center",
            flexWrap: "wrap",
            marginBottom: 12,
            padding: 12,
            border: "1px solid var(--border)",
            borderRadius: 8,
          }}
        >
          <span>
            Asignar la orden de <strong>{asignando.equipoId}</strong> a:
          </span>
          <select value={tecnicoSel} onChange={(e) => setTecnicoSel(e.target.value)}>
            <option value="">Selecciona un técnico</option>
            {tecnicos.map((t) => (
              <option key={t.correo} value={t.correo}>
                {t.nombre ? `${t.nombre} (${t.correo})` : t.correo}
              </option>
            ))}
          </select>
          <button disabled={!tecnicoSel} onClick={confirmarAsignacion}>
            Confirmar
          </button>
          <button onClick={() => setAsignando(null)}>Volver</button>
          {tecnicos.length === 0 && !errorTecnicos && (
            <span style={{ color: "var(--muted)" }}>No hay técnicos con el rol Usuario.</span>
          )}
        </div>
      )}

      {errorTecnicos && (
        <p style={{ color: "crimson" }}>
          No se pudo cargar la lista de técnicos: {mensajeDe(errorTecnicos)}
        </p>
      )}
      {errorCarga && (
        <p style={{ color: "crimson" }}>No se pudieron cargar las órdenes: {mensajeDe(errorCarga)}</p>
      )}
      {errorAccion && (
        <p style={{ color: "crimson" }}>
          No se pudo actualizar la orden: {mensajeDe(errorAccion)}{" "}
          <button onClick={() => setErrorAccion(null)}>Cerrar</button>
        </p>
      )}

      <DataTable
        rows={ordenes}
        columns={columnas}
        renderActions={(o) => {
          const acciones = ACCIONES_ADMIN[o.estado] ?? [];
          if (acciones.length === 0) return "—";
          return (
            <span style={{ display: "flex", gap: 6 }}>
              {acciones.map((a) => (
                <button
                  key={a.etiqueta}
                  disabled={trabajando === o.id}
                  onClick={() => accion(o, a.estado, a.etiqueta)}
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