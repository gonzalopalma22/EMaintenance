const ISO = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/;

function formatear(columna, valor) {
  if (valor === null || valor === undefined || valor === "") return "";

  if (columna === "estado" || columna === "prioridad") {
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

export default function DataTable({ rows, renderActions }) {
  if (!rows || rows.length === 0) {
    return <p style={{ color: "var(--muted)" }}>Sin datos.</p>;
  }

  const columns = Object.keys(rows[0]);

  return (
    <div className="tabla-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c}>{c}</th>
            ))}
            {renderActions && <th>Acciones</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={row.id ?? i}>
              {columns.map((c) => (
                <td key={c}>{formatear(c, row[c])}</td>
              ))}
              {renderActions && <td>{renderActions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}