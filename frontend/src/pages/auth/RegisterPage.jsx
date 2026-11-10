import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import authApi from '../../api/authApi.js'
import PasswordInput from '../../components/PasswordInput.jsx'
import Spinner from '../../components/Spinner.jsx'
import { useAuth } from '../../context/AuthContext.jsx'
import { homeFor } from '../../utils/roles.js'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@.]+(\.[^\s@.]+)+$/

const FIELDS = [
  { key: 'fullName', label: 'Họ tên', autoComplete: 'name', max: 100 },
  { key: 'email', label: 'Email', type: 'email', autoComplete: 'email', max: 100 },
  { key: 'password', label: 'Mật khẩu (8-72 ký tự)', type: 'password', autoComplete: 'new-password', max: 72 },
  { key: 'confirm', label: 'Nhập lại mật khẩu', type: 'password', autoComplete: 'new-password', max: 72 },
]

function validate(form) {
  const errors = {}
  if (!form.fullName.trim()) errors.fullName = 'Không được để trống'
  if (!form.email.trim()) errors.email = 'Không được để trống'
  else if (!EMAIL_PATTERN.test(form.email.trim())) errors.email = 'Email không hợp lệ'
  if (form.password.length < 8) errors.password = 'Ít nhất 8 ký tự'
  else if (form.password.length > 72) errors.password = 'Tối đa 72 ký tự'
  if (form.confirm !== form.password) errors.confirm = 'Mật khẩu nhập lại không khớp'
  return errors
}

/** A reader creates their own account and gets a library card number; they then sign in on Keycloak's page. */
export default function RegisterPage() {
  const { isAuthenticated, role, login } = useAuth()
  const [created, setCreated] = useState(null) // the answer of the sign-up: the new card number
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirm: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated) return <Navigate to={homeFor(role)} replace />

  const set = (key) => (e) => {
    setForm((f) => ({ ...f, [key]: e.target.value }))
    if (fieldErrors[key]) setFieldErrors((errs) => ({ ...errs, [key]: undefined }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errors = validate(form)
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      setError('Kiểm tra lại các ô được đánh dấu.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      setCreated(await authApi.register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
      }))
    } catch (err) {
      setFieldErrors(err.fieldErrors ?? {})
      setError(err.fieldErrors ? 'Kiểm tra lại các ô được đánh dấu.'
        : (err.message ?? 'Không kết nối được máy chủ. Thử lại sau.'))
      setSubmitting(false)
    }
  }

  if (created) {
    return (
      <section className="login" aria-labelledby="done-title">
        <div className="card enter">
          <div className="card-head">
            <div className="brand-dark">Thư viện</div>
            <span className="hole" aria-hidden="true" />
          </div>
          <h1 id="done-title">Đã tạo tài khoản</h1>
          <p className="notice" role="status">
            Số thẻ của bạn là <b>{created.libraryCardNo}</b>. Đăng nhập bằng số thẻ hoặc email <b>{created.email}</b>.
          </p>
          <button className="btn primary wide" type="button" onClick={() => login(created.email)}>Đăng nhập ngay</button>
        </div>
      </section>
    )
  }

  return (
    <section className="login" aria-labelledby="register-title">
      <form className="card enter" onSubmit={handleSubmit} noValidate>
        <div className="card-head">
          <div className="brand-dark">Thư viện</div>
          <span className="hole" aria-hidden="true" />
        </div>
        <h1 id="register-title">Đăng ký tài khoản</h1>
        <p className="sub">Số thẻ thư viện được cấp tự động sau khi đăng ký.</p>
        {FIELDS.map(({ key, label, max, ...input }, index) => (
          <div className="field" key={key}>
            <label htmlFor={`r-${key}`}>{label}</label>
            {input.type === 'password'
              ? <PasswordInput id={`r-${key}`} value={form[key]} onChange={set(key)} maxLength={max}
                  aria-invalid={Boolean(fieldErrors[key])} autoComplete={input.autoComplete} />
              : <input id={`r-${key}`} value={form[key]} onChange={set(key)} maxLength={max} autoFocus={index === 0}
                  aria-invalid={Boolean(fieldErrors[key])} {...input} />}
            {fieldErrors[key] && <span className="err">{fieldErrors[key]}</span>}
          </div>
        ))}
        {error && <p className="err pop" role="alert" key={error}>{error}</p>}
        <button className="btn primary wide" type="submit" disabled={submitting}>
          {submitting && <Spinner />}
          {submitting ? 'Đang đăng ký...' : 'Đăng ký'}
        </button>
        <p className="note auth-links"><span>Đã có tài khoản? <Link to="/login">Đăng nhập</Link></span></p>
      </form>
    </section>
  )
}
