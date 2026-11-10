import axios from 'axios'
import keycloak from '../auth/keycloak.js'
import { API_BASE_URL } from '../config/env.js'

// All requests go through the API Gateway, never straight to a service.
const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

/**
 * Guards against a sign-in loop: if the gateway keeps answering 401 right after a fresh sign-in (for example its
 * Keycloak settings are wrong), going to Keycloak again would bounce back here forever.
 */
function signedInJustNow() {
  try {
    const last = Number(sessionStorage.getItem('dlms_relogin') ?? 0)
    if (Date.now() - last < 15000) return true
    sessionStorage.setItem('dlms_relogin', String(Date.now()))
  } catch {
    // sessionStorage unavailable: no guard, a single redirect is still harmless
  }
  return false
}

axiosClient.interceptors.request.use(async (config) => {
  if (keycloak.authenticated) {
    // Renews the token when it has less than 30 seconds left, so a long session never sends an expired one
    try {
      await keycloak.updateToken(30)
    } catch {
      // The session is over (Keycloak refused to renew): the request goes out unsigned and the 401 below handles it
    }
    if (keycloak.token) config.headers.Authorization = `Bearer ${keycloak.token}`
  }
  return config
})

axiosClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // The gateway rejected our token although we are signed in: the session on Keycloak ended, so sign in again
    if (error.response?.status === 401 && keycloak.authenticated && !signedInJustNow()) {
      void keycloak.login({ redirectUri: window.location.href })
    }
    // Backend returns ApiError { status, message, fieldErrors }
    return Promise.reject(error.response?.data ?? { message: error.message })
  },
)

export default axiosClient
