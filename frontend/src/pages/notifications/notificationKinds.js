// How each kind of notification looks, shared by the librarian's list and the reader's inbox.
// tone matches the status colours: late = red, soon = amber.
export const KINDS = {
  OVERDUE: { label: 'Quá hạn', tone: 'late' },
  DUE_SOON: { label: 'Sắp đến hạn', tone: 'soon' },
}

export const kindOf = (type) => KINDS[type] ?? { label: type, tone: 'none' }

export const CHANNELS = { EMAIL: 'Email', SMS: 'Tin nhắn', IN_APP: 'Trong ứng dụng' }

/** When the reader got it: the send time once sent, else when it was scheduled. */
export const shownAt = (n) => n.sentAt ?? n.scheduledAt ?? n.createdAt

/** Fired on window after the librarian retries a notification, so the bell refreshes its count right away. */
export const NOTIFICATIONS_CHANGED = 'dlms:notifications-changed'

/** A reminder scheduled for later has not reached the reader yet, so their inbox leaves it out until then. */
export const visibleToReader = (n, now = Date.now()) => new Date(n.scheduledAt).getTime() <= now
