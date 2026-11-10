import axiosClient from './axiosClient.js'

const bookApi = {
  getAll: (params) => axiosClient.get('/books', { params }),
  getById: (id) => axiosClient.get(`/books/${id}`),
  create: (data) => axiosClient.post('/books', data),
  update: (id, data) => axiosClient.put(`/books/${id}`, data),
  remove: (id) => axiosClient.delete(`/books/${id}`),
  getCopies: (bookId) => axiosClient.get(`/books/${bookId}/copies`),
  addCopy: (bookId, data) => axiosClient.post(`/books/${bookId}/copies`, data),
  updateCopy: (id, data) => axiosClient.put(`/copies/${id}`, data),
  removeCopy: (id) => axiosClient.delete(`/copies/${id}`),
  findCopyByBarcode: (barcode) => axiosClient.get(`/copies/barcode/${encodeURIComponent(barcode)}`),
}

export default bookApi
