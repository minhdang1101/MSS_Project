const TONE = {
  ok: ['ACTIVE', 'AVAILABLE', 'PAID', 'SENT', 'RETURNED', 'BORROWED'],
  late: ['OVERDUE', 'UNPAID', 'FAILED', 'SUSPENDED', 'LOCKED', 'LOST', 'DAMAGED'],
  soon: ['PENDING', 'RESERVED', 'ON_LOAN'],
}

const LABEL = {
  ACTIVE: 'Hoạt động', SUSPENDED: 'Tạm khoá', LOCKED: 'Đã khoá',
  AVAILABLE: 'Sẵn có', UNAVAILABLE: 'Không sẵn có', ARCHIVED: 'Lưu trữ',
  BORROWED: 'Đang mượn', OVERDUE: 'Quá hạn', RETURNED: 'Đã trả',
  UNPAID: 'Chưa thu', PAID: 'Đã thu', WAIVED: 'Miễn phạt',
  PENDING: 'Chờ gửi', SENT: 'Đã gửi', FAILED: 'Gửi lỗi',
}

const toneOf = (status) => Object.keys(TONE).find((t) => TONE[t].includes(status)) ?? 'none'

export default function StatusBadge({ status }) {
  const key = String(status).toUpperCase()
  return <span className={`st ${toneOf(key)}`}>{LABEL[key] ?? status}</span>
}
