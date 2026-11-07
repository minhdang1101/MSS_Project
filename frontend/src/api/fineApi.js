import axiosClient from './axiosClient.js'

const fineApi = {
  getAll: (memberId) => axiosClient.get('/fines', { params: { memberId } }),
  pay: (id) => axiosClient.post(`/fines/${id}/pay`),
  waive: (id) => axiosClient.post(`/fines/${id}/waive`),
  // Resolves to { dailyRate, graceDays, maxAmount (null = no ceiling), lostBookFee, effectiveFrom }
  getCurrentPolicy: () => axiosClient.get('/fine-policies/current'),
}

export default fineApi
