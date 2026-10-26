import memberApi from '../../api/memberApi.js'
import DataTable from '../../components/DataTable.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import useFetch from '../../hooks/useFetch.js'
import { formatDate } from '../../utils/format.js'

const columns = [
  { key: 'libraryCardNo', label: 'Số thẻ' },
  { key: 'fullName', label: 'Họ tên' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Điện thoại' },
  { key: 'joinedAt', label: 'Ngày tham gia', render: (m) => formatDate(m.joinedAt) },
  { key: 'status', label: 'Trạng thái', render: (m) => <StatusBadge status={m.status} /> },
]

export default function MemberListPage() {
  const { data, loading, error } = useFetch(memberApi.getAll)

  return (
    <section>
      <h2>Thành viên</h2>
      <PageState loading={loading} error={error}>
        <DataTable columns={columns} rows={data} />
      </PageState>
    </section>
  )
}
