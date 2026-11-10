const TILES = ['#E9D8A6', '#D8E2D3', '#D5DEE0', '#E8DCCB', '#E3D5D9', '#D6D9E8']

const tileFor = (title) => {
  let hash = 0
  for (const ch of title ?? '') hash = (hash * 31 + ch.codePointAt(0)) % 997
  return TILES[hash % TILES.length]
}

export default function BookCover({ book, size = 'sm' }) {
  return (
    <div className={`cover cover-${size}`} style={{ background: tileFor(book.title) }}>
      <div className="cover-tile" aria-label={`Bìa sách ${book.title}`}>
        <span className="cover-author">{book.author}</span>
        <span className="cover-title">{book.title}</span>
      </div>
    </div>
  )
}
