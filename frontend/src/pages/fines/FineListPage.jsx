import { useCallback, useState } from 'react'
import bookApi from '../../api/bookApi.js'
import fineApi from '../../api/fineApi.js'
import memberApi from '../../api/memberApi.js'
import DataTable from '../../components/DataTable.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import Toast from '../../components/Toast.jsx'
import useFetch from '../../hooks/useFetch.js'
import { formatCurrency } from '../../utils/format.js'
import FinePolicyDialog from './FinePolicyDialog.jsx'
import FinePolicyPanel from './FinePolicyPanel.jsx'
import FineSettleDialog from './FineSettleDialog.jsx'

const byId = (result) => new Map(result.status === 'fulfilled' ? result.value.map((item) => [item.id, item]) : [])

// Fines come from fine-service, titles and names from the catalog and member services. When one of those is
// down the list still shows, with ids in place of names.
const fetchFines = async () => {
  const lookups = Promise.allSettled([memberApi.getAll(), bookApi.getAll(), fineApi.getCurrentPolicy()])
  const fines = await fineApi.getAll()
  const [members, books, policy] = await lookups
  const memberById = byId(members)
  const bookById = byId(books)
  return {
    fines: fines.map((f) => ({
      ...f,
      title: bookById.get(f.bookId)?.title ?? `Sách #${f.bookId}`,
      memberName: memberById.get(f.memberId)?.fullName ?? `Thành viên #${f.memberId}`,
    })),
    policy: policy.status === 'fulfilled' ? policy.value : null,
  }
}

export default function FineListPage() {
  const { data, loading, error, reload } = useFetch(fetchFines)
  const [settling, setSettling] = useState(null) // the UNPAID fine being collected
  const [editingPolicy, setEditingPolicy] = useState(false)
  const [notice, setNotice] = useState(null)
  const clearNotice = useCallback(() => setNotice(null), [])

  const handleSettled = (text) => {
    setSettling(null)
    setNotice({ text, kind: 'ok' })
    reload()
  }

  const handlePolicySaved = (text) => {
    setEditingPolicy(false)
    setNotice({ text, kind: 'ok' })
    reload()
  }

  const columns = [
    {
      key: 'title',
      label: 'Thành viên và sách',
      render: (f) => (
        <div>
          <div className="title">{f.title}</div>
          <div className="meta">{f.memberName}</div>
        </div>
      ),
    },
    { key: 'overdueDays', label: 'Trễ', num: true, render: (f) => `${f.overdueDays} ngày` },
    { key: 'amount', label: 'Số tiền', num: true, render: (f) => formatCurrency(f.amount) },
    {
      key: 'status',
      label: 'Trạng thái',
      // UNPAID with no return date: the book is still out and the amount is still growing
      render: (f) => (f.status === 'UNPAID' && f.returnedAt == null
        ? <StatusBadge status="ACCRUING" />
        : f.status === 'UNPAID'
        ? <StatusBadge status={f.status} onClick={() => setSettling(f)} title="Thu tiền phạt" />
        : <StatusBadge status={f.status} />),
    },
  ]

  return (
    <section>
      <PageHead title="Tiền phạt" subtitle="Tính theo ngày cho sách in trả muộn." />
      <PageState loading={loading} error={error}>
        <div className="fines">
          <DataTable columns={columns} rows={data?.fines} emptyText="Chưa có khoản phạt nào." />
          <FinePolicyPanel policy={data?.policy} onEdit={() => setEditingPolicy(true)} />
        </div>
      </PageState>

      {settling && <FineSettleDialog fine={settling} onClose={() => setSettling(null)} onSettled={handleSettled} />}
      {editingPolicy && (
        <FinePolicyDialog policy={data?.policy} onClose={() => setEditingPolicy(false)} onSaved={handlePolicySaved} />
      )}
      <Toast notice={notice} onDone={clearNotice} />
    </section>
  )
}
