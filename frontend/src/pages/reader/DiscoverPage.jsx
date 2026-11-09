import { useMemo, useState } from 'react'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import ProductCard from './ProductCard.jsx'
import useBooks, { fold } from './useBooks.js'

const FORMATS = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'PHYSICAL', label: 'Sách in' },
  { key: 'DIGITAL', label: 'Sách điện tử' },
]
const SORTS = {
  newest: { label: 'Mới nhất', fn: (a, b) => b.id - a.id },
  title: { label: 'Tên A–Z', fn: (a, b) => a.title.localeCompare(b.title, 'vi') },
  stock: { label: 'Còn nhiều nhất', fn: (a, b) => b.availableCopies - a.availableCopies },
}

/** The catalogue as a shop window: cards with covers, a search box and a few filters. Click a card for the details. */
export default function DiscoverPage() {
  const { data, loading, error } = useBooks()
  const [query, setQuery] = useState('')
  const [format, setFormat] = useState('ALL')
  const [onlyAvailable, setOnlyAvailable] = useState(false)
  const [sort, setSort] = useState('newest')

  const shown = useMemo(() => {
    const q = fold(query.trim())
    return (data ?? [])
      .filter((b) => b.status !== 'ARCHIVED')
      .filter((b) => format === 'ALL' || b.format === format)
      .filter((b) => !onlyAvailable || b.availableCopies > 0)
      .filter((b) => !q || fold(b.title).includes(q) || fold(b.author).includes(q) || fold(b.isbn).includes(q)
        || fold(b.publisher).includes(q))
      .sort(SORTS[sort].fn)
  }, [data, query, format, onlyAvailable, sort])

  const filtered = query.trim() || format !== 'ALL' || onlyAvailable
  const reset = () => {
    setQuery('')
    setFormat('ALL')
    setOnlyAvailable(false)
  }

  return (
    <section>
      <PageHead title="Khám phá sách" subtitle="Chọn một cuốn để xem đầy đủ thông tin. Mượn sách tại quầy thủ thư." />

      <div className="shop-bar">
        <input type="search" value={query} onChange={(e) => setQuery(e.target.value)}
          placeholder="Tìm theo tên sách, tác giả, nhà xuất bản hoặc ISBN" aria-label="Tìm sách" />
        <label className="sort">
          <span className="meta">Sắp xếp</span>
          <select value={sort} onChange={(e) => setSort(e.target.value)}>
            {Object.entries(SORTS).map(([key, s]) => <option key={key} value={key}>{s.label}</option>)}
          </select>
        </label>
      </div>

      <div className="chips" role="group" aria-label="Lọc sách">
        {FORMATS.map((f) => (
          <button key={f.key} type="button" className="chip" aria-pressed={format === f.key} onClick={() => setFormat(f.key)}>
            {f.label}
          </button>
        ))}
        <button type="button" className="chip" aria-pressed={onlyAvailable} onClick={() => setOnlyAvailable((v) => !v)}>
          Còn sách để mượn
        </button>
        {filtered && <button type="button" className="link-btn" onClick={reset}>Đặt lại</button>}
      </div>

      <PageState loading={loading} error={error} skeleton="grid">
        <p className="meta result-count">{shown.length} cuốn sách</p>
        {shown.length === 0 ? (
          <p className="panel empty">
            {filtered ? 'Không tìm thấy sách phù hợp. Thử bỏ bớt bộ lọc hoặc gõ ít từ hơn.' : 'Thư viện chưa có sách nào.'}
          </p>
        ) : (
          <div className="product-grid">
            {shown.map((book, i) => (
              <div className="product-slot" key={book.id} style={{ animationDelay: `${Math.min(i, 12) * 35}ms` }}>
                <ProductCard book={book} />
              </div>
            ))}
          </div>
        )}
      </PageState>
    </section>
  )
}
