/** columns: [{ key, label, render?(row) }] */
export default function DataTable({ columns, rows, emptyText = 'Không có dữ liệu' }) {
  if (!rows?.length) return <p className="muted">{emptyText}</p>

  return (
    <table className="table">
      <thead>
        <tr>
          {columns.map((c) => (
            <th key={c.key}>{c.label}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.id}>
            {columns.map((c) => (
              <td key={c.key}>{c.render ? c.render(row) : row[c.key]}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
