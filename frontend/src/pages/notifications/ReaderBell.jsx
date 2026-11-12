import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import BellMenu, { BellIcon } from '../../components/BellMenu.jsx'
import Toast from '../../components/Toast.jsx'
import useNotificationActions from '../../hooks/useNotificationActions.js'
import { formatDateTime, formatRelative } from '../../utils/format.js'
import KindIcon from './KindIcon.jsx'
import { kindOf, shownAt, visibleToReader } from './notificationKinds.js'

const LIMIT = 6

/** The reader's bell: unread count, and the latest reminders in a drop-down. */
export default function ReaderBell({ notifications }) {
  const [tab, setTab] = useState('all')
  const [notice, setNotice] = useState(null)
  const clearNotice = useCallback(() => setNotice(null), [])
  const { markRead, markAllRead } = useNotificationActions(notifications, setNotice)

  const all = (notifications.data ?? []).filter((n) => visibleToReader(n))
    .sort((a, b) => new Date(shownAt(b)) - new Date(shownAt(a)))
  const unread = all.filter((n) => !n.readAt)
  const list = (tab === 'unread' ? unread : all).slice(0, LIMIT)

  return (
    <>
      <BellMenu
        count={unread.length}
        label="Thông báo"
        title="Thông báo"
        actions={(
          <button type="button" className="link-btn" disabled={unread.length === 0} onClick={() => markAllRead(all)}>
            Đánh dấu tất cả đã đọc
          </button>
        )}
        footer={({ close }) => <Link to="/me/notifications" onClick={close}>Xem tất cả thông báo</Link>}
      >
        {({ close }) => (
          <>
            <div className="bell-tabs" role="group" aria-label="Lọc thông báo">
              <button type="button" aria-pressed={tab === 'all'} onClick={() => setTab('all')}>Tất cả</button>
              <button type="button" aria-pressed={tab === 'unread'} onClick={() => setTab('unread')}>
                Chưa đọc{unread.length > 0 && ` (${unread.length})`}
              </button>
            </div>

            {notifications.error && <p className="bell-empty error">Không tải được thông báo.</p>}
            {!notifications.error && list.length === 0 && (
              <div className="bell-empty">
                <BellIcon size={34} />
                <p>{tab === 'unread' ? 'Bạn đã đọc hết thông báo.' : 'Chưa có thông báo nào.'}</p>
              </div>
            )}

            <ul className="bell-list">
              {list.map((n) => {
                const kind = kindOf(n.type)
                return (
                  <li key={n.id}>
                    <Link to="/me/loans" className={`bell-item${n.readAt ? '' : ' unread'}`}
                      onClick={() => { markRead(n); close() }}>
                      <KindIcon type={n.type} />
                      <span className="bell-text">
                        <span className="bell-subject">{n.subject}</span>
                        <span className="bell-msg">{n.message}</span>
                        <span className={`bell-time ${kind.tone}`} title={formatDateTime(shownAt(n))}>
                          {kind.label} · {formatRelative(shownAt(n))}
                        </span>
                      </span>
                      {!n.readAt && <span className="unread-dot" aria-label="Chưa đọc" />}
                    </Link>
                  </li>
                )
              })}
            </ul>
          </>
        )}
      </BellMenu>
      <Toast notice={notice} onDone={clearNotice} />
    </>
  )
}
