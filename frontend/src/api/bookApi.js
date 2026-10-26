import axiosClient from './axiosClient.js'

const bookApi = {
  getAll: () => axiosClient.get('/books'),
  getById: (id) => axiosClient.get(`/books/${id}`),
  create: (data) => axiosClient.post('/books', data),
}

export default bookApi
