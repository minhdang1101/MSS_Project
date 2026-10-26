import { NavLink, Outlet } from 'react-router-dom'

const NAV_ITEMS = [
  { to: '/', label: 'Tổng quan', end: true },
  { to: '/books', label: 'Sách' },
  { to: '/members', label: 'Thành viên' },
  { to: '/loans', label: 'Mượn / Trả' },
  { to: '/fines', label: 'Tiền phạt' },
  { to: '/notifications', label: 'Thông báo' },
]

export default function MainLayout() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <h1 className="brand">Digital Library</h1>
        <nav>
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className="nav-link">
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
