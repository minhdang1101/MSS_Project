import { useCallback, useMemo, useRef, useState } from 'react'
import memberApi from '../../api/memberApi.js'
import DataTable from '../../components/DataTable.jsx'
import PageHead from '../../components/PageHead.jsx'
import PageState from '../../components/PageState.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import Toast from '../../components/Toast.jsx'
import useFetch from '../../hooks/useFetch.js'
import { formatDate } from '../../utils/format.js'
import MemberFormDialog from './MemberFormDialog.jsx'
import MemberImportDialog from './MemberImportDialog.jsx'

const matches = (m, query) =>
  [m.fullName, m.libraryCardNo, m.email, m.phone].some((v) => v?.toLowerCase().includes(query))

export default function MemberListPage() {
  const { data, loading, error, reload } = useFetch(memberApi.getAll)
  const [query, setQuery] = useState('')
  const [dialog, setDialog] = useState(null) // { type: 'form', member? } | { type: 'import' }
  const [notice, setNotice] = useState(null) // { text, kind: 'ok' | 'error' }
  const importedCount = useRef(0)

  const showOk = (text) => setNotice({ text, kind: 'ok' })
  const showError = (text) => setNotice({ text, kind: 'error' })
  const clearNotice = useCallback(() => setNotice(null), [])

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase()
    return q ? (data ?? []).filter((m) => matches(m, q)) : data
  }, [data, query])

  const closeDialog = () => setDialog(null)

  const handleSaved = (message) => {
    closeDialog()
    showOk(message)
    reload()
  }

  // The import result stays on screen inside the dialog; the toast appears once the librarian closes it.
  const handleImportClosed = () => {
    closeDialog()
    if (importedCount.current > 0) {
      showOk(`Đã nhập thành công ${importedCount.current} thành viên.`)
      importedCount.current = 0
      reload()
    }
  }

  const toggleStatus = async (member) => {
    const next = member.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
    try {
      await memberApi.setStatus(member.id, next)
      showOk(next === 'ACTIVE' ? `Đã mở lại thẻ ${member.libraryCardNo}.` : `Đã tạm khoá thẻ ${member.libraryCardNo}.`)
      reload()
    } catch (err) {
      showError(err.message ?? 'Không đổi được trạng thái. Thử lại.')
    }
  }

  const columns = [
    {
      key: 'fullName',
      label: 'Thành viên',
      render: (m) => (
        <div>
          <div className="title">{m.fullName}</div>
          <div className="meta">{m.libraryCardNo}</div>
        </div>
      ),
    },
    { key: 'email', label: 'Email' },
    { key: 'phone', label: 'Điện thoại', render: (m) => m.phone ?? '-' },
    { key: 'joinedAt', label: 'Ngày tham gia', render: (m) => formatDate(m.joinedAt) },
    { key: 'status', label: 'Trạng thái', render: (m) => <StatusBadge status={m.status} /> },
    {
      key: 'actions',
      label: '',
      render: (m) => (
        <div className="row-actions">
          <button type="button" className="btn" onClick={() => setDialog({ type: 'form', member: m })}>Sửa</button>
          <button type="button" className="btn" onClick={() => toggleStatus(m)}>
            {m.status === 'ACTIVE' ? 'Tạm khoá' : 'Mở lại'}
          </button>
        </div>
      ),
    },
  ]

  return (
    <section>
      <PageHead title="Thành viên" subtitle="Hồ sơ bạn đọc đang có thẻ thư viện.">
        <div className="row-actions">
          <button type="button" className="btn" onClick={() => setDialog({ type: 'import' })}>Nhập từ Excel</button>
          <button type="button" className="btn primary" onClick={() => setDialog({ type: 'form' })}>Cấp thẻ mới</button>
        </div>
      </PageHead>

      <div className="toolbar">
        <input type="search" value={query} onChange={(e) => setQuery(e.target.value)}
          placeholder="Tìm theo tên, số thẻ, email hoặc điện thoại" aria-label="Tìm thành viên" />
      </div>

      <PageState loading={loading} error={error}>
        <DataTable columns={columns} rows={rows}
          emptyText={query ? `Không có thành viên khớp với “${query}”.` : 'Chưa có thành viên nào. Cấp thẻ đầu tiên để bắt đầu.'} />
      </PageState>

      {dialog?.type === 'form' && (
        <MemberFormDialog member={dialog.member} onClose={closeDialog} onSaved={handleSaved} />
      )}
      {dialog?.type === 'import' && (
        <MemberImportDialog onClose={handleImportClosed} onImported={(count) => { importedCount.current += count }} />
      )}
      <Toast notice={notice} onDone={clearNotice} />
    </section>
  )
}
