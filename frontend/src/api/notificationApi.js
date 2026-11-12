import axiosClient from './axiosClient.js'

const notificationApi = {
  // Librarian: every notification; both filters are optional
  getAll: ({ memberId, status } = {}) => axiosClient.get('/notifications', { params: { memberId, status } }),
  // Librarian: put a FAILED notification back in the queue
  retry: (id) => axiosClient.post(`/notifications/${id}/retry`),
  // Reader: the gateway works out who is asking from the token
  getMine: () => axiosClient.get('/notifications/me'),
  markRead: (id) => axiosClient.patch(`/notifications/${id}/read`),
}

export default notificationApi
