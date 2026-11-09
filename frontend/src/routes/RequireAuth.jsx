import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { homeFor } from '../utils/roles.js'

/** Keeps signed-out visitors on /login, and sends a user to their own area when they open the other one. */
export default function RequireAuth({ role }) {
  const { isAuthenticated, role: userRole } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (role && userRole !== role) return <Navigate to={homeFor(userRole)} replace />
  return <Outlet />
}
