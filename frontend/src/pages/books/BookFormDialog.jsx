import { useState } from 'react'
import bookApi from '../../api/bookApi.js'
import Modal from '../../components/Modal.jsx'
import Spinner from '../../components/Spinner.jsx'

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
  if (form.format === 'DIGITAL') {
    const copies = Number(form.totalCopies)
    if (form.totalCopies === '' || !Number.isInteger(copies) || copies < 1 || copies > 999) errors.totalCopies = 'Nhập số từ 1 đến 999'
  }
  return errors
}

const initialForm = (book) => ({
  isbn: book?.isbn ?? '',
  title: book?.title ?? '',
  author: book?.author ?? '',
  publisher: book?.publisher ?? '',
  publicationYear: book?.publicationYear == null ? '' : String(book.publicationYear),
  format: book?.format ?? 'PHYSICAL',
  totalCopies: book ? String(book.totalCopies) : '1',
})

export default function BookFormDialog({ book, onClose, onSaved }) {
  const editing = Boolean(book)
  const [form, setForm] = useState(() => initialForm(book))
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

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
    setSaving(true)
    setError(null)
    const data = {
      isbn: digitsOf(form.isbn),
      title: form.title.trim(),
      author: form.author.trim(),
      publisher: form.publisher.trim() || null,
      publicationYear: form.publicationYear === '' ? null : Number(form.publicationYear),
      format: form.format,
      totalCopies: form.format === 'DIGITAL' ? Number(form.totalCopies) : 0,
    }
    try {
      const saved = editing ? await bookApi.update(book.id, data) : await bookApi.create(data)
      onSaved(editing ? `Đã cập nhật sách "${saved.title}".` : `Đã thêm sách "${saved.title}".`, 'ok')
    } catch (err) {
      setFieldErrors(err.fieldErrors ?? {})
      setError(err.fieldErrors ? 'Kiểm tra lại các ô được đánh dấu.' : (err.message ?? 'Không lưu được. Thử lại.'))
      setSaving(false)
    }
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
    <Modal title={editing ? 'Sửa thông tin sách' : 'Thêm sách mới'} onClose={onClose} wide>
      <form onSubmit={handleSubmit} noValidate>
        <div className="form-grid">
          {field('title', 'Tên sách', { wide: true, attrs: { maxLength: 255, autoFocus: true } })}
          {field('author', 'Tác giả', { attrs: { maxLength: 255 } })}
          {field('isbn', 'ISBN (13 chữ số)', { attrs: { maxLength: 17, inputMode: 'numeric' } })}
          {field('publisher', 'Nhà xuất bản', { attrs: { maxLength: 255 } })}
          {field('publicationYear', 'Năm xuất bản', { attrs: { inputMode: 'numeric', maxLength: 4, placeholder: String(new Date().getFullYear()) } })}
          <div className="field">
            <label htmlFor="b-format">Định dạng</label>
            <select id="b-format" value={form.format} onChange={set('format')} disabled={editing}>
              <option value="PHYSICAL">Sách in</option>
              <option value="DIGITAL">Sách điện tử</option>
            </select>
          </div>
          {form.format === 'DIGITAL'
            ? field('totalCopies', 'Số giấy phép', { attrs: { inputMode: 'numeric', maxLength: 3 } })
            : (
              <div className="field">
                <label>Số bản</label>
                <span className="meta">
                  {editing ? `${book.availableCopies} / ${book.totalCopies} bản. Quản lý ở nút "Bản sách".`
                    : 'Thêm từng bản kèm mã vạch và vị trí kệ sau khi tạo sách.'}
                </span>
              </div>
            )}
        </div>
        {error && <p className="err" role="alert">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn" onClick={onClose}>Huỷ</button>
          <button type="submit" className="btn primary" disabled={saving}>{saving && <Spinner />}
            {saving ? 'Đang lưu...' : editing ? 'Lưu thay đổi' : 'Thêm sách'}</button>
        </div>
      </form>
    </Modal>
  )
}
