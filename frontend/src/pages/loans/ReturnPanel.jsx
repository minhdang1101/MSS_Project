import { useEffect, useMemo, useState } from 'react'
import loanApi from '../../api/loanApi.js'
import StatusBadge from '../../components/StatusBadge.jsx'
import { formatDate } from '../../utils/format.js'
import { dueHint, lateDays } from './loanDates.js'

const MAX_RESULTS = 8

const matches = (loan, q) =>
  [loan.title, loan.isbn, loan.memberName, loan.cardNo].some((v) => v?.toLowerCase().includes(q))

/**
 * Take a book back: find the open loan (by card, member name, book title or ISBN), check the dates, confirm.
 * `selected` can also be set from the loan table ("Nhận trả" on a row).
 */
export default function ReturnPanel({ loans, selected, onSelect, onReturned }) {
  const [query, setQuery] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  const open = useMemo(() => loans.filter((l) => l.status !== 'RETURNED'), [loans])
  const results = useMemo(() => {
    const q = query.trim().toLowerCase()
    const found = q ? open.filter((l) => matches(l, q)) : open.filter((l) => l.status === 'OVERDUE')
    return found.slice(0, MAX_RESULTS)
  }, [open, query])

  useEffect(() => setError(null), [selected])

  const confirm = async () => {
    setBusy(true)
    setError(null)
    try {
      const loan = await loanApi.returnBook(selected.id)
      const late = lateDays(loan)
      onReturned(loan, late > 0
        ? `Đã nhận lại “${selected.title}”, trễ ${late} ngày. Tiền phạt sẽ hiện ở mục Tiền phạt.`
        : `Đã nhận lại “${selected.title}” đúng hạn.`)
    } catch (err) {
      setError(err.message ?? 'Không lưu được. Thử lại.')
    } finally {
      setBusy(false)
    }
  }

  if (selected) {
    const late = lateDays(selected, new Date())
    return (
      <div>
        <div className="title">{selected.title}</div>
        <div className="meta">{selected.memberName} · {selected.cardNo}</div>
        <div className="calc">
          <div className="row"><span>Ngày mượn</span><span>{formatDate(selected.borrowedAt)}</span></div>
          <div className="row"><span>Hạn trả</span><span>{formatDate(selected.dueAt)}</span></div>
          <div className="row"><span>Ngày nhận lại</span><span>Hôm nay, {formatDate(new Date())}</span></div>
          <div className="row total">
            <span>Trễ hạn</span>
            <span className={late > 0 ? 'error' : undefined}>{late > 0 ? `${late} ngày` : 'Không'}</span>
          </div>
        </div>
        {late > 0 && selected.loanType !== 'DIGITAL' && (
          <p className="note">Tiền phạt được tính theo quy định hiện hành sau khi nhận sách, xem ở mục Tiền phạt.</p>
        )}
        {error && <p className="err" role="alert" style={{ marginTop: 12 }}>{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn" onClick={() => onSelect(null)} disabled={busy}>Chọn phiếu khác</button>
          <button type="button" className="btn primary" onClick={confirm} disabled={busy}>
            {busy ? 'Đang lưu...' : 'Nhận lại sách'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div>
      <div className="field">
        <label htmlFor="return-search">Tìm phiếu đang mượn</label>
        <input id="return-search" type="search" value={query} onChange={(e) => setQuery(e.target.value)}
          placeholder="Quét mã sách hoặc thẻ, gõ tên" autoComplete="off" autoFocus />
      </div>
      {!query && results.length > 0 && <p className="meta" style={{ margin: '0 0 4px' }}>Các phiếu đang quá hạn:</p>}
      {results.length === 0 ? (
        <p className="note">{query ? `Không có phiếu đang mượn khớp với “${query}”.` : 'Gõ để tìm phiếu cần nhận trả.'}</p>
      ) : (
        <ul className="list">
          {results.map((l) => (
            <li key={l.id}>
              <div>
                <div className="title">{l.title}</div>
                <div className="meta">{l.memberName} · hạn {formatDate(l.dueAt)} ({dueHint(l)})</div>
              </div>
              <div className="right">
                <StatusBadge status={l.status} />
                <div><button type="button" className="btn" style={{ marginTop: 6 }} onClick={() => onSelect(l)}>Chọn</button></div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
