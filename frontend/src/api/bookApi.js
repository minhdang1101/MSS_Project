import axiosClient from './axiosClient.js'

const bookApi = {
  getAll: () => axiosClient.get('/books'),
  getById: (id) => axiosClient.get(`/books/${id}`),
  create: (data) => axiosClient.post('/books', data),
  // Resolves to the book with its new coverUrl. A picture is slower than JSON, so a longer timeout.
  uploadCover: (id, file) => {
    const form = new FormData()
    form.append('file', file)
    return axiosClient.put(`/books/${id}/cover`, form, { timeout: 60000 })
  },
}

export default bookApi
