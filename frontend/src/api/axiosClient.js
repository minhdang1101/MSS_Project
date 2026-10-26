import axios from 'axios'
import { API_BASE_URL } from '../config/env.js'
import { tokenStorage } from '../utils/storage.js'

// All requests go through the API Gateway, never straight to a service.
const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

axiosClient.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

axiosClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) tokenStorage.clear()
    // Backend returns ApiError { status, message, fieldErrors }
    return Promise.reject(error.response?.data ?? { message: error.message })
  },
)

export default axiosClient
