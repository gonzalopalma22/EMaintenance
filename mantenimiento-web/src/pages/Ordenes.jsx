import { useCallback, useEffect, useRef, useState } from "react";
import { useApi } from "../useApi";
import DataTable from "../components/DataTable";

const REFRESCO_MS = 5000;
const ESTADOS = ["ABIERTA", "ASIGNADA", "EN_CURSO", "REALIZADA", "CERRADA"];

const ACCIONES_ADMIN = {
  ABIERTA: [
    { estado: "ASIGNADA", etiqueta: "Asignar" },
    { estado: "CERRADA", etiqueta: "Cancelar" },
  ],
  ASIGNADA: [
    { estado: "ASIGNADA", etiqueta: "Reasignar" },
    { estado: "CERRADA", etiqueta: "Cancelar" },
  ],
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

  const [estado, setEstado] = useState("");
  const [ordenes, setOrdenes] = useState([]);
  const [error, setError] = useState(null);
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

  useEffect(() => {
    apiRef.current
      .get("/api/ordenes/tecnicos")
      .then((d) => setTecnicos(Array.isArray(d) ? d : []))
      .catch((e) => setErrorTecnicos(e));
  }, []);

  const enviarCambio = async (orden, body) => {
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

  const accion = (orden, nuevoEstado) => {
    if (nuevoEstado === "ASIGNADA") {
      setAsignando(orden);
      setTecnicoSel(orden.asignadoA ?? "");
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
            border: "1px solid var(--border, #ccc)",
            borderRadius: 8,
          }}
        >
          <span>
            Asignar orden de <strong>{asignando.equipoId}</strong> a:
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
          <button onClick={() => setAsignando(null)}>Cancelar</button>
          {tecnicos.length === 0 && !errorTecnicos && (
            <span style={{ color: "var(--muted)" }}>No hay técnicos con el rol Usuario.</span>
          )}
        </div>
      )}

      {errorTecnicos && (
        <p style={{ color: "crimson" }}>
          No se pudo cargar la lista de técnicos: {errorTecnicos.message}
        </p>
      )}
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
                  key={a.etiqueta}
                  disabled={trabajando === o.id}
                  onClick={() => accion(o, a.estado)}
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