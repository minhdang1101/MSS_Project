import { useCallback, useState } from 'react'
import bookApi from '../../api/bookApi.js'
import DataTable from '../../components/DataTable.jsx'
import Modal from '../../components/Modal.jsx'
import PageState from '../../components/PageState.jsx'
import Spinner from '../../components/Spinner.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import useFetch from '../../hooks/useFetch.js'

const EDITABLE_STATUSES = [
  { value: 'AVAILABLE', label: 'Sẵn có' },
  { value: 'LOST', label: 'Mất' },
  { value: 'DAMAGED', label: 'Hỏng' },
]

const emptyForm = { barcode: '', shelfLocation: '', status: 'AVAILABLE' }

export default function CopyDialog({ book, onClose, onChanged }) {
  const load = useCallback(() => bookApi.getCopies(book.id), [book.id])
  const { data, loading, error: loadError, reload } = useFetch(load)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  const set = (key) => (e) => {
    setForm((f) => ({ ...f, [key]: e.target.value }))
    if (fieldErrors[key]) setFieldErrors((errs) => ({ ...errs, [key]: undefined }))
  }

  const reset = () => {
    setEditing(null)
    setForm(emptyForm)
    setFieldErrors({})
    setError(null)
  }

  const startEdit = (copy) => {
    setEditing(copy)
    setForm({ barcode: copy.barcode, shelfLocation: copy.shelfLocation ?? '', status: copy.status })
    setFieldErrors({})
    setError(null)
  }

  const changed = () => {
    reset()
    reload()
    onChanged()
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const barcode = form.barcode.trim()
    if (!barcode) {
      setFieldErrors({ barcode: 'Không được để trống' })
      return
    }
    setSaving(true)
    setError(null)
    const shelfLocation = form.shelfLocation.trim() || null
    try {
      if (editing) await bookApi.updateCopy(editing.id, { barcode, shelfLocation, status: form.status })
      else await bookApi.addCopy(book.id, { barcode, shelfLocation })
      changed()
    } catch (err) {
      setFieldErrors(err.fieldErrors ?? {})
      setError(err.message ?? 'Không lưu được. Thử lại.')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (copy) => {
    if (!window.confirm(`Xoá bản sách ${copy.barcode}?`)) return
    setError(null)
    try {
      await bookApi.removeCopy(copy.id)
      changed()
    } catch (err) {
      setError(err.message ?? 'Không xoá được. Thử lại.')
    }
  }

  const columns = [
    { key: 'barcode', label: 'Mã vạch', render: (c) => <span className="title">{c.barcode}</span> },
    { key: 'shelfLocation', label: 'Vị trí kệ', render: (c) => c.shelfLocation ?? '—' },
    { key: 'status', label: 'Tình trạng', render: (c) => <StatusBadge status={c.status} /> },
    {
      key: 'actions',
      label: '',
      render: (c) => c.status === 'ON_LOAN' ? <span className="meta">Đang cho mượn</span> : (
        <div className="row-actions">
          <button type="button" className="btn small" onClick={() => startEdit(c)}>Sửa</button>
          <button type="button" className="btn small" onClick={() => remove(c)}>Xoá</button>
        </div>
      ),
    },
  ]

  return (
    <Modal title={`Bản sách: ${book.title}`} onClose={onClose} wide>
      <PageState loading={loading} error={loadError}>
        <DataTable columns={columns} rows={data} emptyText="Chưa có bản nào. Thêm bản đầu tiên bằng mã vạch dán trên sách." />
      </PageState>

      <form onSubmit={handleSubmit} noValidate style={{ marginTop: 16 }}>
        <div className="form-grid">
          <div className="field">
            <label htmlFor="c-barcode">Mã vạch</label>
            <input id="c-barcode" value={form.barcode} onChange={set('barcode')} maxLength={50} autoComplete="off"
              aria-invalid={Boolean(fieldErrors.barcode)} placeholder="Quét mã vạch dán trên sách" autoFocus />
            {fieldErrors.barcode && <span className="err">{fieldErrors.barcode}</span>}
          </div>
          <div className="field">
            <label htmlFor="c-shelf">Vị trí kệ</label>
            <input id="c-shelf" value={form.shelfLocation} onChange={set('shelfLocation')} maxLength={50}
              autoComplete="off" placeholder="Ví dụ A1-01" />
            {fieldErrors.shelfLocation && <span className="err">{fieldErrors.shelfLocation}</span>}
          </div>
          {editing && (
            <div className="field">
              <label htmlFor="c-status">Tình trạng</label>
              <select id="c-status" value={form.status} onChange={set('status')}>
                {EDITABLE_STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
            </div>
          )}
        </div>
        {error && <p className="err" role="alert">{error}</p>}
        <div className="modal-actions">
          {editing && <button type="button" className="btn" onClick={reset}>Huỷ sửa</button>}
          <button type="button" className="btn" onClick={onClose}>Đóng</button>
          <button type="submit" className="btn primary" disabled={saving}>{saving && <Spinner />}
            {saving ? 'Đang lưu...' : editing ? 'Lưu bản sách' : 'Thêm bản'}</button>
        </div>
      </form>
    </Modal>
  )
}
