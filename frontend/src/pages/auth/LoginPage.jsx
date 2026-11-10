import { Link, Navigate, useLocation } from 'react-router-dom'
import { resetPasswordUrl } from '../../auth/keycloak.js'
import { useAuth } from '../../context/AuthContext.jsx'
import { homeFor } from '../../utils/roles.js'

/** The door of the app: signing in happens on Keycloak's page, so this page only sends the visitor there. */
export default function LoginPage() {
  const { isAuthenticated, role, login } = useAuth()
  const { state } = useLocation()

  if (isAuthenticated) return <Navigate to={homeFor(role)} replace />

  return (
    <section className="login" aria-labelledby="login-title">
      <div className="card enter">
        <div className="card-head">
          <div className="brand-dark">Thư viện</div>
          <span className="hole" aria-hidden="true" />
        </div>
        <h1 id="login-title">Đăng nhập</h1>
        <p className="sub">Bạn đọc dùng số thẻ hoặc email. Thủ thư dùng mã thủ thư.</p>
        {state?.notice && <p className="notice" role="status">{state.notice}</p>}
        <button className="btn primary wide" type="button" onClick={() => login()}>Đăng nhập</button>
        <p className="note auth-links">
          <a href={resetPasswordUrl()}>Quên mật khẩu?</a>
          <span>Chưa có tài khoản? <Link to="/register">Đăng ký</Link></span>
        </p>
      </div>
    </section>
  )
}
