import axiosClient from './axiosClient.js'

const loanApi = {
  // params: { status?: 'BORROWED' | 'OVERDUE' | 'RETURNED', memberId? }
  getAll: (params) => axiosClient.get('/loans', { params }),
  getById: (id) => axiosClient.get(`/loans/${id}`),
  // data: { idempotencyKey, memberId, bookId }. The caller keeps the same key when it retries the same borrow,
  // so a double click or a retry after a timeout never creates a second loan.
  borrow: (data) => axiosClient.post('/loans', data),
  returnBook: (id) => axiosClient.post(`/loans/${id}/return`),
}

export default loanApi
