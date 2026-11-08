// Due dates follow the library's calendar day (Asia/Ho_Chi_Minh), the same rule as loan-service and fine-service.
const LIBRARY_ZONE = 'Asia/Ho_Chi_Minh'
const DAY_MS = 86_400_000

const libraryDay = (value) =>
  Date.parse(new Date(value).toLocaleDateString('en-CA', { timeZone: LIBRARY_ZONE })) // "YYYY-MM-DD" at UTC 0h

/** Whole library days from `from` to `to`; negative when `to` is earlier. */
export const daysBetween = (from, to = new Date()) => Math.round((libraryDay(to) - libraryDay(from)) / DAY_MS)

/** Days the book is (or was) late: 0 when it came back, or is still out, on or before the due day. */
export const lateDays = (loan, until = loan.returnedAt ?? new Date()) => Math.max(0, daysBetween(loan.dueAt, until))

/** "còn 3 ngày", "hạn hôm nay", "trễ 2 ngày" for an open loan; null once returned. */
export const dueHint = (loan) => {
  if (loan.status === 'RETURNED') return null
  const left = -daysBetween(loan.dueAt)
  if (left > 0) return `còn ${left} ngày`
  if (left === 0) return 'hạn hôm nay'
  return `trễ ${-left} ngày`
}

/** The ink on the due slip: 22.11.26 */
export const stampDate = (value) =>
  value
    ? new Date(value).toLocaleDateString('vi-VN', { timeZone: LIBRARY_ZONE, day: '2-digit', month: '2-digit', year: '2-digit' })
        .replaceAll('/', '.')
    : ''
