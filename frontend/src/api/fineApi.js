import axiosClient from './axiosClient.js'

const fineApi = {
  getAll: (memberId) => axiosClient.get('/fines', { params: { memberId } }),
  pay: (id) => axiosClient.post(`/fines/${id}/pay`),
}

export default fineApi
