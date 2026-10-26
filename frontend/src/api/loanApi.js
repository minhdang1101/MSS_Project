import axiosClient from './axiosClient.js'

const loanApi = {
  getAll: () => axiosClient.get('/loans'),
  getById: (id) => axiosClient.get(`/loans/${id}`),
  // idempotencyKey lets the backend ignore accidental double submits
  borrow: (data) => axiosClient.post('/loans', { idempotencyKey: crypto.randomUUID(), ...data }),
  returnBook: (id) => axiosClient.post(`/loans/${id}/return`),
}

export default loanApi
