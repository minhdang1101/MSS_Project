import axiosClient from './axiosClient.js'

const authApi = {
  // Resolves to { token, tokenType, expiresAt, staffCode, fullName }
  login: (staffCode, password) => axiosClient.post('/auth/login', { staffCode, password }),
}

export default authApi
