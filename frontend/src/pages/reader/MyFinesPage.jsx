import { useOutletContext } from 'react-router-dom'
import BookCover from '../../components/BookCover.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import { formatCurrency, formatDate } from '../../utils/format.js'
import useBooks, { unknownBook } from './useBooks.js'

const OWED = ['ACCRUING', 'UNPAID']
const RANK = { ACCRUING: 0, UNPAID: 1, PAID: 2, WAIVED: 3 }

/** The reader's fines: what they still owe on top, then what was settled. */
export default function MyFinesPage() {
  const { fines } = useOutletContext()
  const { byId } = useBooks()

  const all = fines.data ?? []
  const rows = [...all].sort((a, b) => (RANK[a.status] ?? 9) - (RANK[b.status] ?? 9) || b.id - a.id)
  const owed = all.filter((f) => OWED.includes(f.status))
  const total = owed.reduce((sum, f) => sum + Number(f.amount), 0)
  const growing = all.filter((f) => f.status === 'ACCRUING').length

  return (
    <section>
      <PageHead title="Tiền phạt" subtitle="Các khoản phạt do trả sách trễ hạn." />
      <PageState loading={fines.loading} error={fines.error}>
        <div className="stat-row">
          <div className={`stat${total > 0 ? ' late' : ''}`}>
            <span>Cần thanh toán</span><b>{formatCurrency(total)}</b><small>thanh toán tại quầy thủ thư</small>
          </div>
          <div className="stat"><span>Khoản chưa thanh toán</span><b>{owed.length}</b><small>trong tổng {all.length} khoản</small></div>
        </div>

        {growing > 0 && (
          <div className="banner late" role="status">
            Có <b>{growing}</b> cuốn chưa trả đang bị tính phạt mỗi ngày. Trả sách sớm để dừng khoản phạt.
          </div>
        )}

        {rows.length === 0 ? (
          <p className="panel empty">Bạn không có khoản tiền phạt nào. Cảm ơn bạn đã trả sách đúng hạn!</p>
        ) : (
          <div className="panel tablewrap">
            <table>
              <thead>
                <tr>
                  <th>Sách</th><th>Hạn trả</th><th>Ngày trả</th><th className="num">Ngày trễ</th>
                  <th className="num">Mỗi ngày</th><th className="num">Số tiền</th><th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((fine) => {
                  const book = byId.get(fine.bookId) ?? unknownBook(fine.bookId)
                  return (
                    <tr key={fine.id}>
                      <td>
                        <div className="book-line-cell">
                          <BookCover book={book} size="sm" />
                          <div><div className="title">{book.title}</div><div className="meta">{book.author}</div></div>
                        </div>
                      </td>
                      <td>{formatDate(fine.dueAt)}</td>
                      <td>{fine.returnedAt ? formatDate(fine.returnedAt) : 'Chưa trả'}</td>
                      <td className="num">{fine.overdueDays}</td>
                      <td className="num">{formatCurrency(fine.dailyRate)}</td>
                      <td className="num"><b>{formatCurrency(fine.amount)}</b></td>
                      <td><StatusBadge status={fine.status} /></td>
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
