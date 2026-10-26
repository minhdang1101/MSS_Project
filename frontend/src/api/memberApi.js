import axiosClient from './axiosClient.js'

const memberApi = {
  getAll: () => axiosClient.get('/members'),
  getById: (id) => axiosClient.get(`/members/${id}`),
  create: (data) => axiosClient.post('/members', data),
}

export default memberApi
