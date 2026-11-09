import notificationApi from '../api/notificationApi.js'

/**
 * Mark-as-read for the reader, shared by the bell and the notifications page so both stay in step.
 * `notifications` is the useFetch result kept by ReaderLayout. Each change shows at once, then goes to the server,
 * and is put back if the server refuses. onResult({ text, kind }) reports what happened (for a Toast).
 */
export default function useNotificationActions(notifications, onResult = () => {}) {
  const setRead = (ids, readAt) =>
    notifications.setData((list) => (list ?? []).map((n) => (ids.includes(n.id) ? { ...n, readAt } : n)))

  const markRead = async (n) => {
    if (n.readAt) return
    setRead([n.id], new Date().toISOString())
    try {
      await notificationApi.markRead(n.id)
    } catch (err) {
      setRead([n.id], null)
      onResult({ text: err.message ?? 'Không đánh dấu được thông báo.', kind: 'error' })
    }
  }

  const markAllRead = async (items) => {
    const ids = items.filter((n) => !n.readAt).map((n) => n.id)
    if (ids.length === 0) return
    setRead(ids, new Date().toISOString())
    const results = await Promise.allSettled(ids.map((id) => notificationApi.markRead(id)))
    const failed = ids.filter((_, i) => results[i].status === 'rejected')
    if (failed.length) {
      setRead(failed, null)
      onResult({ text: `Không đánh dấu được ${failed.length} thông báo. Hãy thử lại.`, kind: 'error' })
    } else {
      onResult({ text: 'Đã đánh dấu tất cả là đã đọc.', kind: 'ok' })
    }
  }

  return { markRead, markAllRead }
}
