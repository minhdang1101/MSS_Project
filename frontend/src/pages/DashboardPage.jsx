import fineApi from '../api/fineApi.js'
import loanApi from '../api/loanApi.js'
import PageHead from '../components/PageHead.jsx'
import PageState from '../components/PageState.jsx'
import useFetch from '../hooks/useFetch.js'
import { formatCurrency } from '../utils/format.js'

const fetchOverview = async () => {
  const [loans, fines] = await Promise.all([loanApi.getAll(), fineApi.getAll()])
  return { loans, fines }
}

export default function DashboardPage() {
  const { data, loading, error } = useFetch(fetchOverview)

  const today = new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long' })
  const open = data?.loans.filter((l) => l.status !== 'RETURNED') ?? []
  const overdue = open.filter((l) => l.status === 'OVERDUE')
  const unpaid = data?.fines.filter((f) => f.status === 'UNPAID') ?? []
  const unpaidTotal = unpaid.reduce((sum, f) => sum + Number(f.amount), 0)

  return (
    <section>
      <PageHead title={today} subtitle="Những việc cần xử lý ở quầy hôm nay." />
      <PageState loading={loading} error={error}>
        <div className="strip">
          <div><b>{open.length}</b><span>lượt đang mượn</span></div>
          <div className="late"><b>{overdue.length}</b><span>lượt quá hạn</span></div>
          <div><b>{unpaid.length}</b><span>khoản phạt chưa thu</span></div>
          <div><b>{formatCurrency(unpaidTotal)}</b><span>tiền phạt chưa thu</span></div>
        </div>
      </PageState>
    </section>
  )
}
