import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import keycloak, { identityOf, startKeycloak } from '../auth/keycloak.js'

const AuthContext = createContext(null)

const SIGNED_OUT = { ready: true, authenticated: false, user: null, username: null, role: null }

const readSession = () => ({ ready: true, authenticated: Boolean(keycloak.authenticated), ...identityOf(keycloak) })

/**
 * Sign-in is Keycloak's page: login() sends the user there and brings them back signed in. This context only reflects
 * what the token says (name, card number or staff code, role) and offers login, logout and the account page.
 */
export function AuthProvider({ children }) {
  const [session, setSession] = useState({ ...SIGNED_OUT, ready: false })
  const [failed, setFailed] = useState(null)

  useEffect(() => {
    let current = true
    startKeycloak()
      .then(() => current && setSession(readSession()))
      .catch(() => current && setFailed('Không kết nối được máy chủ đăng nhập (Keycloak). Kiểm tra Keycloak đã chạy chưa rồi tải lại trang.'))
    // The token was renewed or could not be: keep the session in step
    keycloak.onAuthRefreshSuccess = () => setSession(readSession())
    keycloak.onAuthLogout = () => setSession(SIGNED_OUT)
    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => setSession(SIGNED_OUT))
    }
    return () => {
      current = false
    }
  }, [])

  const value = useMemo(
    () => ({
      ...session,
      isAuthenticated: session.authenticated,
      /** Goes to Keycloak's sign-in page; `hint` pre-fills the user name or e-mail. */
      login: (hint) => keycloak.login({ redirectUri: window.location.origin + '/', loginHint: hint, locale: 'vi' }),
      logout: () => keycloak.logout({ redirectUri: window.location.origin + '/login' }),
      /** Keycloak's own page for changing the password. */
      openAccount: () => keycloak.accountManagement(),
    }),
    [session],
  )

  if (failed) return <p className="error" role="alert" style={{ padding: 24 }}>{failed}</p>
  if (!session.ready) return <p className="muted" role="status" style={{ padding: 24 }}>Đang tải...</p>
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
