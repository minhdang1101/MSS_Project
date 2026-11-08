/** columns: [{ key, label, num?, render?(row) }]; rowClassName?(row) marks rows such as overdue loans. */
export default function DataTable({ columns, rows, emptyText = 'Chưa có dữ liệu.', rowClassName }) {
  if (!rows?.length) return <p className="panel empty">{emptyText}</p>

  return (
    <div className="panel tablewrap" style={{ padding: '6px 8px' }}>
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c.key} className={c.num ? 'num' : undefined}>{c.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id} className={rowClassName?.(row)}>
              {columns.map((c) => (
                <td key={c.key} className={c.num ? 'num' : undefined}>{c.render ? c.render(row) : row[c.key]}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
