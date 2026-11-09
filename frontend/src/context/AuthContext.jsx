import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { UNAUTHORIZED_EVENT } from '../api/axiosClient.js'
import { tokenStorage } from '../utils/storage.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSession] = useState({
    token: tokenStorage.get(),
    user: tokenStorage.getUser(),
    role: tokenStorage.getRole(),
    username: tokenStorage.getUsername(),
  })

  // The gateway rejected our token (expired / invalid): drop the session so RequireAuth redirects to /login.
  useEffect(() => {
    const onUnauthorized = () => setSession({ token: null, user: null, role: null, username: null })
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [])

  const value = useMemo(
    () => ({
      ...session,
      isAuthenticated: Boolean(session.token),
      login: (token, user, role, username) => {
        tokenStorage.set(token, user, role, username)
        setSession({ token, user: user ?? null, role: role ?? null, username: username ?? null })
      },
      logout: () => {
        tokenStorage.clear()
        setSession({ token: null, user: null, role: null, username: null })
      },
    }),
    [session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
