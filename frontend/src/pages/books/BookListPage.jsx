import { useCallback, useEffect, useState } from 'react'
import bookApi from '../../api/bookApi.js'
import BookCover from '../../components/BookCover.jsx'
import DataTable from '../../components/DataTable.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import Toast from '../../components/Toast.jsx'
import useFetch from '../../hooks/useFetch.js'
import BookFormDialog from './BookFormDialog.jsx'
import CopyDialog from './CopyDialog.jsx'

const FORMATS = [
  { key: '', label: 'Tất cả' },
  { key: 'PHYSICAL', label: 'Sách in' },
  { key: 'DIGITAL', label: 'Sách điện tử' },
]

export default function BookListPage() {
  const [query, setQuery] = useState('')
  const [search, setSearch] = useState('')
  const [format, setFormat] = useState('')
  const [dialog, setDialog] = useState(null)
  const [notice, setNotice] = useState(null)
  const clearNotice = useCallback(() => setNotice(null), [])

  useEffect(() => {
    const timer = setTimeout(() => setSearch(query.trim()), 300)
    return () => clearTimeout(timer)
  }, [query])

  const load = useCallback(
    () => bookApi.getAll({ q: search || undefined, format: format || undefined }),
    [search, format],
  )
  const { data, loading, error, reload } = useFetch(load)

  const handleSaved = (text, kind) => {
    setDialog(null)
    setNotice({ text, kind })
    reload()
  }

  const archive = async (book) => {
    if (!window.confirm(`Ngừng phục vụ sách "${book.title}"? Sách sẽ không còn hiện trong danh mục.`)) return
    try {
      await bookApi.remove(book.id)
      setNotice({ text: `Đã ngừng phục vụ sách "${book.title}".`, kind: 'ok' })
      reload()
    } catch (err) {
      setNotice({ text: err.message ?? 'Không thực hiện được. Thử lại.', kind: 'error' })
    }
  }

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
    { key: 'availableCopies', label: 'Còn sẵn', num: true, render: (b) => `${b.availableCopies} / ${b.totalCopies}` },
    { key: 'status', label: 'Tình trạng', render: (b) => <StatusBadge status={b.status} /> },
    {
      key: 'actions',
      label: '',
      render: (b) => (
        <div className="row-actions">
          <button type="button" className="btn small" onClick={() => setDialog({ type: 'form', book: b })}>Sửa</button>
          {b.format === 'PHYSICAL' && (
            <button type="button" className="btn small" onClick={() => setDialog({ type: 'copies', book: b })}>Bản sách</button>
          )}
          <button type="button" className="btn small" onClick={() => archive(b)}>Ngừng phục vụ</button>
        </div>
      ),
    },
  ]

  const filtered = search || format

  return (
    <section>
      <PageHead title="Danh mục sách" subtitle="Bản in trên kệ và giấy phép sách điện tử.">
        <button type="button" className="btn primary" onClick={() => setDialog({ type: 'form' })}>Thêm sách mới</button>
      </PageHead>

      <div className="toolbar">
        <input type="search" value={query} onChange={(e) => setQuery(e.target.value)}
          placeholder="Tìm theo tên sách, tác giả hoặc ISBN" aria-label="Tìm sách" />
        <div className="seg" role="group" aria-label="Lọc định dạng" style={{ marginBottom: 0 }}>
          {FORMATS.map((f) => (
            <button key={f.key} type="button" aria-pressed={format === f.key} onClick={() => setFormat(f.key)}>{f.label}</button>
          ))}
        </div>
      </div>

      <PageState loading={loading} error={error}>
        <DataTable columns={columns} rows={data}
          emptyText={filtered ? 'Không có sách phù hợp.' : 'Chưa có đầu sách nào. Thêm cuốn đầu tiên để bắt đầu.'} />
      </PageState>

      {dialog?.type === 'form' && <BookFormDialog book={dialog.book} onClose={() => setDialog(null)} onSaved={handleSaved} />}
      {dialog?.type === 'copies' && <CopyDialog book={dialog.book} onClose={() => setDialog(null)} onChanged={reload} />}
      <Toast notice={notice} onDone={clearNotice} />
    </section>
  )
}
