import { createContext, useContext, useMemo, useState } from 'react'
import { tokenStorage } from '../utils/storage.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(tokenStorage.get())

  const value = useMemo(
    () => ({
      token,
      isAuthenticated: Boolean(token),
      // TODO: call the auth endpoint once it exists and store the returned JWT
      login: (newToken) => {
        tokenStorage.set(newToken)
        setToken(newToken)
      },
      logout: () => {
        tokenStorage.clear()
        setToken(null)
      },
    }),
    [token],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
