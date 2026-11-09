import { useState } from 'react'
import { API_BASE_URL } from '../config/env.js'

// Placeholder tiles: soft paper colours, picked from the title so a book always gets the same one.
const TILES = ['#E9D8A6', '#D8E2D3', '#D5DEE0', '#E8DCCB', '#E3D5D9', '#D6D9E8']

const tileFor = (title) => {
  let hash = 0
  for (const ch of title ?? '') hash = (hash * 31 + ch.codePointAt(0)) % 997
  return TILES[hash % TILES.length]
}

/** coverUrl comes from the API as a path ("/api/books/3/cover?v=..."): resolve it against the gateway. */
const absolute = (coverUrl) => new URL(coverUrl, new URL(API_BASE_URL, window.location.href)).href

/** The book's cover, or a tile with its title and author when it has none (or the picture fails to load). */
export default function BookCover({ book, size = 'sm' }) {
  const [failedUrl, setFailedUrl] = useState(null)
  const showImage = book.coverUrl && failedUrl !== book.coverUrl

  return (
    <div className={`cover cover-${size}`} style={showImage ? undefined : { background: tileFor(book.title) }}>
      {showImage ? (
        <img src={absolute(book.coverUrl)} alt={`Bìa sách ${book.title}`} loading="lazy"
          onError={() => setFailedUrl(book.coverUrl)} />
      ) : (
        <div className="cover-tile" aria-label={`Bìa sách ${book.title}`}>
          <span className="cover-author">{book.author}</span>
          <span className="cover-title">{book.title}</span>
        </div>
      )}
    </div>
  )
}
