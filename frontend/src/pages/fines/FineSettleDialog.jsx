import { useState } from 'react'
import fineApi from '../../api/fineApi.js'
import Modal from '../../components/Modal.jsx'
import { formatCurrency, formatDate } from '../../utils/format.js'

/** Closes an unpaid fine: the librarian either took the money or let the member off. */
export default function FineSettleDialog({ fine, onClose, onSettled }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const amount = formatCurrency(fine.amount)

  const settle = async (action) => {
    setBusy(true)
    setError(null)
    try {
      if (action === 'pay') {
        await fineApi.pay(fine.id)
        onSettled(`Đã thu ${amount} của ${fine.memberName}.`)
      } else {
        await fineApi.waive(fine.id)
        onSettled(`Đã miễn khoản phạt ${amount} cho ${fine.memberName}.`)
      }
    } catch (err) {
      setError(err.message ?? 'Không lưu được. Thử lại.')
      setBusy(false)
    }
  }

  return (
    <Modal title="Thu tiền phạt" onClose={onClose}>
      <div className="title">{fine.title}</div>
      <div className="meta">{fine.memberName}</div>
      <div className="calc">
        <div className="row"><span>Hạn trả</span><span>{formatDate(fine.dueAt)}</span></div>
        <div className="row"><span>Ngày nhận lại sách</span><span>{formatDate(fine.returnedAt)}</span></div>
        <div className="row"><span>Trễ</span><span>{fine.overdueDays} ngày</span></div>
        <div className="row total"><span>Cần thu</span><span>{amount}</span></div>
      </div>
      {error && <p className="err" role="alert" style={{ marginTop: 12 }}>{error}</p>}
      <div className="modal-actions">
        <button type="button" className="btn" onClick={onClose} disabled={busy}>Để sau</button>
        <button type="button" className="btn" onClick={() => settle('waive')} disabled={busy}>Miễn phạt</button>
        <button type="button" className="btn primary" onClick={() => settle('pay')} disabled={busy}>
          Đã thu {amount}
        </button>
      </div>
    </Modal>
  )
}
