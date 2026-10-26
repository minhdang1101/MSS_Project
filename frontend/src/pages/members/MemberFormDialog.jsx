import { useState } from 'react'
import memberApi from '../../api/memberApi.js'
import Modal from '../../components/Modal.jsx'

// Every field is required. Limits match the backend (name/email 100; phone 20 follows the DB column).
const FIELDS = [
  { key: 'fullName', label: 'Họ tên', max: 100, autoComplete: 'off' },
  { key: 'email', label: 'Email', max: 100, type: 'email', autoComplete: 'off' },
  { key: 'phone', label: 'Điện thoại', max: 20, type: 'tel', autoComplete: 'off' },
]

const EMAIL_PATTERN = /^[^\s@]+@[^\s@.]+(\.[^\s@.]+)+$/

// Same rule as the backend: separators are ignored, then 8-15 digits with an optional leading +
const PHONE_SEPARATORS = /[\s.\-()]/g
const PHONE_PATTERN = /^\+?\d{8,15}$/

function validate(form, fields) {
  const errors = {}
  for (const { key, max, type } of fields) {
    const value = form[key].trim()
    if (!value) errors[key] = 'Không được để trống'
    else if (value.length > max) errors[key] = `Tối đa ${max} ký tự`
    else if (type === 'email' && !EMAIL_PATTERN.test(value)) errors[key] = 'Email không hợp lệ'
    else if (type === 'tel' && !PHONE_PATTERN.test(value.replace(PHONE_SEPARATORS, ''))) {
      errors[key] = 'Số điện thoại không hợp lệ (8-15 chữ số)'
    }
  }
  return errors
}

/** Creates a member, or edits one when `member` is given (the card number is fixed once issued). */
export default function MemberFormDialog({ member, onClose, onSaved }) {
  const editing = Boolean(member)
  const [form, setForm] = useState({
    fullName: member?.fullName ?? '',
    email: member?.email ?? '',
    phone: member?.phone ?? '',
  })
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const set = (key) => (e) => {
    setForm((f) => ({ ...f, [key]: e.target.value }))
    if (fieldErrors[key]) setFieldErrors((errs) => ({ ...errs, [key]: undefined }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errors = validate(form, FIELDS)
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      setError('Kiểm tra lại các ô được đánh dấu.')
      return
    }
    setSaving(true)
    setError(null)
    setFieldErrors({})
    const payload = { fullName: form.fullName.trim(), email: form.email.trim(), phone: form.phone.trim() }
    try {
      if (editing) {
        await memberApi.update(member.id, payload)
        onSaved('Đã lưu thay đổi.')
      } else {
        const created = await memberApi.create(payload)
        onSaved(`Đã cấp thẻ ${created.libraryCardNo} cho ${created.fullName}.`)
      }
    } catch (err) {
      setFieldErrors(err.fieldErrors ?? {})
      setError(err.fieldErrors ? 'Kiểm tra lại các ô được đánh dấu.' : (err.message ?? 'Không lưu được. Thử lại.'))
      setSaving(false)
    }
  }

  return (
    <Modal title={editing ? 'Sửa thông tin thành viên' : 'Cấp thẻ mới'} onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <p className="meta">
          {editing
            ? <>Số thẻ <b>{member.libraryCardNo}</b> không thể thay đổi.</>
            : 'Số thẻ được cấp tự động sau khi lưu.'}
        </p>
        {FIELDS.map(({ key, label, max, ...input }, index) => (
          <div className="field" key={key}>
            <label htmlFor={`m-${key}`}>{label}</label>
            <input id={`m-${key}`} value={form[key]} onChange={set(key)} maxLength={max}
              autoFocus={index === 0} aria-invalid={Boolean(fieldErrors[key])}
              aria-describedby={fieldErrors[key] ? `m-${key}-err` : undefined} {...input} />
            {fieldErrors[key] && <span className="err" id={`m-${key}-err`}>{fieldErrors[key]}</span>}
          </div>
        ))}
        {error && <p className="err" role="alert">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn" onClick={onClose}>Huỷ</button>
          <button type="submit" className="btn primary" disabled={saving}>
            {editing ? 'Lưu thay đổi' : 'Cấp thẻ'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
