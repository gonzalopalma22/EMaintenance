const ISO = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/;

// Columnas cuyo valor se muestra como etiqueta (clase badge-<valor en minúsculas>)
const COLUMNAS_ETIQUETA = new Set(["estado", "prioridad", "severidad"]);

function formatear(columna, valor) {
  if (valor === null || valor === undefined || valor === "") return "";

  if (COLUMNAS_ETIQUETA.has(columna)) {
    const clase = String(valor).toLowerCase();
    return <span className={`badge badge-${clase}`}>{String(valor)}</span>;
  }

  if (typeof valor === "string" && ISO.test(valor)) {
    const d = new Date(valor);
    if (!Number.isNaN(d.getTime())) return d.toLocaleString("es-CL");
  }

  if (typeof valor === "object") return JSON.stringify(valor);
  return String(valor);
}

/**
 * rows:          filas a mostrar.
 * columns:       opcional. Lista de { key, titulo, render? }.
 *                - key: campo de la fila.
 *                - titulo: encabezado que se muestra (si falta, se usa key).
 *                - render: (valor, fila) => contenido; si falta, se formatea solo.
 *                Si no se pasa, se muestran todas las columnas de todas las filas.
 * renderActions: opcional. (fila) => contenido de la columna "Acciones".
 */
export default function DataTable({ rows, columns, renderActions }) {
  if (!rows || rows.length === 0) {
    return <p style={{ color: "var(--muted)" }}>Sin datos.</p>;
  }

  const cols =
    columns ??
    [...new Set(rows.flatMap((r) => Object.keys(r)))].map((key) => ({ key }));

  return (
    <div className="tabla-wrap">
      <table>
        <thead>
          <tr>
            {cols.map((c) => (
              <th key={c.key}>{c.titulo ?? c.key}</th>
            ))}
            {renderActions && <th>Acciones</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={row.id ?? i}>
              {cols.map((c) => (
                <td key={c.key}>
                  {c.render ? c.render(row[c.key], row) : formatear(c.key, row[c.key])}
                </td>
              ))}
              {renderActions && <td>{renderActions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}