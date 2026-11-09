import { Link, useParams } from 'react-router-dom'
import { useCallback } from 'react'
import bookApi from '../../api/bookApi.js'
import BookCover from '../../components/BookCover.jsx'
import PageState from '../../components/PageState.jsx'
import useFetch from '../../hooks/useFetch.js'
import { stock } from './bookStock.js'

/** Everything the library knows about one book, laid out like a product page. */
export default function BookDetailPage() {
  const { id } = useParams()
  const load = useCallback(() => bookApi.getById(id), [id])
  const { data: book, loading, error } = useFetch(load)

  return (
    <section>
      <Link to="/me" className="back-link">← Quay lại danh sách</Link>
      <PageState loading={loading} error={error?.includes?.('not found') ? 'Không tìm thấy cuốn sách này.' : error}>
        {book && <Detail book={book} />}
      </PageState>
    </section>
  )
}

function Detail({ book }) {
  const state = stock(book)
  const digital = book.format === 'DIGITAL'
  const rows = [
    ['ISBN', book.isbn],
    ['Nhà xuất bản', book.publisher ?? '—'],
    ['Năm xuất bản', book.publicationYear ?? '—'],
    ['Định dạng', digital ? 'Sách điện tử' : 'Sách in'],
    ...(digital ? [] : [['Vị trí kệ', book.shelfLocation ?? '—']]),
    [digital ? 'Giấy phép còn lại' : 'Số bản còn trên kệ', `${book.availableCopies} / ${book.totalCopies}`],
  ]

  return (
    <article className="detail">
      <div className="detail-cover"><BookCover book={book} size="xl" /></div>
      <div className="detail-main">
        <h1>{book.title}</h1>
        <p className="detail-author">{book.author}</p>
        <p><span className={`st ${state.tone}`}>{state.text}</span></p>

        <dl className="detail-facts">
          {rows.map(([label, value]) => (
            <div key={label}><dt>{label}</dt><dd>{value}</dd></div>
          ))}
        </dl>

        <div className="detail-note" role="note">
          {book.availableCopies > 0
            ? 'Cuốn sách đang có sẵn. Mang thẻ thư viện đến quầy thủ thư để mượn.'
            : 'Hiện chưa còn bản nào. Hãy quay lại sau khi có người trả sách.'}
        </div>
      </div>
    </article>
  )
}
