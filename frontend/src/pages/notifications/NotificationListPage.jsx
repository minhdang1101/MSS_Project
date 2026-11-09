import { useCallback, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import memberApi from '../../api/memberApi.js'
import notificationApi from '../../api/notificationApi.js'
import DataTable from '../../components/DataTable.jsx'
import Modal from '../../components/Modal.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import Toast from '../../components/Toast.jsx'
import useFetch from '../../hooks/useFetch.js'
import { formatDateTime, formatRelative } from '../../utils/format.js'
import { fold } from '../reader/useBooks.js'
import KindIcon from './KindIcon.jsx'
import { CHANNELS, NOTIFICATIONS_CHANGED, kindOf } from './notificationKinds.js'

const STATUSES = [
  { key: 'all', label: 'Tất cả' },
  { key: 'FAILED', label: 'Gửi lỗi' },
  { key: 'PENDING', label: 'Chờ gửi' },
  { key: 'SENT', label: 'Đã gửi' },
]

const TYPES = [
  { key: 'all', label: 'Mọi loại' },
  { key: 'OVERDUE', label: 'Quá hạn' },
  { key: 'DUE_SOON', label: 'Sắp đến hạn' },
]

// Notifications come from notification-service, names from member-service. When member-service is down the list
// still shows, with ids in place of names.
const fetchNotifications = async () => {
  const membersCall = Promise.allSettled([memberApi.getAll()])
  const notifications = await notificationApi.getAll()
  const [members] = await membersCall
  const memberById = new Map(members.status === 'fulfilled' ? members.value.map((m) => [m.id, m]) : [])
  return notifications.map((n) => {
    const member = memberById.get(n.memberId)
    return { ...n, memberName: member?.fullName ?? `Thành viên #${n.memberId}`, cardNo: member?.libraryCardNo ?? '' }
  })
}

/** The librarian's view of every due-date reminder: what was sent, what is waiting, and what failed. */
export default function NotificationListPage() {
  const { data, loading, error, reload, setData } = useFetch(fetchNotifications)
  // The status filter lives in the URL, so the bell can link straight to /notifications?status=FAILED
  const [params, setParams] = useSearchParams()
  const status = STATUSES.some((s) => s.key === params.get('status')) ? params.get('status') : 'all'
  const setStatus = (key) => setParams(key === 'all' ? {} : { status: key }, { replace: true })
  const [type, setType] = useState('all')
  const [query, setQuery] = useState('')
  const [viewing, setViewing] = useState(null)
  const [retrying, setRetrying] = useState(null) // id being put back in the queue
  const [notice, setNotice] = useState(null)
  const clearNotice = useCallback(() => setNotice(null), [])

  const all = useMemo(() => data ?? [], [data])
  const countBy = (key) => all.filter((n) => key === 'all' || n.status === key).length
  const failed = countBy('FAILED')

  const rows = useMemo(() => {
    const words = fold(query.trim())
    return all
      .filter((n) => status === 'all' || n.status === status)
      .filter((n) => type === 'all' || n.type === type)
      .filter((n) => !words || fold(`${n.memberName} ${n.cardNo} ${n.recipient} ${n.subject} #${n.loanId}`).includes(words))
      // Failed first (they need the librarian), then the newest
      .sort((a, b) => (b.status === 'FAILED') - (a.status === 'FAILED') || new Date(b.scheduledAt) - new Date(a.scheduledAt))
  }, [all, status, type, query])

  const retry = async (n) => {
    setRetrying(n.id)
    try {
      const updated = await notificationApi.retry(n.id)
      setData((list) => list.map((x) => (x.id === n.id ? { ...x, ...updated } : x)))
      setViewing((v) => (v?.id === n.id ? { ...v, ...updated } : v))
      window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED)) // the bell drops it from its list
      setNotice({ text: `Đã đưa thông báo #${n.id} vào hàng đợi, hệ thống sẽ gửi lại trong giây lát.`, kind: 'ok' })
    } catch (err) {
      setNotice({ text: err.message ?? 'Không gửi lại được.', kind: 'error' })
    } finally {
      setRetrying(null)
    }
  }

  const columns = [
    {
      key: 'type',
      label: 'Loại',
      render: (n) => (
        <div className="kind-cell">
          <KindIcon type={n.type} />
          <span>{kindOf(n.type).label}</span>
        </div>
      ),
    },
    {
      key: 'member',
      label: 'Thành viên',
      render: (n) => (
        <div>
          <div className="title">{n.memberName}</div>
          <div className="meta">{[n.cardNo, n.recipient].filter(Boolean).join(' · ')}</div>
        </div>
      ),
    },
    {
      key: 'subject',
      label: 'Nội dung',
      render: (n) => (
        <button type="button" className="subject-btn" onClick={() => setViewing(n)} title="Xem chi tiết">
          <span className="subject">{n.subject}</span>
          <span className="meta clamp">{n.message}</span>
        </button>
      ),
    },
    {
      key: 'time',
      label: 'Thời gian',
      render: (n) => (
        <div>
          <div>{n.sentAt ? 'Gửi' : 'Hẹn'} {formatDateTime(n.sentAt ?? n.scheduledAt)}</div>
          <div className="meta">{formatRelative(n.sentAt ?? n.scheduledAt)}</div>
        </div>
      ),
    },
    {
      key: 'status',
      label: 'Trạng thái',
      render: (n) => (
        <div className="status-cell">
          <StatusBadge status={n.status} />
          {n.status === 'FAILED' && (
            <>
              <div className="error-line" title={n.lastError ?? ''}>{n.attempts} lần thử · {n.lastError ?? 'Không rõ lỗi'}</div>
              <button type="button" className="btn small" disabled={retrying === n.id} onClick={() => retry(n)}>
                {retrying === n.id ? 'Đang gửi lại...' : 'Gửi lại'}
              </button>
            </>
          )}
          {n.status === 'SENT' && <div className="meta">{n.readAt ? `Đã đọc ${formatRelative(n.readAt)}` : 'Chưa đọc'}</div>}
        </div>
      ),
    },
  ]

  return (
    <section>
      <PageHead title="Nhắc hạn" subtitle="Email nhắc trả sách gửi cho từng lượt mượn sắp đến hạn hoặc quá hạn.">
        <button className="btn" type="button" onClick={reload} disabled={loading}>Tải lại</button>
      </PageHead>

      <PageState loading={loading} error={error}>
        <div className="strip notice-strip">
          {STATUSES.map((s) => (
            <button key={s.key} type="button" aria-pressed={status === s.key} onClick={() => setStatus(s.key)}
              className={s.key === 'FAILED' && failed > 0 ? 'late' : undefined}>
              <b>{countBy(s.key)}</b>
              <span>{s.key === 'all' ? 'Tổng thông báo' : s.label}</span>
            </button>
          ))}
        </div>

        {failed > 0 && status !== 'FAILED' && (
          <div className="banner late" role="status">
            <span><b>{failed}</b> thông báo gửi lỗi sau nhiều lần thử. Kiểm tra email người nhận rồi bấm “Gửi lại”.</span>
            <button type="button" className="btn" onClick={() => setStatus('FAILED')}>Xem thông báo lỗi</button>
          </div>
        )}

        <div className="toolbar">
          <input type="search" value={query} onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo tên, số thẻ, email hoặc mã lượt mượn" aria-label="Tìm thông báo" />
          <div className="seg" role="group" aria-label="Lọc theo loại" style={{ marginBottom: 0 }}>
            {TYPES.map((t) => (
              <button key={t.key} type="button" aria-pressed={type === t.key} onClick={() => setType(t.key)}>{t.label}</button>
            ))}
          </div>
        </div>

        <DataTable columns={columns} rows={rows}
          rowClassName={(n) => (n.status === 'FAILED' ? 'row-failed' : undefined)}
          emptyText={all.length ? 'Không có thông báo nào khớp bộ lọc.' : 'Chưa có lời nhắc nào.'} />
      </PageState>

      {viewing && (
        <Modal title={`Thông báo #${viewing.id}`} onClose={() => setViewing(null)}>
          <div className="notice-detail">
            <div className="notice-top">
              <StatusBadge status={viewing.type} />
              <StatusBadge status={viewing.status} />
            </div>
            <h3 className="notice-subject">{viewing.subject}</h3>
            <p className="notice-message">{viewing.message}</p>
            <dl className="detail-list">
              <dt>Người nhận</dt><dd>{viewing.memberName} · {viewing.recipient}</dd>
              <dt>Kênh</dt><dd>{CHANNELS[viewing.channel] ?? viewing.channel}</dd>
              <dt>Lượt mượn</dt><dd>#{viewing.loanId}</dd>
              <dt>Hẹn gửi</dt><dd>{formatDateTime(viewing.scheduledAt)}</dd>
              <dt>Đã gửi</dt><dd>{viewing.sentAt ? formatDateTime(viewing.sentAt) : 'Chưa'}</dd>
              <dt>Số lần thử</dt><dd>{viewing.attempts}</dd>
              {viewing.lastError && (<><dt>Lỗi gần nhất</dt><dd className="error-line">{viewing.lastError}</dd></>)}
              <dt>Độc giả đã đọc</dt><dd>{viewing.readAt ? formatDateTime(viewing.readAt) : 'Chưa'}</dd>
            </dl>
          </div>
          <div className="modal-actions">
            {viewing.status === 'FAILED' && (
              <button type="button" className="btn primary" disabled={retrying === viewing.id} onClick={() => retry(viewing)}>
                Gửi lại
              </button>
            )}
            <button type="button" className="btn" onClick={() => setViewing(null)}>Đóng</button>
          </div>
        </Modal>
      )}
      <Toast notice={notice} onDone={clearNotice} />
    </section>
  )
}
