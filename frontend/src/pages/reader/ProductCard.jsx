import { Link } from 'react-router-dom'
import BookCover from '../../components/BookCover.jsx'
import { stock } from './bookStock.js'

export default function ProductCard({ book }) {
  const state = stock(book)
  return (
    <Link to={`/me/books/${book.id}`} className="product" aria-label={`${book.title}, ${book.author}`}>
      <div className="product-cover">
        <BookCover book={book} size="lg" />
        {book.format === 'DIGITAL' && <span className="product-tag">Sách điện tử</span>}
      </div>
      <div className="product-body">
        <h3 className="product-title">{book.title}</h3>
        <div className="product-author">{book.author}</div>
        <span className={`st ${state.tone}`}>{state.text}</span>
      </div>
    </Link>
  )
}
