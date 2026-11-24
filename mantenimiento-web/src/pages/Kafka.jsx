import { useCallback, useEffect, useRef, useState } from "react";
import { useApi } from "../useApi";
import { useRoles } from "../useRoles";
import DataTable from "../components/DataTable";

const CONFIGS_COMUNES = [
  "retention.ms",
  "cleanup.policy",
  "min.insync.replicas",
  "max.message.bytes",
  "segment.ms",
];

function mensajeDe(e) {
  const m = /"detail":"([^"]*)"/.exec(e?.message ?? "");
  return m ? m[1] : (e?.message ?? "Error desconocido");
}

const unir = (lista) => (Array.isArray(lista) ? lista.join(", ") : "");

export default function Kafka() {
  const api = useApi();
  const apiRef = useRef(api);
  apiRef.current = api;

  const { esAdmin } = useRoles();

  const [cluster, setCluster] = useState(null);
  const [topics, setTopics] = useState([]);
  const [grupos, setGrupos] = useState([]);
  const [detalle, setDetalle] = useState(null);
  const [grupo, setGrupo] = useState(null);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [trabajando, setTrabajando] = useState(false);

  // Formularios
  const [nuevo, setNuevo] = useState({ name: "", partitions: 3, replicationFactor: 3 });
  const [masParticiones, setMasParticiones] = useState("");
  const [cfg, setCfg] = useState({ clave: "", valor: "" });

  const cargar = useCallback(async () => {
    try {
      const [c, t, g] = await Promise.all([
        apiRef.current.get("/api/kafka/cluster"),
        apiRef.current.get("/api/kafka/topics"),
        // si falla el listado de grupos no debe romper el resto del panel
        apiRef.current.get("/api/kafka/groups").catch(() => []),
      ]);
      setCluster(c);
      setTopics(Array.isArray(t) ? t : []);
      setGrupos(Array.isArray(g) ? g : []);
      setError(null);
    } catch (e) {
      setError(e);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const ejecutar = async (accion, mensajeOk) => {
    setTrabajando(true);
    setInfo(null);
    try {
      await accion();
      if (mensajeOk) setInfo(mensajeOk);
      setError(null);
    } catch (e) {
      setError(e);
    } finally {
      setTrabajando(false);
    }
  };

  const verDetalle = (nombre) =>
    ejecutar(async () => {
      const d = await apiRef.current.get(
        `/api/kafka/topics/${encodeURIComponent(nombre)}`
      );
      setDetalle(d);
      setMasParticiones("");
      setCfg({ clave: "", valor: "" });
    });

  const verGrupo = (id) =>
    ejecutar(async () => {
      const g = await apiRef.current.get(
        `/api/kafka/groups/${encodeURIComponent(id)}`
      );
      setGrupo(g);
    });

  const crear = (ev) => {
    ev.preventDefault();
    return ejecutar(async () => {
      await apiRef.current.post("/api/kafka/topics", {
        name: nuevo.name.trim(),
        partitions: Number(nuevo.partitions),
        replicationFactor: Number(nuevo.replicationFactor),
      });
      setNuevo({ name: "", partitions: 3, replicationFactor: 3 });
      await cargar();
    }, `Tópico "${nuevo.name.trim()}" creado.`);
  };

  const eliminar = (nombre) => {
    if (!window.confirm(`¿Eliminar el tópico ${nombre}? Se perderán sus mensajes.`)) return;
    return ejecutar(async () => {
      await apiRef.current.del(`/api/kafka/topics/${encodeURIComponent(nombre)}`);
      if (detalle?.name === nombre) setDetalle(null);
      await cargar();
    }, `Tópico "${nombre}" eliminado.`);
  };

  const aumentarParticiones = () =>
    ejecutar(async () => {
      await apiRef.current.patch(
        `/api/kafka/topics/${encodeURIComponent(detalle.name)}/partitions`,
        { partitions: Number(masParticiones) }
      );
      await verDetalle(detalle.name);
      await cargar();
    }, "Particiones aumentadas.");

  // valor === null restablece la configuración a su valor por defecto
  const guardarConfig = (clave, valor) =>
    ejecutar(async () => {
      const actualizado = await apiRef.current.put(
        `/api/kafka/topics/${encodeURIComponent(detalle.name)}/config`,
        { configs: { [clave]: valor } }
      );
      setDetalle(actualizado);
      setCfg({ clave: "", valor: "" });
    }, valor === null ? `Configuración "${clave}" restablecida.` : `Configuración "${clave}" guardada.`);

  if (!esAdmin) {
    return <p>Esta sección requiere el rol Admin.</p>;
  }

  const colsTopics = [
    { key: "name", titulo: "Tópico" },
    { key: "partitions", titulo: "Particiones" },
    { key: "replicationFactor", titulo: "Réplicas" },
  ];

  const colsParticiones = [
    { key: "partition", titulo: "Partición" },
    { key: "leader", titulo: "Líder (broker)" },
    { key: "replicas", titulo: "Réplicas", render: unir },
    { key: "isr", titulo: "ISR (sincronizadas)", render: unir },
  ];

  const colsGrupos = [
    { key: "groupId", titulo: "Consumer group" },
    { key: "miembros", titulo: "Miembros" },
    { key: "estado", titulo: "Estado" },
  ];

  const colsMiembros = [
    { key: "clientId", titulo: "Client ID" },
    { key: "host", titulo: "Host" },
    { key: "particiones", titulo: "Particiones asignadas", render: unir },
  ];

  const colsOffsets = [
    { key: "topic", titulo: "Tópico" },
    { key: "particion", titulo: "Partición" },
    { key: "offset", titulo: "Offset confirmado" },
    { key: "finLog", titulo: "Fin del log" },
    { key: "lag", titulo: "Lag" },
  ];

  const filasConfig = Object.entries(detalle?.configs ?? {}).map(([config, valor]) => ({
    id: config,
    config,
    valor,
  }));

  return (
    <section>
      <h2>Administración de Kafka</h2>

      {cluster && (
        <p style={{ color: "var(--muted)" }}>
          Cluster <code>{cluster.clusterId}</code> · Controlador: broker {cluster.controller} ·
          Brokers: {cluster.brokers.map((b) => `${b.id} (${b.host}:${b.port})`).join(", ")}
        </p>
      )}

      {info && <p style={{ color: "green" }}>{info}</p>}
      {error && <p style={{ color: "crimson" }}>Error: {mensajeDe(error)}</p>}

      <h3>Crear tópico</h3>
      <form
        onSubmit={crear}
        style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap", marginBottom: 16 }}
      >
        <input
          value={nuevo.name}
          onChange={(e) => setNuevo({ ...nuevo, name: e.target.value })}
          placeholder="nombre.del.topico"
          style={{ minWidth: 240 }}
          required
        />
        <label>
          Particiones{" "}
          <input
            type="number"
            min="1"
            max="50"
            value={nuevo.partitions}
            onChange={(e) => setNuevo({ ...nuevo, partitions: e.target.value })}
            style={{ width: 70 }}
          />
        </label>
        <label>
          Réplicas{" "}
          <input
            type="number"
            min="1"
            max="3"
            value={nuevo.replicationFactor}
            onChange={(e) => setNuevo({ ...nuevo, replicationFactor: e.target.value })}
            style={{ width: 70 }}
          />
        </label>
        <button type="submit" disabled={trabajando || !nuevo.name.trim()}>
          Crear
        </button>
      </form>

      <h3>
        Tópicos <button onClick={cargar}>Actualizar</button>
      </h3>
      <DataTable
        rows={topics.map((t) => ({ ...t, id: t.name }))}
        columns={colsTopics}
        renderActions={(t) => (
          <span style={{ display: "flex", gap: 8 }}>
            <button disabled={trabajando} onClick={() => verDetalle(t.name)}>
              Detalle
            </button>
            <button disabled={trabajando} onClick={() => eliminar(t.name)}>
              Eliminar
            </button>
          </span>
        )}
      />

      {detalle && (
        <div style={{ marginTop: 24 }}>
          <h3>
            Detalle de <code>{detalle.name}</code>{" "}
            <button onClick={() => setDetalle(null)}>Cerrar</button>
          </h3>

          <DataTable
            rows={detalle.particiones.map((p) => ({ ...p, id: p.partition }))}
            columns={colsParticiones}
          />

          <div style={{ display: "flex", gap: 8, alignItems: "center", margin: "12px 0" }}>
            <label>
              Aumentar particiones a{" "}
              <input
                type="number"
                min={detalle.partitions + 1}
                max="50"
                value={masParticiones}
                onChange={(e) => setMasParticiones(e.target.value)}
                placeholder={`> ${detalle.partitions}`}
                style={{ width: 90 }}
              />
            </label>
            <button
              onClick={aumentarParticiones}
              disabled={trabajando || Number(masParticiones) <= detalle.partitions}
            >
              Aplicar
            </button>
            <span style={{ color: "var(--muted)" }}>Kafka solo permite aumentar, no reducir.</span>
          </div>

          <h4>Configuración del tópico</h4>
          {filasConfig.length === 0 ? (
            <p style={{ color: "var(--muted)" }}>Usa los valores por defecto del cluster.</p>
          ) : (
            <DataTable
              rows={filasConfig}
              columns={[
                { key: "config", titulo: "Configuración" },
                { key: "valor", titulo: "Valor" },
              ]}
              renderActions={(f) => (
                <button disabled={trabajando} onClick={() => guardarConfig(f.config, null)}>
                  Restablecer
                </button>
              )}
            />
          )}

          <div style={{ display: "flex", gap: 8, alignItems: "center", marginTop: 12 }}>
            <input
              list="configs-comunes"
              value={cfg.clave}
              onChange={(e) => setCfg({ ...cfg, clave: e.target.value })}
              placeholder="retention.ms"
              style={{ minWidth: 200 }}
            />
            <datalist id="configs-comunes">
              {CONFIGS_COMUNES.map((c) => (
                <option key={c} value={c} />
              ))}
            </datalist>
            <input
              value={cfg.valor}
              onChange={(e) => setCfg({ ...cfg, valor: e.target.value })}
              placeholder="valor"
              style={{ minWidth: 140 }}
            />
            <button
              disabled={trabajando || !cfg.clave.trim() || cfg.valor === ""}
              onClick={() => guardarConfig(cfg.clave.trim(), cfg.valor)}
            >
              Guardar configuración
            </button>
          </div>
        </div>
      )}

      <h3 style={{ marginTop: 32 }}>
        Consumer groups <button onClick={cargar}>Actualizar</button>
      </h3>
      {grupos.length === 0 ? (
        <p style={{ color: "var(--muted)" }}>No hay consumer groups registrados.</p>
      ) : (
        <DataTable
          rows={grupos.map((g) => ({ ...g, id: g.groupId }))}
          columns={colsGrupos}
          renderActions={(g) => (
            <button disabled={trabajando} onClick={() => verGrupo(g.groupId)}>
              Detalle
            </button>
          )}
        />
      )}

      {grupo && (
        <div style={{ marginTop: 24 }}>
          <h3>
            Grupo <code>{grupo.groupId}</code>{" "}
            <button disabled={trabajando} onClick={() => verGrupo(grupo.groupId)}>
              Refrescar
            </button>{" "}
            <button onClick={() => setGrupo(null)}>Cerrar</button>
          </h3>
          <p style={{ color: "var(--muted)" }}>
            Estado: {grupo.estado} · Coordinador: {grupo.coordinador} · Asignador:{" "}
            {grupo.asignador} · Lag total: <strong>{grupo.lagTotal}</strong>
          </p>

          <h4>Miembros</h4>
          {grupo.miembros.length === 0 ? (
            <p style={{ color: "var(--muted)" }}>Sin consumidores conectados.</p>
          ) : (
            <DataTable
              rows={grupo.miembros.map((m) => ({ ...m, id: m.consumerId }))}
              columns={colsMiembros}
            />
          )}

          <h4>Offsets y lag por partición</h4>
          {grupo.offsets.length === 0 ? (
            <p style={{ color: "var(--muted)" }}>El grupo aún no ha confirmado offsets.</p>
          ) : (
            <DataTable
              rows={grupo.offsets.map((o) => ({ ...o, id: `${o.topic}-${o.particion}` }))}
              columns={colsOffsets}
            />
          )}
        </div>
      )}
    </section>
  );
}