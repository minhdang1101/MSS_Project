import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <section>
      <h2>404 - Không tìm thấy trang</h2>
      <Link to="/">Về trang chủ</Link>
    </section>
  )
}
