import bookApi from '../../api/bookApi.js'
import DataTable from '../../components/DataTable.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import useFetch from '../../hooks/useFetch.js'

const columns = [
  {
    key: 'title',
    label: 'Sách',
    render: (b) => (
      <div>
        <div className="title">{b.title}</div>
        <div className="meta">{b.author}</div>
      </div>
    ),
  },
  { key: 'isbn', label: 'ISBN', render: (b) => <span className="meta">{b.isbn}</span> },
  { key: 'format', label: 'Định dạng' },
  { key: 'availableCopies', label: 'Còn sẵn', num: true, render: (b) => `${b.availableCopies} / ${b.totalCopies}` },
  { key: 'status', label: 'Tình trạng', render: (b) => <StatusBadge status={b.status} /> },
]

export default function BookListPage() {
  const { data, loading, error } = useFetch(bookApi.getAll)

  return (
    <section>
      <PageHead title="Danh mục sách" subtitle="Bản in trên kệ và giấy phép sách điện tử." />
      <PageState loading={loading} error={error}>
        <DataTable columns={columns} rows={data} emptyText="Chưa có đầu sách nào trong danh mục." />
      </PageState>
    </section>
  )
}
