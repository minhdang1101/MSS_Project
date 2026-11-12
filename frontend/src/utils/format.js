export const formatDate = (value) => (value ? new Date(value).toLocaleDateString('vi-VN') : '-')

// "14:52 09/11/2026": for events where the hour matters (a reminder was sent, a book is due)
export const formatDateTime = (value) =>
  value
    ? new Date(value).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })
    : '-'

// "Vừa xong", "5 phút trước", "3 giờ trước", "2 ngày trước", then the plain date after a week
export const formatRelative = (value, now = Date.now()) => {
  if (!value) return '-'
  const minutes = Math.round((now - new Date(value).getTime()) / 60000)
  if (minutes < 1) return 'Vừa xong'
  if (minutes < 60) return `${minutes} phút trước`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} giờ trước`
  const days = Math.round(hours / 24)
  if (days < 7) return `${days} ngày trước`
  return formatDate(value)
}

// 39000 -> "39.000đ", the way amounts are written at the counter
export const formatCurrency = (value) =>
  value == null ? '-' : `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 0 })}đ`
