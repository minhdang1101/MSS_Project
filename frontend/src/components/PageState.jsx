/** Grey placeholders shown while data loads: the page keeps its shape instead of jumping when it arrives. */
function RowsSkeleton({ rows = 5 }) {
  return (
    <div className="panel skeleton" role="status" aria-label="Đang tải">
      {Array.from({ length: rows }, (_, i) => (
        <div className="sk-row" key={i} style={{ animationDelay: `${i * 90}ms` }}>
          <span className="sk-block sk-avatar" />
          <span className="sk-block sk-long" />
          <span className="sk-block sk-mid" />
          <span className="sk-block sk-short" />
        </div>
      ))}
    </div>
  )
}

function GridSkeleton({ cards = 8 }) {
  return (
    <div className="product-grid" role="status" aria-label="Đang tải">
      {Array.from({ length: cards }, (_, i) => (
        <div className="product sk-card" key={i} style={{ animationDelay: `${i * 70}ms` }}>
          <span className="sk-block sk-cover" />
          <span className="sk-block sk-long" />
          <span className="sk-block sk-mid" />
        </div>
      ))}
    </div>
  )
}

/** Renders loading / error states so pages only handle the happy path. `skeleton` is 'rows' (a list) or 'grid' (cards). */
export default function PageState({ loading, error, skeleton = 'rows', children }) {
  if (loading) return skeleton === 'grid' ? <GridSkeleton /> : <RowsSkeleton />
  if (error) return <p className="error" role="alert">Không tải được dữ liệu: {error}</p>
  return <div className="fade-in">{children}</div>
}
