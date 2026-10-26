import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { UNAUTHORIZED_EVENT } from '../api/axiosClient.js'
import { tokenStorage } from '../utils/storage.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(tokenStorage.get())
  const [user, setUser] = useState(tokenStorage.getUser())

  // The gateway rejected our token (expired / invalid): drop the session so RequireAuth redirects to /login.
  useEffect(() => {
    const onUnauthorized = () => {
      setToken(null)
      setUser(null)
    }
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [])

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token),
      login: (newToken, name) => {
        tokenStorage.set(newToken, name)
        setToken(newToken)
        setUser(name ?? null)
      },
      logout: () => {
        tokenStorage.clear()
        setToken(null)
        setUser(null)
      },
    }),
    [token, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
