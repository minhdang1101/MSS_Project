import loanApi from '../../api/loanApi.js'
import DataTable from '../../components/DataTable.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import useFetch from '../../hooks/useFetch.js'
import { formatDate } from '../../utils/format.js'

const columns = [
  { key: 'id', label: 'Mã phiếu' },
  { key: 'memberId', label: 'Thành viên' },
  { key: 'bookId', label: 'Sách' },
  { key: 'borrowedAt', label: 'Ngày mượn', render: (l) => formatDate(l.borrowedAt) },
  { key: 'dueAt', label: 'Hạn trả', render: (l) => formatDate(l.dueAt) },
  { key: 'returnedAt', label: 'Ngày trả', render: (l) => formatDate(l.returnedAt) },
  { key: 'status', label: 'Trạng thái', render: (l) => <StatusBadge status={l.status} /> },
]

export default function LoanListPage() {
  const { data, loading, error } = useFetch(loanApi.getAll)

  return (
    <section>
      <h2>Mượn / Trả sách</h2>
      {/* TODO: form mượn sách + nút trả sách */}
      <PageState loading={loading} error={error}>
        <DataTable columns={columns} rows={data} />
      </PageState>
    </section>
  )
}
