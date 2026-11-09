import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import authApi from '../../api/authApi.js'
import PasswordInput from '../../components/PasswordInput.jsx'
import Spinner from '../../components/Spinner.jsx'
import { useAuth } from '../../context/AuthContext.jsx'
import { homeFor } from '../../utils/roles.js'

const EMPTY_MESSAGE = 'Nhập tên đăng nhập và mật khẩu để đăng nhập.'
const UNREACHABLE_MESSAGE = 'Không kết nối được máy chủ. Kiểm tra dịch vụ rồi thử lại.'

export default function LoginPage() {
  const { isAuthenticated, role, login } = useAuth()
  const navigate = useNavigate()
  const { state } = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated) return <Navigate to={homeFor(role)} replace />

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!username.trim() || !password) {
      setError(EMPTY_MESSAGE)
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const session = await authApi.login(username.trim(), password)
      login(session.token, session.fullName, session.role, session.username)
      navigate(homeFor(session.role), { replace: true })
    } catch (err) {
      // axiosClient rejects with the ApiError body; no body means the gateway or network is down.
      if (err.status === 401 || err.status === 429) setError(err.message)
      else setError(err.status ? 'Đăng nhập không thành công. Thử lại sau.' : UNREACHABLE_MESSAGE)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="login" aria-labelledby="login-title">
      <form className="card enter" onSubmit={handleSubmit} noValidate>
        <div className="card-head">
          <div className="brand-dark">Thư viện</div>
          <span className="hole" aria-hidden="true" />
        </div>
        <h1 id="login-title">Đăng nhập</h1>
        <p className="sub">Bạn đọc dùng số thẻ hoặc email. Thủ thư dùng mã thủ thư.</p>
        {state?.notice && <p className="notice" role="status">{state.notice}</p>}
        <div className="field">
          <label htmlFor="l-id">Tên đăng nhập</label>
          <input id="l-id" value={username} onChange={(e) => setUsername(e.target.value)}
            autoComplete="username" placeholder="Số thẻ, email hoặc mã thủ thư" />
        </div>
        <div className="field">
          <label htmlFor="l-pw">Mật khẩu</label>
          <PasswordInput id="l-pw" value={password} onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password" />
        </div>
        {error && <p className="err pop" role="alert" key={error}>{error}</p>}
        <button className="btn primary wide" type="submit" disabled={submitting}>
          {submitting && <Spinner />}
          {submitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
        <p className="note auth-links">
          <Link to="/forgot-password">Quên mật khẩu?</Link>
          <span>Chưa có tài khoản? <Link to="/register">Đăng ký</Link></span>
        </p>
      </form>
    </section>
  )
}
