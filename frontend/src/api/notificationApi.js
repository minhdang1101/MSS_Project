import axiosClient from './axiosClient.js'

const notificationApi = {
  getAll: (memberId) => axiosClient.get('/notifications', { params: { memberId } }),
}

export default notificationApi
