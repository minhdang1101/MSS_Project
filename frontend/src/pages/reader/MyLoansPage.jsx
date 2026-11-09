import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import BookCover from '../../components/BookCover.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import { formatDate } from '../../utils/format.js'
import { dueHint } from '../loans/loanDates.js'
import useBooks, { unknownBook } from './useBooks.js'

const TABS = [
  { key: 'open', label: 'Đang mượn', keep: (l) => l.status !== 'RETURNED' },
  { key: 'returned', label: 'Đã trả', keep: (l) => l.status === 'RETURNED' },
  { key: 'all', label: 'Tất cả', keep: () => true },
]

/** The reader's own loans: what they hold now, when it is due, and what they already returned. */
export default function MyLoansPage() {
  const { loans } = useOutletContext()
  const { byId } = useBooks()
  const [tab, setTab] = useState('open')

  const all = loans.data ?? []
  const active = TABS.find((t) => t.key === tab)
  const rows = all.filter(active.keep).sort((a, b) => new Date(a.dueAt) - new Date(b.dueAt))
  const overdue = all.filter((l) => l.status === 'OVERDUE').length

  return (
    <section>
      <PageHead title="Sách đang mượn" subtitle="Các cuốn bạn đang giữ, hạn trả và lịch sử mượn." />
      <PageState loading={loans.loading} error={loans.error}>
        {overdue > 0 && (
          <div className="banner late" role="status">
            Bạn có <b>{overdue}</b> cuốn đã quá hạn. Hãy trả sách sớm để tránh tăng tiền phạt.
          </div>
        )}
        <div className="seg" role="group" aria-label="Lọc theo trạng thái">
          {TABS.map((t) => (
            <button key={t.key} type="button" aria-pressed={t.key === tab} onClick={() => setTab(t.key)}>{t.label}</button>
          ))}
        </div>

        {rows.length === 0 ? (
          <p className="panel empty">
            {all.length === 0 ? 'Bạn chưa mượn cuốn sách nào. Đến quầy thủ thư để mượn sách.' : 'Không có sách nào trong mục này.'}
          </p>
        ) : (
          <div className="panel tablewrap">
            <table>
              <thead>
                <tr><th>Sách</th><th>Ngày mượn</th><th>Hạn trả</th><th>Ngày trả</th><th>Trạng thái</th></tr>
              </thead>
              <tbody>
                {rows.map((loan) => {
                  const book = byId.get(loan.bookId) ?? unknownBook(loan.bookId)
                  const hint = dueHint(loan)
                  return (
                    <tr key={loan.id} className={loan.status === 'OVERDUE' ? 'is-late' : undefined}>
                      <td>
                        <div className="book-line-cell">
                          <BookCover book={book} size="sm" />
                          <div><div className="title">{book.title}</div><div className="meta">{book.author}</div></div>
                        </div>
                      </td>
                      <td>{formatDate(loan.borrowedAt)}</td>
                      <td>{formatDate(loan.dueAt)}{hint && <div className="meta">{hint}</div>}</td>
                      <td>{formatDate(loan.returnedAt)}</td>
                      <td><StatusBadge status={loan.status} /></td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </PageState>
    </section>
  )
}
