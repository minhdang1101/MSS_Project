import { Link } from 'react-router-dom'
import PageHead from '../components/PageHead.jsx'

export default function NotFoundPage() {
  return (
    <main>
      <PageHead title="Không tìm thấy trang" subtitle="Địa chỉ này không tồn tại hoặc đã được chuyển.">
        <Link className="btn primary" to="/">Về trang Hôm nay</Link>
      </PageHead>
    </main>
  )
}
