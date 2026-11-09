import axiosClient from './axiosClient.js'

const fineApi = {
  // Librarian: every fine, optionally only one status (UNPAID | PAID)
  getAll: (status) => axiosClient.get('/fines', { params: { status } }),
  // Member (My Fines): the gateway tells fine-service who the caller is
  getMine: () => axiosClient.get('/fines/me'),
  pay: (id) => axiosClient.patch(`/fines/${id}/paid`),
  // Resolves to { dailyRate, graceDays, maxAmount (null = no ceiling), effectiveFrom }
  getCurrentPolicy: () => axiosClient.get('/fines/policy'),
  // Saves a new version of the rules; same shape as above
  updatePolicy: (policy) => axiosClient.put('/fines/policy', policy),
}

export default fineApi
