import fineApi from '../../api/fineApi.js'
import DataTable from '../../components/DataTable.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import useFetch from '../../hooks/useFetch.js'
import { formatCurrency } from '../../utils/format.js'

const columns = [
  { key: 'loanId', label: 'Phiếu mượn' },
  { key: 'memberId', label: 'Thành viên' },
  { key: 'overdueDays', label: 'Số ngày trễ' },
  { key: 'amount', label: 'Số tiền', render: (f) => formatCurrency(f.amount) },
  { key: 'status', label: 'Trạng thái', render: (f) => <StatusBadge status={f.status} /> },
]

export default function FineListPage() {
  const { data, loading, error } = useFetch(fineApi.getAll)

  return (
    <section>
      <h2>Tiền phạt</h2>
      <PageState loading={loading} error={error}>
        <DataTable columns={columns} rows={data} />
      </PageState>
    </section>
  )
}
