/** A small rotating ring for buttons and short waits. The text next to it still says what is happening. */
export default function Spinner({ label }) {
  return <span className="spinner" role="img" aria-label={label ?? 'Đang xử lý'} />
}
