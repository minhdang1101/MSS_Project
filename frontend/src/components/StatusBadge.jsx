const TONE = {
  ok: ['ACTIVE', 'AVAILABLE', 'PAID', 'SENT', 'RETURNED', 'BORROWED'],
  late: ['OVERDUE', 'ACCRUING', 'FAILED', 'SUSPENDED', 'LOCKED', 'LOST', 'DAMAGED'],
  soon: ['PENDING', 'RESERVED', 'ON_LOAN', 'UNPAID', 'DUE_SOON'],
}

const LABEL = {
  ACTIVE: 'Hoạt động', SUSPENDED: 'Tạm khoá', LOCKED: 'Đã khoá',
  AVAILABLE: 'Sẵn có', UNAVAILABLE: 'Không sẵn có', ARCHIVED: 'Lưu trữ',
  ON_LOAN: 'Đang cho mượn', LOST: 'Mất', DAMAGED: 'Hỏng',
  BORROWED: 'Đang mượn', OVERDUE: 'Quá hạn', RETURNED: 'Đã trả',
  ACCRUING: 'Đang tăng mỗi ngày', UNPAID: 'Chưa thu', PAID: 'Đã thu', WAIVED: 'Miễn phạt',
  PENDING: 'Chờ gửi', SENT: 'Đã gửi', FAILED: 'Gửi lỗi', DUE_SOON: 'Sắp đến hạn',
}

const toneOf = (status) => Object.keys(TONE).find((t) => TONE[t].includes(status)) ?? 'none'

/** With onClick the badge becomes a button that looks the same, for a status the librarian can act on. */
export default function StatusBadge({ status, onClick, title }) {
  const key = String(status).toUpperCase()
  const className = `st ${toneOf(key)}`
  const label = LABEL[key] ?? status
  if (onClick) {
    return <button type="button" className={className} onClick={onClick} title={title}>{label}</button>
  }
  return <span className={className}>{label}</span>
}
