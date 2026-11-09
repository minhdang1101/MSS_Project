import { useCallback, useMemo, useState } from 'react'
import { Link, useOutletContext } from 'react-router-dom'
import BookCover from '../../components/BookCover.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import Toast from '../../components/Toast.jsx'
import useNotificationActions from '../../hooks/useNotificationActions.js'
import { formatDateTime, formatRelative } from '../../utils/format.js'
import KindIcon from '../notifications/KindIcon.jsx'
import { kindOf, shownAt, visibleToReader } from '../notifications/notificationKinds.js'
import useBooks, { unknownBook } from './useBooks.js'

const FILTERS = [
  { key: 'all', label: 'Tất cả', keep: () => true },
  { key: 'unread', label: 'Chưa đọc', keep: (n) => !n.readAt },
  { key: 'OVERDUE', label: 'Quá hạn', keep: (n) => n.type === 'OVERDUE' },
  { key: 'DUE_SOON', label: 'Sắp đến hạn', keep: (n) => n.type === 'DUE_SOON' },
]

const EMPTY = {
  all: 'Bạn chưa có thông báo nào. Khi sách sắp đến hạn hoặc quá hạn, thư viện sẽ nhắc bạn ở đây.',
  unread: 'Bạn đã đọc hết thông báo.',
  OVERDUE: 'Không có cuốn nào quá hạn. Cảm ơn bạn đã trả sách đúng hạn!',
  DUE_SOON: 'Không có cuốn nào sắp đến hạn.',
}

const DAY = 24 * 60 * 60 * 1000

/** "Hôm nay", "Hôm qua", "7 ngày qua" or "Cũ hơn", counted in calendar days on this computer. */
const groupOf = (value, today) => {
  const day = new Date(value)
  day.setHours(0, 0, 0, 0)
  const daysAgo = Math.round((today - day) / DAY)
  if (daysAgo <= 0) return 'Hôm nay'
  if (daysAgo === 1) return 'Hôm qua'
  if (daysAgo < 7) return '7 ngày qua'
  return 'Cũ hơn'
}

/** The reader's inbox of due-date reminders: newest first, grouped by day, unread ones highlighted. */
export default function MyNotificationsPage() {
  const { notifications, loans } = useOutletContext()
  const { byId: bookById } = useBooks()
  const [filter, setFilter] = useState('all')
  const [notice, setNotice] = useState(null)
  const clearNotice = useCallback(() => setNotice(null), [])

  const all = useMemo(
    () => (notifications.data ?? []).filter((n) => visibleToReader(n))
      .sort((a, b) => new Date(shownAt(b)) - new Date(shownAt(a))),
    [notifications.data],
  )
  const loanById = useMemo(() => new Map((loans.data ?? []).map((l) => [l.id, l])), [loans.data])
  const unread = all.filter((n) => !n.readAt)

  const active = FILTERS.find((f) => f.key === filter)
  const rows = all.filter(active.keep)

  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const groups = []
  for (const n of rows) {
    const label = groupOf(shownAt(n), today)
    const last = groups[groups.length - 1]
    if (last?.label === label) last.items.push(n)
    else groups.push({ label, items: [n] })
  }

  // Same list as the bell (both read ReaderLayout's data), so reading here also lowers the bell's count.
  const { markRead, markAllRead: markAll } = useNotificationActions(notifications, setNotice)
  const markAllRead = () => markAll(all)

  const bookFor = (n) => {
    const loan = loanById.get(n.loanId)
    return loan ? bookById.get(loan.bookId) ?? unknownBook(loan.bookId) : null
  }

  const overdueUnread = unread.filter((n) => n.type === 'OVERDUE').length

  return (
    <section>
      <PageHead title="Thông báo" subtitle="Lời nhắc hạn trả sách thư viện đã gửi tới bạn.">
        <button className="btn" type="button" onClick={markAllRead} disabled={unread.length === 0}>
          Đánh dấu tất cả đã đọc
        </button>
      </PageHead>

      <PageState loading={notifications.loading} error={notifications.error}>
        {overdueUnread > 0 && (
          <div className="banner late" role="status">
            <span>Bạn có <b>{overdueUnread}</b> thông báo quá hạn chưa đọc. Trả sách sớm để tránh tăng tiền phạt.</span>
            <Link className="btn" to="/me/loans">Xem sách đang mượn</Link>
          </div>
        )}

        <div className="chips" role="group" aria-label="Lọc thông báo">
          {FILTERS.map((f) => {
            const count = all.filter(f.keep).length
            return (
              <button key={f.key} type="button" className="chip" aria-pressed={f.key === filter} onClick={() => setFilter(f.key)}>
                {f.label}
                <span className="chip-count">{count}</span>
              </button>
            )
          })}
        </div>

        {rows.length === 0 ? (
          <p className="panel empty">{EMPTY[filter]}</p>
        ) : (
          groups.map((group) => (
            <div className="inbox-group" key={group.label}>
              <h2 className="inbox-day">{group.label}</h2>
              <ul className="inbox">
                {group.items.map((n) => {
                  const kind = kindOf(n.type)
                  const book = bookFor(n)
                  return (
                    <li key={n.id} className={`inbox-item ${kind.tone}${n.readAt ? '' : ' unread'}`}>
                      <KindIcon type={n.type} />
                      <div className="notice-body">
                        <div className="notice-top">
                          <span className={`st ${kind.tone}`}>{kind.label}</span>
                          <time dateTime={shownAt(n)} title={formatDateTime(shownAt(n))}>{formatRelative(shownAt(n))}</time>
                          {!n.readAt && <span className="unread-dot" aria-label="Chưa đọc" />}
                        </div>
                        <h3 className="notice-subject">{n.subject}</h3>
                        <p className="notice-message">{n.message}</p>
                        {book && (
                          <div className="notice-book">
                            <BookCover book={book} size="sm" />
                            <div><div className="title">{book.title}</div><div className="meta">{book.author}</div></div>
                          </div>
                        )}
                        <div className="notice-actions">
                          <Link className="btn" to="/me/loans" onClick={() => markRead(n)}>Xem phiếu mượn</Link>
                          {!n.readAt && (
                            <button type="button" className="link-btn" onClick={() => markRead(n)}>Đánh dấu đã đọc</button>
                          )}
                        </div>
                      </div>
                    </li>
                  )
                })}
              </ul>
            </div>
          ))
        )}
      </PageState>
      <Toast notice={notice} onDone={clearNotice} />
    </section>
  )
}
