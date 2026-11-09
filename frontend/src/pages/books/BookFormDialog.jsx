import { useEffect, useState } from 'react'
import bookApi from '../../api/bookApi.js'
import Modal from '../../components/Modal.jsx'

const MAX_COVER_BYTES = 1024 * 1024
const COVER_TYPES = ['image/jpeg', 'image/png']

// The ISBN is typed with or without hyphens or spaces; it is kept as its 13 digits.
const digitsOf = (value) => value.replace(/[\s-]/g, '')

function validate(form) {
  const errors = {}
  const text = (key, label, max) => {
    const v = form[key].trim()
    if (!v) errors[key] = 'Không được để trống'
    else if (v.length > max) errors[key] = `${label} tối đa ${max} ký tự`
  }
  if (!/^\d{13}$/.test(digitsOf(form.isbn))) errors.isbn = 'ISBN phải đúng 13 chữ số'
  text('title', 'Tên sách', 255)
  text('author', 'Tác giả', 255)
  if (form.publisher.trim().length > 255) errors.publisher = 'Tối đa 255 ký tự'
  if (form.publicationYear !== '') {
    const year = Number(form.publicationYear)
    if (!Number.isInteger(year) || year < 1000 || year > new Date().getFullYear()) {
      errors.publicationYear = year > new Date().getFullYear() ? 'Năm xuất bản không được ở tương lai' : 'Năm không hợp lệ'
    }
  }
  const copies = Number(form.totalCopies)
  if (form.totalCopies === '' || !Number.isInteger(copies) || copies < 1 || copies > 999) errors.totalCopies = 'Nhập số từ 1 đến 999'
  return errors
}

/** Adds a book to the catalogue, with an optional cover picture. */
export default function BookFormDialog({ onClose, onSaved }) {
  const [form, setForm] = useState({
    isbn: '', title: '', author: '', publisher: '', publicationYear: '', format: 'PHYSICAL', totalCopies: '1', shelfLocation: '',
  })
  const [cover, setCover] = useState(null)
  const [preview, setPreview] = useState(null)
  const [coverError, setCoverError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!cover) return undefined
    const url = URL.createObjectURL(cover)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [cover])

  const set = (key) => (e) => {
    setForm((f) => ({ ...f, [key]: e.target.value }))
    if (fieldErrors[key]) setFieldErrors((errs) => ({ ...errs, [key]: undefined }))
  }

  const pickCover = (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    if (!COVER_TYPES.includes(file.type)) return setCoverError('Chỉ nhận ảnh JPG hoặc PNG.')
    if (file.size > MAX_COVER_BYTES) return setCoverError('Ảnh quá lớn (tối đa 1MB).')
    setCoverError(null)
    setCover(file)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errors = validate(form)
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      setError('Kiểm tra lại các ô được đánh dấu.')
      return
    }
    setSaving(true)
    setError(null)
    let created
    try {
      created = await bookApi.create({
        isbn: digitsOf(form.isbn),
        title: form.title.trim(),
        author: form.author.trim(),
        publisher: form.publisher.trim() || null,
        publicationYear: form.publicationYear === '' ? null : Number(form.publicationYear),
        format: form.format,
        totalCopies: Number(form.totalCopies),
        shelfLocation: form.format === 'PHYSICAL' ? form.shelfLocation.trim() || null : null,
      })
    } catch (err) {
      setFieldErrors(err.fieldErrors ?? {})
      setError(err.fieldErrors ? 'Kiểm tra lại các ô được đánh dấu.' : (err.message ?? 'Không lưu được. Thử lại.'))
      setSaving(false)
      return
    }
    // The book exists now; a failed picture must not make the librarian add it a second time.
    if (cover) {
      try {
        await bookApi.uploadCover(created.id, cover)
      } catch (err) {
        onSaved(`Đã thêm "${created.title}" nhưng chưa lưu được ảnh bìa: ${err.message ?? 'lỗi không rõ'}`, 'error')
        return
      }
    }
    onSaved(`Đã thêm sách "${created.title}".`, 'ok')
  }

  const field = (key, label, input = {}) => (
    <div className={`field${input.wide ? ' wide' : ''}`} key={key}>
      <label htmlFor={`b-${key}`}>{label}</label>
      <input id={`b-${key}`} value={form[key]} onChange={set(key)} aria-invalid={Boolean(fieldErrors[key])}
        autoComplete="off" {...input.attrs} />
      {fieldErrors[key] && <span className="err">{fieldErrors[key]}</span>}
    </div>
  )

  return (
    <Modal title="Thêm sách mới" onClose={onClose} wide>
      <form onSubmit={handleSubmit} noValidate>
        <div className="form-grid">
          {field('title', 'Tên sách', { wide: true, attrs: { maxLength: 255, autoFocus: true } })}
          {field('author', 'Tác giả', { attrs: { maxLength: 255 } })}
          {field('isbn', 'ISBN (13 chữ số)', { attrs: { maxLength: 17, inputMode: 'numeric' } })}
          {field('publisher', 'Nhà xuất bản', { attrs: { maxLength: 255 } })}
          {field('publicationYear', 'Năm xuất bản', { attrs: { inputMode: 'numeric', maxLength: 4, placeholder: String(new Date().getFullYear()) } })}
          <div className="field">
            <label htmlFor="b-format">Định dạng</label>
            <select id="b-format" value={form.format} onChange={set('format')}>
              <option value="PHYSICAL">Sách in</option>
              <option value="DIGITAL">Sách điện tử</option>
            </select>
          </div>
          {field('totalCopies', form.format === 'DIGITAL' ? 'Số giấy phép' : 'Số bản', { attrs: { inputMode: 'numeric', maxLength: 3 } })}
          {form.format === 'PHYSICAL' && field('shelfLocation', 'Vị trí kệ (ví dụ A1-01)', { attrs: { maxLength: 50 } })}
          <div className="field wide">
            <label htmlFor="b-cover">Ảnh bìa (không bắt buộc)</label>
            <div className="cover-pick">
              {preview && <img className="cover-preview" src={preview} alt="Ảnh bìa đã chọn" />}
              <input id="b-cover" type="file" accept="image/jpeg,image/png" onChange={pickCover} />
              {cover && <button type="button" className="link-btn" onClick={() => { setCover(null); setPreview(null) }}>Bỏ ảnh</button>}
            </div>
            <span className="meta">JPG hoặc PNG, tối đa 1MB.</span>
            {coverError && <span className="err">{coverError}</span>}
          </div>
        </div>
        {error && <p className="err" role="alert">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn" onClick={onClose}>Huỷ</button>
          <button type="submit" className="btn primary" disabled={saving}>{saving ? 'Đang lưu...' : 'Thêm sách'}</button>
        </div>
      </form>
    </Modal>
  )
}
