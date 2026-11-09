import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import authApi from '../../api/authApi.js'
import PasswordInput from '../../components/PasswordInput.jsx'
import Spinner from '../../components/Spinner.jsx'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@.]+(\.[^\s@.]+)+$/
const STEPS = ['Email', 'Mã xác nhận', 'Mật khẩu mới']

function Steps({ current }) {
  return (
    <ol className="steps" aria-label="Các bước">
      {STEPS.map((label, i) => (
        <li key={label} className={i < current ? 'done' : i === current ? 'now' : undefined}
          aria-current={i === current ? 'step' : undefined}>
          <span className="steps-dot">{i < current ? '✓' : i + 1}</span>
          <span className="steps-label">{label}</span>
        </li>
      ))}
    </ol>
  )
}

/**
 * Three steps: 1) the e-mail, 2) the code from that e-mail, checked on its own, 3) the new password, which can
 * only be reached once the code was confirmed.
 */
export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(0)
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [info, setInfo] = useState(null)
  const [busy, setBusy] = useState(false)
  const [shake, setShake] = useState(0) // bumped on each failure so the message animates in again

  const fail = (message, fields = {}) => {
    setError(message)
    setFieldErrors(fields)
    setShake((n) => n + 1)
  }

  const sendCode = async (e) => {
    e?.preventDefault()
    if (!EMAIL_PATTERN.test(email.trim())) return fail('Nhập email hợp lệ.')
    setBusy(true)
    setError(null)
    try {
      await authApi.forgotPassword(email.trim())
      setInfo(`Nếu ${email.trim()} có tài khoản, một mã 6 chữ số vừa được gửi tới đó. Mã có hiệu lực 10 phút.`)
      setStep(1)
      setError(null)
      setFieldErrors({})
    } catch (err) {
      fail(err.message ?? 'Không kết nối được máy chủ. Thử lại sau.')
    } finally {
      setBusy(false)
    }
  }

  const checkCode = async (e) => {
    e.preventDefault()
    if (!/^\d{6}$/.test(code.trim())) return fail('Mã gồm 6 chữ số.', { code: 'Mã gồm 6 chữ số' })
    setBusy(true)
    setError(null)
    try {
      await authApi.verifyResetCode(email.trim(), code.trim())
      setStep(2)
      setError(null)
      setFieldErrors({})
    } catch (err) {
      fail(err.message ?? 'Không kết nối được máy chủ. Thử lại sau.', { code: ' ' })
    } finally {
      setBusy(false)
    }
  }

  const savePassword = async (e) => {
    e.preventDefault()
    const errors = {}
    if (password.length < 8) errors.password = 'Ít nhất 8 ký tự'
    else if (password.length > 72) errors.password = 'Tối đa 72 ký tự'
    if (confirm !== password) errors.confirm = 'Mật khẩu nhập lại không khớp'
    if (Object.keys(errors).length > 0) return fail('Kiểm tra lại các ô được đánh dấu.', errors)
    setBusy(true)
    setError(null)
    try {
      await authApi.resetPassword(email.trim(), code.trim(), password)
      navigate('/login', { replace: true, state: { notice: 'Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.' } })
    } catch (err) {
      // The code may have run out while the reader was typing: send them back to ask for a new one.
      if (err.status === 409) {
        setCode('')
        setStep(1)
        return fail(err.message ?? 'Mã không còn hiệu lực. Hãy nhập lại mã hoặc gửi mã mới.')
      }
      fail(err.fieldErrors ? 'Kiểm tra lại các ô được đánh dấu.' : (err.message ?? 'Không kết nối được máy chủ. Thử lại sau.'),
        err.fieldErrors ?? {})
    } finally {
      setBusy(false)
    }
  }

  const resend = async () => {
    setCode('')
    await sendCode()
  }

  const submit = [sendCode, checkCode, savePassword][step]
  const submitLabel = [['Gửi mã', 'Đang gửi...'], ['Xác nhận mã', 'Đang kiểm tra...'], ['Đặt lại mật khẩu', 'Đang lưu...']][step]

  return (
    <section className="login" aria-labelledby="forgot-title">
      <form className="card enter" onSubmit={submit} noValidate>
        <div className="card-head">
          <div className="brand-dark">Thư viện</div>
          <span className="hole" aria-hidden="true" />
        </div>
        <h1 id="forgot-title">Quên mật khẩu</h1>
        <Steps current={step} />

        <div className="step-body" key={`step-${step}`}>
          {step === 0 && (
            <>
              <p className="sub">Nhập email đã đăng ký, chúng tôi sẽ gửi mã xác nhận.</p>
              <div className="field">
                <label htmlFor="f-email">Email</label>
                <input id="f-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                  autoComplete="email" autoFocus maxLength={100} />
              </div>
            </>
          )}
          {step === 1 && (
            <>
              <p className="notice" role="status">{info}</p>
              <div className="field">
                <label htmlFor="f-code">Mã xác nhận</label>
                <input id="f-code" className="code-input" value={code}
                  onChange={(e) => { setCode(e.target.value.replace(/\D/g, '')); if (fieldErrors.code) setFieldErrors({}) }}
                  inputMode="numeric" maxLength={6} autoComplete="one-time-code" autoFocus placeholder="••••••"
                  aria-invalid={Boolean(fieldErrors.code)} />
                {fieldErrors.code?.trim() && <span className="err">{fieldErrors.code}</span>}
              </div>
            </>
          )}
          {step === 2 && (
            <>
              <p className="notice" role="status">Mã đúng rồi. Đặt mật khẩu mới cho {email.trim()}.</p>
              <div className="field">
                <label htmlFor="f-pw">Mật khẩu mới (8-72 ký tự)</label>
                <PasswordInput id="f-pw" value={password} onChange={(e) => setPassword(e.target.value)}
                  autoComplete="new-password" autoFocus maxLength={72}
                  aria-invalid={Boolean(fieldErrors.password || fieldErrors.newPassword)} />
                {(fieldErrors.password || fieldErrors.newPassword) && <span className="err">{fieldErrors.password ?? fieldErrors.newPassword}</span>}
              </div>
              <div className="field">
                <label htmlFor="f-confirm">Nhập lại mật khẩu mới</label>
                <PasswordInput id="f-confirm" value={confirm} onChange={(e) => setConfirm(e.target.value)}
                  autoComplete="new-password" maxLength={72} aria-invalid={Boolean(fieldErrors.confirm)} />
                {fieldErrors.confirm && <span className="err">{fieldErrors.confirm}</span>}
              </div>
            </>
          )}
        </div>

        {error && <p className="err pop" role="alert" key={`error-${shake}`}>{error}</p>}
        <button className="btn primary wide" type="submit" disabled={busy}>
          {busy && <Spinner />}
          {busy ? submitLabel[1] : submitLabel[0]}
        </button>
        <p className="note auth-links">
          {step === 1
            ? <button type="button" className="link-btn" onClick={resend} disabled={busy}>Gửi lại mã</button>
            : <span />}
          <Link to="/login">Quay lại đăng nhập</Link>
        </p>
      </form>
    </section>
  )
}
