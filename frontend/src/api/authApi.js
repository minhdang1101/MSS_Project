import axiosClient from './axiosClient.js'

const authApi = {
  // Sign-in, forgot password and password change are Keycloak's pages. Only signing up goes through our API,
  // because it also issues the library card number. Resolves to { libraryCardNo, fullName, email, ... }.
  register: (data) => axiosClient.post('/auth/register', data),
}

export default authApi
