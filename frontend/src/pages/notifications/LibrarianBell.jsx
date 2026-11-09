import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import notificationApi from '../../api/notificationApi.js'
import BellMenu, { BellIcon } from '../../components/BellMenu.jsx'
import Toast from '../../components/Toast.jsx'
import useFetch from '../../hooks/useFetch.js'
import { formatDateTime, formatRelative } from '../../utils/format.js'
import KindIcon from './KindIcon.jsx'
import { NOTIFICATIONS_CHANGED } from './notificationKinds.js'

const POLL_MS = 60000
const LIMIT = 6
const fetchFailed = () => notificationApi.getAll({ status: 'FAILED' })

/** The librarian's bell: reminders that could not be sent and need a retry. */
export default function LibrarianBell() {
  const failed = useFetch(fetchFailed)
  const { setData } = failed
  const [retrying, setRetrying] = useState(null)
  const [notice, setNotice] = useState(null)
  const clearNotice = useCallback(() => setNotice(null), [])

  useEffect(() => {
    const refresh = () => fetchFailed().then(setData).catch(() => {})
    const timer = setInterval(refresh, POLL_MS)
    window.addEventListener(NOTIFICATIONS_CHANGED, refresh)
    return () => {
      clearInterval(timer)
      window.removeEventListener(NOTIFICATIONS_CHANGED, refresh)
    }
  }, [setData])

  const list = (failed.data ?? []).slice().sort((a, b) => new Date(b.scheduledAt) - new Date(a.scheduledAt))

  const retry = async (n) => {
    setRetrying(n.id)
    try {
      await notificationApi.retry(n.id)
      setData((items) => (items ?? []).filter((x) => x.id !== n.id))
      setNotice({ text: `Đã đưa thông báo #${n.id} vào hàng đợi để gửi lại.`, kind: 'ok' })
    } catch (err) {
      setNotice({ text: err.message ?? 'Không gửi lại được.', kind: 'error' })
    } finally {
      setRetrying(null)
    }
  }

  return (
    <>
      <BellMenu
        count={list.length}
        label="Thông báo gửi lỗi"
        title="Nhắc hạn gửi lỗi"
        footer={({ close }) => (
          <Link to="/notifications?status=FAILED" onClick={close}>Mở trang Nhắc hạn</Link>
        )}
      >
        {() => (
          <>
            {failed.error && <p className="bell-empty error">Không tải được thông báo.</p>}
            {!failed.error && list.length === 0 && (
              <div className="bell-empty">
                <BellIcon size={34} />
                <p>Mọi lời nhắc đều đã gửi được. Không có gì cần xử lý.</p>
              </div>
            )}
            <ul className="bell-list">
              {list.slice(0, LIMIT).map((n) => (
                <li key={n.id}>
                  <div className="bell-item unread">
                    <KindIcon type={n.type} />
                    <span className="bell-text">
                      <span className="bell-subject">{n.recipient}</span>
                      <span className="bell-msg error">{n.lastError ?? 'Không rõ lỗi'}</span>
                      <span className="bell-time" title={formatDateTime(n.scheduledAt)}>
                        Lượt mượn #{n.loanId} · {n.attempts} lần thử · {formatRelative(n.scheduledAt)}
                      </span>
                    </span>
                    <button type="button" className="btn small" disabled={retrying === n.id} onClick={() => retry(n)}>
                      {retrying === n.id ? 'Đang gửi...' : 'Gửi lại'}
                    </button>
                  </div>
                </li>
              ))}
            </ul>
            {list.length > LIMIT && <p className="bell-more">Còn {list.length - LIMIT} thông báo lỗi khác.</p>}
          </>
        )}
      </BellMenu>
      <Toast notice={notice} onDone={clearNotice} />
    </>
  )
}
