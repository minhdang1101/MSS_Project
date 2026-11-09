/** What a shopper wants to know first: is there a copy, said the way the library counts it. */
export function stock(book) {
  const digital = book.format === 'DIGITAL'
  if (book.status === 'ARCHIVED') return { text: 'Ngừng phục vụ', tone: 'none' }
  if (book.availableCopies > 0) {
    return { text: digital ? `Còn ${book.availableCopies} giấy phép` : `Còn ${book.availableCopies} bản`, tone: 'ok' }
  }
  return { text: digital ? 'Hết giấy phép' : 'Đang được mượn hết', tone: 'late' }
}
