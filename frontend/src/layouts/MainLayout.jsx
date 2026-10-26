import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

const NAV_ITEMS = [
  { to: '/', label: 'Hôm nay', end: true },
  { to: '/loans', label: 'Mượn và trả' },
  { to: '/books', label: 'Danh mục sách' },
  { to: '/members', label: 'Thành viên' },
  { to: '/fines', label: 'Tiền phạt' },
  { to: '/notifications', label: 'Nhắc hạn' },
]

export default function MainLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app">
      <aside className="rail">
        <div className="brand">Quản lý thư viện<small>Quầy thủ thư</small></div>
        <nav className="nav" aria-label="Điều hướng chính">
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="who">
          <b>{user ?? 'Thủ thư'}</b>Thủ thư
          <button className="logout" type="button" onClick={handleLogout}>Đăng xuất</button>
        </div>
      </aside>
      <main>
        <Outlet />
      </main>
    </div>
  )
}
