import axios from 'axios'
import { API_BASE_URL } from '../config/env.js'
import { tokenStorage } from '../utils/storage.js'

export const UNAUTHORIZED_EVENT = 'dlms:unauthorized'

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
    // An expired or invalid token: tell AuthContext to sign out. A failed login attempt is also a 401
    // but has no stored token, so it must not trigger this.
    if (error.response?.status === 401 && tokenStorage.get()) {
      tokenStorage.clear()
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    // Backend returns ApiError { status, message, fieldErrors }
    return Promise.reject(error.response?.data ?? { message: error.message })
  },
)

export default axiosClient
