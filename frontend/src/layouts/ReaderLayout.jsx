import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import readerApi from '../api/readerApi.js'
import { useAuth } from '../context/AuthContext.jsx'
import useFetch from '../hooks/useFetch.js'

/** The reader's shell: a sidebar to move between the catalogue, their loans and their fines. */
export default function ReaderLayout() {
  const { user, username, logout } = useAuth()
  const navigate = useNavigate()
  const loans = useFetch(readerApi.getMyLoans)
  const fines = useFetch(readerApi.getMyFines)

  const open = (loans.data ?? []).filter((l) => l.status !== 'RETURNED').length
  const owed = (fines.data ?? []).filter((f) => f.status === 'ACCRUING' || f.status === 'UNPAID').length

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app">
      <aside className="rail">
        <div className="brand">Thư viện<small>Không gian bạn đọc</small></div>
        <nav className="nav" aria-label="Điều hướng chính">
          <NavLink to="/me" end>Khám phá sách</NavLink>
          <NavLink to="/me/loans">
            Sách đang mượn
            {open > 0 && <span className="count count-plain" aria-label={`${open} cuốn đang mượn`}>{open}</span>}
          </NavLink>
          <NavLink to="/me/fines">
            Tiền phạt
            {owed > 0 && <span className="count" aria-label={`${owed} khoản chưa thanh toán`}>{owed}</span>}
          </NavLink>
        </nav>
        <div className="who">
          <b>{user ?? 'Bạn đọc'}</b>Bạn đọc · {username}
          <button className="logout" type="button" onClick={handleLogout}>Đăng xuất</button>
        </div>
      </aside>
      <main className="page-in">
        <Outlet context={{ loans, fines }} />
      </main>
    </div>
  )
}
