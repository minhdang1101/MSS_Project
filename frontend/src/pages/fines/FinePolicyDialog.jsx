import { useState } from 'react'
import fineApi from '../../api/fineApi.js'
import Modal from '../../components/Modal.jsx'

const today = () => new Date().toISOString().slice(0, 10) // yyyy-MM-dd

/** Librarian edits the fine rules. Saving creates a new version that applies from the chosen date. */
export default function FinePolicyDialog({ policy, onClose, onSaved }) {
  // Form values are kept as strings, converted to numbers only when sending.
  const [form, setForm] = useState({
    dailyRate: String(policy?.dailyRate ?? ''),
    graceDays: String(policy?.graceDays ?? ''),
    maxAmount: policy?.maxAmount == null ? '' : String(policy.maxAmount),
    effectiveFrom: today(),
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (form.dailyRate === '' || form.graceDays === '' || form.effectiveFrom === '') {
      setError('Nhập đủ mức phạt, số ngày ân hạn và ngày áp dụng.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await fineApi.updatePolicy({
        dailyRate: Number(form.dailyRate),
        graceDays: Number(form.graceDays),
        maxAmount: form.maxAmount === '' ? null : Number(form.maxAmount), // empty = no ceiling
        effectiveFrom: form.effectiveFrom,
      })
      onSaved('Đã cập nhật chính sách phạt.')
    } catch (err) {
      setError(err.message ?? 'Không lưu được. Thử lại.')
      setSaving(false)
    }
  }

  return (
    <Modal title="Sửa chính sách phạt" onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label htmlFor="p-rate">Mức phạt mỗi ngày (VND)</label>
          <input id="p-rate" type="number" min="0" value={form.dailyRate} onChange={set('dailyRate')} autoFocus />
        </div>
        <div className="field">
          <label htmlFor="p-grace">Số ngày ân hạn (0 - 30)</label>
          <input id="p-grace" type="number" min="0" max="30" value={form.graceDays} onChange={set('graceDays')} />
        </div>
        <div className="field">
          <label htmlFor="p-max">Mức trần mỗi lượt mượn (để trống = không giới hạn)</label>
          <input id="p-max" type="number" min="0" value={form.maxAmount} onChange={set('maxAmount')} />
        </div>
        <div className="field">
          <label htmlFor="p-from">Áp dụng từ ngày</label>
          <input id="p-from" type="date" value={form.effectiveFrom} onChange={set('effectiveFrom')} />
        </div>
        {error && <p className="err" role="alert">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn" onClick={onClose} disabled={saving}>Huỷ</button>
          <button type="submit" className="btn primary" disabled={saving}>
            {saving ? 'Đang lưu...' : 'Lưu'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
