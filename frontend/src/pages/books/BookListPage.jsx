import { useCallback, useMemo, useState } from 'react'
import bookApi from '../../api/bookApi.js'
import BookCover from '../../components/BookCover.jsx'
import DataTable from '../../components/DataTable.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import Toast from '../../components/Toast.jsx'
import useFetch from '../../hooks/useFetch.js'
import BookFormDialog from './BookFormDialog.jsx'

const columns = [
  {
    key: 'title',
    label: 'Sách',
    render: (b) => (
      <div className="book-line-cell">
        <BookCover book={b} size="sm" />
        <div>
          <div className="title">{b.title}</div>
          <div className="meta">{b.author}</div>
        </div>
      </div>
    ),
  },
  { key: 'isbn', label: 'ISBN', render: (b) => <span className="meta">{b.isbn}</span> },
  { key: 'format', label: 'Định dạng', render: (b) => (b.format === 'DIGITAL' ? 'Sách điện tử' : 'Sách in') },
  { key: 'shelfLocation', label: 'Vị trí kệ', render: (b) => b.shelfLocation ?? '—' },
  { key: 'availableCopies', label: 'Còn sẵn', num: true, render: (b) => `${b.availableCopies} / ${b.totalCopies}` },
  { key: 'status', label: 'Tình trạng', render: (b) => <StatusBadge status={b.status} /> },
]

const matches = (b, q) => [b.title, b.author, b.isbn].some((v) => v?.toLowerCase().includes(q))

export default function BookListPage() {
  const { data, loading, error, reload } = useFetch(bookApi.getAll)
  const [query, setQuery] = useState('')
  const [adding, setAdding] = useState(false)
  const [notice, setNotice] = useState(null)
  const clearNotice = useCallback(() => setNotice(null), [])

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase()
    return q ? (data ?? []).filter((b) => matches(b, q)) : data
  }, [data, query])

  const handleSaved = (text, kind) => {
    setAdding(false)
    setNotice({ text, kind })
    reload()
  }

  return (
    <section>
      <PageHead title="Danh mục sách" subtitle="Bản in trên kệ và giấy phép sách điện tử.">
        <button type="button" className="btn primary" onClick={() => setAdding(true)}>Thêm sách mới</button>
      </PageHead>

      <div className="toolbar">
        <input type="search" value={query} onChange={(e) => setQuery(e.target.value)}
          placeholder="Tìm theo tên sách, tác giả hoặc ISBN" aria-label="Tìm sách" />
      </div>

      <PageState loading={loading} error={error}>
        <DataTable columns={columns} rows={rows}
          emptyText={query ? `Không có sách khớp với “${query}”.` : 'Chưa có đầu sách nào. Thêm cuốn đầu tiên để bắt đầu.'} />
      </PageState>

      {adding && <BookFormDialog onClose={() => setAdding(false)} onSaved={handleSaved} />}
      <Toast notice={notice} onDone={clearNotice} />
    </section>
  )
}
