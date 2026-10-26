import fineApi from '../../api/fineApi.js'
import DataTable from '../../components/DataTable.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import useFetch from '../../hooks/useFetch.js'
import { formatCurrency } from '../../utils/format.js'

const columns = [
  { key: 'loanId', label: 'Phiếu mượn' },
  { key: 'memberId', label: 'Thành viên' },
  { key: 'overdueDays', label: 'Trễ', num: true, render: (f) => `${f.overdueDays} ngày` },
  { key: 'amount', label: 'Số tiền', num: true, render: (f) => formatCurrency(f.amount) },
  { key: 'status', label: 'Trạng thái', render: (f) => <StatusBadge status={f.status} /> },
]

export default function FineListPage() {
  const { data, loading, error } = useFetch(fineApi.getAll)

  return (
    <section>
      <PageHead title="Tiền phạt" subtitle="Tính theo ngày cho sách in trả muộn." />
      <PageState loading={loading} error={error}>
        <DataTable columns={columns} rows={data} emptyText="Chưa có khoản phạt nào." />
      </PageState>
    </section>
  )
}
