/** Renders loading / error states so pages only handle the happy path. */
export default function PageState({ loading, error, children }) {
  if (loading) return <p className="muted">Đang tải...</p>
  if (error) return <p className="error">Lỗi: {error}</p>
  return children
}
