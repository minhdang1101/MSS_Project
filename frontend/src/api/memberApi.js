import axiosClient from './axiosClient.js'

const memberApi = {
  getAll: () => axiosClient.get('/members'),
  getById: (id) => axiosClient.get(`/members/${id}`),
  create: (data) => axiosClient.post('/members', data),
  update: (id, data) => axiosClient.put(`/members/${id}`, data),
  setStatus: (id, status) => axiosClient.patch(`/members/${id}/status`, { status }),
  // The blank Excel file to fill in; resolves to a Blob (needs the auth header, so it cannot be a plain link)
  downloadImportTemplate: () => axiosClient.get('/members/import/template', { responseType: 'blob' }),
  // Resolves to { imported, errors: [{ row, message }] }
  importExcel: (file) => {
    const form = new FormData()
    form.append('file', file)
    return axiosClient.post('/members/import', form, { timeout: 60000 }) // slower than a normal call
  },
}

export default memberApi
