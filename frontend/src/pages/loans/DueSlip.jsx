import { lateDays, stampDate } from './loanDates.js'

const ROWS = 6

/**
 * The due-date slip glued inside the book: every loan of this book, oldest first, with the due date stamped on
 * the left and the return date on the right. The loan being handled now is stamped last, with fresh ink.
 * `loans` = this book's loans, `current` = id of the loan just lent or being returned, `fresh` = animate its stamp.
 */
export default function DueSlip({ book, member, loans, current, fresh, preview }) {
  if (!book) {
    return (
      <div className="slip">
        <SlipHead />
        <p className="note" style={{ textAlign: 'center', padding: '24px 0' }}>
          Chọn sách để xem phiếu hạn trả.
        </p>
      </div>
    )
  }

  const history = [...loans].sort((a, b) => new Date(a.borrowedAt) - new Date(b.borrowedAt)).slice(-ROWS)
  const empty = Array.from({ length: Math.max(0, ROWS - history.length) })
  const focus = history.find((l) => l.id === current)
  const returning = preview && focus?.status !== 'RETURNED' ? focus : null
  const big = bigStamp(focus, returning)

  return (
    <div className="slip" aria-label={`Phiếu hạn trả của sách ${book.title}`}>
      <SlipHead />
      <div className="slip-book">
        <span>{book.title}</span>
        <span>{member ? member.libraryCardNo : book.isbn}</span>
      </div>
      <div className="slip-rows">
        <div className="col">
          <div className="lbl">Hạn trả</div>
          {history.map((l) => (
            <div className="cell" key={l.id}>
              <span className={inkClass(l, current, fresh, l.status === 'OVERDUE')}>{stampDate(l.dueAt)}</span>
            </div>
          ))}
          {empty.map((_, i) => <div className="cell" key={`d${i}`} />)}
        </div>
        <div className="col">
          <div className="lbl">Ngày trả</div>
          {history.map((l) => {
            const returnedAt = l === returning ? new Date() : l.returnedAt
            return (
              <div className="cell" key={l.id}>
                {returnedAt && (
                  <span className={inkClass(l, current, fresh || l === returning, lateDays(l, returnedAt) > 0)}>
                    {stampDate(returnedAt)}
                  </span>
                )}
              </div>
            )
          })}
          {empty.map((_, i) => <div className="cell" key={`r${i}`} />)}
        </div>
      </div>
      {big && <div key={`${current}-${big.text}`} className={`bigstamp on ${big.tone}`}>{big.text}</div>}
    </div>
  )
}

function SlipHead() {
  return (
    <div className="slip-head">
      <div className="t">Phiếu hạn trả</div>
      <div className="s">Vui lòng trả sách đúng hạn ghi dưới đây</div>
    </div>
  )
}

function inkClass(loan, current, fresh, late) {
  const classes = ['ink']
  if (loan.id !== current) classes.push('old')
  if (late) classes.push('red')
  if (loan.id === current && fresh) classes.push('new')
  return classes.join(' ')
}

function bigStamp(focus, returning) {
  if (!focus) return null
  if (returning) {
    const late = lateDays(returning, new Date())
    return late > 0 ? { text: `TRỄ ${late} NGÀY`, tone: '' } : null
  }
  if (focus.status === 'RETURNED') return { text: 'ĐÃ TRẢ', tone: 'paid' }
  if (focus.status === 'OVERDUE') return { text: 'QUÁ HẠN', tone: '' }
  return null
}
