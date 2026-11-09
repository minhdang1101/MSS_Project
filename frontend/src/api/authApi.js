import axiosClient from './axiosClient.js'

const authApi = {
  // Resolves to { token, tokenType, expiresAt, role, username, fullName }. `username` is a staff code,
  // a library card number or, for readers, the e-mail they registered with.
  login: (username, password) => axiosClient.post('/auth/login', { username, password }),
  register: (data) => axiosClient.post('/auth/register', data),
  // Always succeeds, whether or not the e-mail has an account.
  forgotPassword: (email) => axiosClient.post('/auth/forgot-password', { email }),
  // Resolves when the code is right (409 when it is wrong or expired); the code stays usable for the next step.
  verifyResetCode: (email, code) => axiosClient.post('/auth/verify-reset-code', { email, code }),
  resetPassword: (email, code, newPassword) => axiosClient.post('/auth/reset-password', { email, code, newPassword }),
}

export default authApi
