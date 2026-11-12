import { useCallback, useMemo, useState } from 'react'
import bookApi from '../../api/bookApi.js'
import loanApi from '../../api/loanApi.js'
import memberApi from '../../api/memberApi.js'
import DataTable from '../../components/DataTable.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import Toast from '../../components/Toast.jsx'
import useFetch from '../../hooks/useFetch.js'
import { formatDate } from '../../utils/format.js'
import BorrowPanel from './BorrowPanel.jsx'
import DueSlip from './DueSlip.jsx'
import ReturnPanel from './ReturnPanel.jsx'
import { dueHint } from './loanDates.js'

const listOf = (result) => (result.status === 'fulfilled' ? result.value : [])

// Loans come from loan-service; titles and names from the catalog and member services. When one of those is
// down the list still shows, with ids in place of names (and lending is not possible until it is back).
const fetchDesk = async () => {
  const lookups = Promise.allSettled([memberApi.getAll(), bookApi.getAll()])
  const loans = await loanApi.getAll()
  const [memberResult, bookResult] = await lookups
  const members = listOf(memberResult)
  const books = listOf(bookResult)
  const memberById = new Map(members.map((m) => [m.id, m]))
  const bookById = new Map(books.map((b) => [b.id, b]))
  return {
    members,
    books,
    lookupsFailed: memberResult.status === 'rejected' || bookResult.status === 'rejected',
    loans: loans.map((l) => ({
      ...l,
      title: bookById.get(l.bookId)?.title ?? `Sách #${l.bookId}`,
      isbn: bookById.get(l.bookId)?.isbn,
      memberName: memberById.get(l.memberId)?.fullName ?? `Thành viên #${l.memberId}`,
      cardNo: memberById.get(l.memberId)?.libraryCardNo ?? '',
    })),
  }
}

const FILTERS = [
  { value: '', label: 'Tất cả' },
  { value: 'BORROWED', label: 'Đang mượn' },
  { value: 'OVERDUE', label: 'Quá hạn' },
  { value: 'RETURNED', label: 'Đã trả' },
]

const EMPTY_FOCUS = { bookId: null, memberId: null, current: null, fresh: false }

export default function LoanListPage() {
  const { data, loading, error, reload } = useFetch(fetchDesk)
  const [mode, setMode] = useState('borrow') // 'borrow' | 'return'
  const [focus, setFocus] = useState(EMPTY_FOCUS) // what the due slip shows
  const [returning, setReturning] = useState(null) // the open loan picked for return
  const [filter, setFilter] = useState('')
  const [query, setQuery] = useState('')
  const [notice, setNotice] = useState(null)
  const clearNotice = useCallback(() => setNotice(null), [])

  const members = data?.members ?? []
  const books = data?.books ?? []
  const loans = useMemo(() => data?.loans ?? [], [data])

  // From the borrow form. Clearing the book after a successful borrow keeps the slip of the loan just made.
  const handleBorrowSelect = useCallback(({ memberId, bookId }) => {
    setFocus((f) => {
      if (bookId == null && f.current != null && f.memberId === memberId) return f
      if (f.bookId === bookId && f.memberId === memberId) return f
      return { ...EMPTY_FOCUS, bookId, memberId }
    })
  }, [])

  const pickForReturn = (loan) => {
    setReturning(loan)
    if (loan) setFocus({ bookId: loan.bookId, memberId: loan.memberId, current: loan.id, fresh: false })
  }

  const switchMode = (next) => {
    setMode(next)
    setReturning(null)
    setFocus(EMPTY_FOCUS)
  }

  const handleDone = (loan, text) => {
    setReturning(null)
    setFocus({ bookId: loan.bookId, memberId: loan.memberId, current: loan.id, fresh: true })
    setNotice({ text, kind: 'ok' })
    reload()
  }

  const startReturnFromRow = (loan) => {
    setMode('return')
    pickForReturn(loan)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase()
    return loans.filter((l) => (!filter || l.status === filter)
      && (!q || [l.title, l.isbn, l.memberName, l.cardNo].some((v) => v?.toLowerCase().includes(q))))
  }, [loans, filter, query])

  const columns = [
    {
      key: 'title',
      label: 'Sách và thành viên',
      render: (l) => (
        <div>
          <div className="title">{l.title}</div>
          <div className="meta">{l.memberName}{l.cardNo ? ` · ${l.cardNo}` : ''}</div>
        </div>
      ),
    },
    { key: 'borrowedAt', label: 'Ngày mượn', render: (l) => formatDate(l.borrowedAt) },
    {
      key: 'dueAt',
      label: 'Hạn trả',
      render: (l) => (
        <div>
          {formatDate(l.dueAt)}
          {dueHint(l) && <div className={`meta${l.status === 'OVERDUE' ? ' error' : ''}`}>{dueHint(l)}</div>}
        </div>
      ),
    },
    { key: 'returnedAt', label: 'Ngày trả', render: (l) => formatDate(l.returnedAt) },
    {
      key: 'status',
      label: 'Trạng thái',
      render: (l) => (
        <div className="row-actions" style={{ justifyContent: 'flex-start' }}>
          <StatusBadge status={l.status} />
          {l.loanType === 'DIGITAL' && <span className="st digital">Sách số</span>}
        </div>
      ),
    },
    {
      key: 'actions',
      label: '',
      render: (l) => (l.status !== 'RETURNED' ? (
        <div className="row-actions">
          <button type="button" className="btn" onClick={() => startReturnFromRow(l)}>Nhận trả</button>
        </div>
      ) : null),
    },
  ]

  // The slip shows one loan: the one being handled, or else this member's unreturned loan of the chosen book.
  const slipLoans = useMemo(() => {
    if (focus.current != null) return loans.filter((l) => l.id === focus.current)
    const open = loans.find((l) => l.bookId === focus.bookId && l.memberId === focus.memberId
      && l.status !== 'RETURNED')
    return open ? [open] : []
  }, [loans, focus])

  const slipBook = books.find((b) => b.id === focus.bookId)
    ?? (focus.bookId != null ? { id: focus.bookId, title: `Sách #${focus.bookId}`, isbn: '' } : null)

  return (
    <section>
      <PageHead title="Mượn và trả" subtitle="Ghi phiếu mượn và nhận lại sách tại quầy." />
      <PageState loading={loading} error={error}>
        {data?.lookupsFailed && (
          <p className="error" role="alert">
            Không tải được danh sách thành viên hoặc sách. Danh sách phiếu vẫn hiện, nhưng chưa cho mượn được.
          </p>
        )}

        <div className="grid2 desk">
          <div className="panel">
            <div className="seg" role="group" aria-label="Việc ở quầy">
              <button type="button" aria-pressed={mode === 'borrow'} onClick={() => switchMode('borrow')}>Cho mượn</button>
              <button type="button" aria-pressed={mode === 'return'} onClick={() => switchMode('return')}>Nhận trả</button>
            </div>
            {mode === 'borrow' ? (
              <BorrowPanel members={members} books={books} loans={loans}
                onSelect={handleBorrowSelect} onBorrowed={handleDone} />
            ) : (
              <ReturnPanel loans={loans} selected={returning} onSelect={pickForReturn} onReturned={handleDone} />
            )}
          </div>
          <DueSlip book={slipBook} member={members.find((m) => m.id === focus.memberId)}
            loans={slipLoans} current={focus.current} fresh={focus.fresh}
            preview={mode === 'return' && returning != null} />
        </div>

        <h2 style={{ marginTop: 32 }}>Phiếu mượn</h2>
        <div className="toolbar">
          <input type="search" value={query} onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo tên sách, ISBN, thành viên hoặc số thẻ" aria-label="Tìm phiếu mượn" />
          <div className="seg" role="group" aria-label="Lọc theo trạng thái" style={{ marginBottom: 0 }}>
            {FILTERS.map((f) => (
              <button key={f.value} type="button" aria-pressed={filter === f.value} onClick={() => setFilter(f.value)}>
                {f.label}
              </button>
            ))}
          </div>
        </div>
        <DataTable columns={columns} rows={rows} rowClassName={(l) => (l.status === 'OVERDUE' ? 'is-late' : undefined)}
          emptyText={query || filter ? 'Không có phiếu nào khớp bộ lọc.' : 'Chưa có phiếu mượn nào.'} />
      </PageState>
      <Toast notice={notice} onDone={clearNotice} />
    </section>
  )
}
