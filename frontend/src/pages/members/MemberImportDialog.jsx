import { useState } from 'react'
import memberApi from '../../api/memberApi.js'
import Modal from '../../components/Modal.jsx'

export default function MemberImportDialog({ onClose, onImported }) {
  const [file, setFile] = useState(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState(null)
  const [result, setResult] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!file) {
      setError('Chọn một file Excel .xlsx để nhập.')
      return
    }
    setUploading(true)
    setError(null)
    try {
      const data = await memberApi.importExcel(file)
      setResult(data)
      if (data.imported > 0) onImported(data.imported)
    } catch (err) {
      setError(err.message ?? 'Không nhập được file. Thử lại.')
    } finally {
      setUploading(false)
    }
  }

  const handleDownloadTemplate = async () => {
    setError(null)
    try {
      const blob = await memberApi.downloadImportTemplate()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'mau-nhap-thanh-vien.xlsx'
      link.click()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch {
      setError('Không tải được file mẫu. Thử lại.')
    }
  }

  if (result) {
    return (
      <Modal title="Kết quả nhập file" onClose={onClose}>
        <p>
          Đã nhập <b>{result.imported}</b> thành viên
          {result.errors.length > 0 && <>, bỏ qua <b>{result.errors.length}</b> dòng lỗi</>}.
        </p>
        {result.errors.length > 0 && (
          <div className="import-errors tablewrap">
            <table>
              <thead><tr><th className="num">Dòng</th><th>Lý do</th></tr></thead>
              <tbody>
                {result.errors.map((r) => (
                  <tr key={r.row}><td className="num">{r.row}</td><td>{r.message}</td></tr>
                ))}
              </tbody>
            </table>
            <p className="note">Sửa các dòng này trong file rồi nhập lại. Dòng đã nhập sẽ không bị tạo lặp.</p>
          </div>
        )}
        <div className="modal-actions">
          <button type="button" className="btn primary" onClick={onClose}>Đóng</button>
        </div>
      </Modal>
    )
  }

  return (
    <Modal title="Nhập thành viên từ Excel" onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <p className="template-link">
          <button type="button" className="btn" onClick={handleDownloadTemplate}>Tải file mẫu</button>
        </p>
        <p className="sub">Điền file mẫu rồi tải lên. Tối đa 1000 dòng, cả ba cột đều bắt buộc:</p>
        <ol className="cols">
          <li>Họ tên (tối đa 100)</li><li>Email (tối đa 100)</li><li>Điện thoại (tối đa 20)</li>
        </ol>
        <p className="note">Email và số điện thoại không được trùng nhau. Số thẻ được cấp tự động.</p>
        <div className="field">
          <label htmlFor="m-file">File Excel</label>
          <input id="m-file" type="file"
            accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            onChange={(e) => setFile(e.target.files[0] ?? null)} />
        </div>
        {error && <p className="err" role="alert">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn" onClick={onClose}>Huỷ</button>
          <button type="submit" className="btn primary" disabled={uploading}>
            {uploading ? 'Đang nhập...' : 'Nhập file'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
