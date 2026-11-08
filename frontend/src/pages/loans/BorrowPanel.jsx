import { useEffect, useMemo, useRef, useState } from 'react'
import loanApi from '../../api/loanApi.js'
import StatusBadge from '../../components/StatusBadge.jsx'
import { formatDate } from '../../utils/format.js'

const norm = (v) => String(v ?? '').trim().toLowerCase()

// The card scanner types the card number; typing a full name or e-mail also works.
const findMember = (members, text) => {
  const q = norm(text)
  return q ? members.find((m) => [m.libraryCardNo, m.email, m.fullName].some((v) => norm(v) === q)) : null
}

// The barcode scanner types the ISBN; picking a title from the suggestions works too.
const findBook = (books, text) => {
  const q = norm(text)
  return q ? books.find((b) => norm(b.isbn) === q || norm(b.title) === q) : null
}

const initials = (name) => name.split(/\s+/).filter(Boolean).slice(-2).map((w) => w[0]).join('').toUpperCase()

/**
 * Lend a book at the desk: identify the member, then the book, then confirm. The idempotency key belongs to this
 * borrow attempt: it is kept when the librarian retries after an error and renewed once the member or book changes.
 */
export default function BorrowPanel({ members, books, loans, onSelect, onBorrowed }) {
  const [memberText, setMemberText] = useState('')
  const [bookText, setBookText] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const keyRef = useRef(crypto.randomUUID())

  const member = useMemo(() => findMember(members, memberText), [members, memberText])
  const book = useMemo(() => findBook(books, bookText), [books, bookText])

  const memberLoans = useMemo(
    () => (member ? loans.filter((l) => l.memberId === member.id && l.status !== 'RETURNED') : []),
    [loans, member],
  )
  const overdueCount = memberLoans.filter((l) => l.status === 'OVERDUE').length
  const alreadyHolding = Boolean(book && memberLoans.some((l) => l.bookId === book.id))

  // Ids, not objects: a reload after a borrow gives new objects for the same member and book.
  const memberId = member?.id ?? null
  const bookId = book?.id ?? null
  useEffect(() => {
    keyRef.current = crypto.randomUUID()
    setError(null)
    onSelect({ memberId, bookId })
  }, [memberId, bookId, onSelect])

  const blocker = !member ? 'Chưa xác định thành viên.'
    : member.status !== 'ACTIVE' ? 'Thẻ không ở trạng thái hoạt động nên không thể mượn.'
      : !book ? 'Chưa chọn sách.'
        : book.status === 'ARCHIVED' ? 'Sách đã ngừng cho mượn.'
          : book.availableCopies <= 0 ? 'Sách này hiện không còn bản nào trên kệ.'
            : alreadyHolding ? 'Thành viên đang mượn cuốn này và chưa trả.'
              : null

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (blocker || busy) return
    setBusy(true)
    setError(null)
    try {
      const loan = await loanApi.borrow({ idempotencyKey: keyRef.current, memberId: member.id, bookId: book.id })
      onBorrowed(loan, `Đã cho ${member.fullName} mượn “${book.title}”. Hạn trả ${formatDate(loan.dueAt)}.`)
      setBookText('') // the same member often borrows several books in a row
    } catch (err) {
      setError(err.message ?? 'Không lưu được phiếu mượn. Thử lại.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="field">
        <label htmlFor="borrow-member">Thẻ thành viên</label>
        <input id="borrow-member" list="borrow-member-list" value={memberText} autoComplete="off"
          onChange={(e) => setMemberText(e.target.value)} placeholder="Quét thẻ hoặc gõ số thẻ, tên" autoFocus />
        <datalist id="borrow-member-list">
          {members.map((m) => <option key={m.id} value={m.libraryCardNo}>{m.fullName}</option>)}
        </datalist>
      </div>

      {member && (
        <div className="member-chip">
          <div className="avatar" aria-hidden="true">{initials(member.fullName)}</div>
          <div style={{ minWidth: 0 }}>
            <div><b>{member.fullName}</b> <StatusBadge status={member.status} /></div>
            <div className="meta">{member.libraryCardNo} · đang mượn {memberLoans.length} cuốn</div>
            {overdueCount > 0 && <div className="warn">Có {overdueCount} cuốn quá hạn chưa trả.</div>}
          </div>
        </div>
      )}
      {memberText && !member && <p className="note" style={{ margin: '-6px 0 14px' }}>Không tìm thấy thẻ “{memberText}”.</p>}

      <div className="field">
        <label htmlFor="borrow-book">Sách</label>
        <input id="borrow-book" list="borrow-book-list" value={bookText} autoComplete="off"
          onChange={(e) => setBookText(e.target.value)} placeholder="Quét mã ISBN hoặc gõ tên sách" />
        <datalist id="borrow-book-list">
          {books.map((b) => <option key={b.id} value={b.isbn}>{b.title}</option>)}
        </datalist>
      </div>

      {book && (
        <div className="book-line">
          <div>
            <div className="title">{book.title}</div>
            <div className="meta">{book.author}{book.publicationYear ? ` · ${book.publicationYear}` : ''}</div>
          </div>
          <div className="right">
            {book.format === 'DIGITAL' && <span className="st digital">Sách số</span>}
            <div className="meta">Còn {book.availableCopies}/{book.totalCopies} bản</div>
          </div>
        </div>
      )}
      {bookText && !book && <p className="note" style={{ margin: '-6px 0 14px' }}>Không tìm thấy sách “{bookText}”.</p>}

      {error && <p className="err" role="alert">{error}</p>}
      {!error && member && blocker && <p className="err">{blocker}</p>}

      <button type="submit" className="btn primary wide" disabled={Boolean(blocker) || busy}>
        {busy ? 'Đang lưu...' : 'Cho mượn'}
      </button>
    </form>
  )
}
