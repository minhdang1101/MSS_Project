import notificationApi from '../../api/notificationApi.js'
import DataTable from '../../components/DataTable.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import useFetch from '../../hooks/useFetch.js'
import { formatDate } from '../../utils/format.js'

const columns = [
  { key: 'type', label: 'Loại' },
  { key: 'recipient', label: 'Người nhận' },
  { key: 'subject', label: 'Tiêu đề' },
  { key: 'scheduledAt', label: 'Lịch gửi', render: (n) => formatDate(n.scheduledAt) },
  { key: 'status', label: 'Trạng thái', render: (n) => <StatusBadge status={n.status} /> },
]

export default function NotificationListPage() {
  const { data, loading, error } = useFetch(notificationApi.getAll)

  return (
    <section>
      <h2>Thông báo</h2>
      <PageState loading={loading} error={error}>
        <DataTable columns={columns} rows={data} />
      </PageState>
    </section>
  )
}
