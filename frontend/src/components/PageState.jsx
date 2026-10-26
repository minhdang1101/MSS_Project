/** Renders loading / error states so pages only handle the happy path. */
export default function PageState({ loading, error, children }) {
  if (loading) return <p className="muted" role="status">Đang tải...</p>;
  if (error) return <p className="error" role="alert">Không tải được dữ liệu: {error}</p>;
  return children;
}
