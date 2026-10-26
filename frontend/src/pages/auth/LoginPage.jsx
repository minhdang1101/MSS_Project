import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import authApi from '../../api/authApi.js'
import { useAuth } from '../../context/AuthContext.jsx'

const EMPTY_MESSAGE = 'Nhập mã thủ thư và mật khẩu để đăng nhập.'
const UNREACHABLE_MESSAGE = 'Không kết nối được máy chủ. Kiểm tra dịch vụ rồi thử lại.'

export default function LoginPage() {
  const { isAuthenticated, login } = useAuth()
  const navigate = useNavigate()
  const [staffCode, setStaffCode] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated) return <Navigate to="/" replace />

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!staffCode.trim() || !password) {
      setError(EMPTY_MESSAGE)
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const session = await authApi.login(staffCode.trim(), password)
      login(session.token, session.fullName)
      navigate('/', { replace: true })
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
      <form className="card" onSubmit={handleSubmit} noValidate>
        <div className="card-head">
          <div className="brand-dark">Quản lý thư viện</div>
          <span className="hole" aria-hidden="true" />
        </div>
        <h1 id="login-title">Đăng nhập quầy thủ thư</h1>
        <p className="sub">Dùng mã thủ thư được cấp khi nhận ca.</p>
        <div className="field">
          <label htmlFor="l-id">Mã thủ thư</label>
          <input id="l-id" value={staffCode} onChange={(e) => setStaffCode(e.target.value)}
            autoComplete="username" placeholder="Ví dụ: TT-0021" />
        </div>
        <div className="field">
          <label htmlFor="l-pw">Mật khẩu</label>
          <input id="l-pw" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password" />
        </div>
        {error && <p className="err" role="alert">{error}</p>}
        <button className="btn primary wide" type="submit" disabled={submitting}>
          {submitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
        <p className="note">Quên mật khẩu? Liên hệ quản trị viên thư viện để cấp lại.</p>
      </form>
    </section>
  )
}
