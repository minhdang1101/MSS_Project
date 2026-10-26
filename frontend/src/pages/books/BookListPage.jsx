import bookApi from '../../api/bookApi.js'
import DataTable from '../../components/DataTable.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import useFetch from '../../hooks/useFetch.js'

const columns = [
  { key: 'isbn', label: 'ISBN' },
  { key: 'title', label: 'Tên sách' },
  { key: 'author', label: 'Tác giả' },
  { key: 'format', label: 'Định dạng' },
  { key: 'availableCopies', label: 'Còn lại', render: (b) => `${b.availableCopies}/${b.totalCopies}` },
  { key: 'status', label: 'Trạng thái', render: (b) => <StatusBadge status={b.status} /> },
]

export default function BookListPage() {
  const { data, loading, error } = useFetch(bookApi.getAll)

  return (
    <section>
      <h2>Danh mục sách</h2>
      <PageState loading={loading} error={error}>
        <DataTable columns={columns} rows={data} />
      </PageState>
    </section>
  )
}
