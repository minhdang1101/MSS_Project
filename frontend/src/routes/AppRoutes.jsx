import { Route, Routes } from 'react-router-dom'
import MainLayout from '../layouts/MainLayout.jsx'
import DashboardPage from '../pages/DashboardPage.jsx'
import NotFoundPage from '../pages/NotFoundPage.jsx'
import LoginPage from '../pages/auth/LoginPage.jsx'
import BookListPage from '../pages/books/BookListPage.jsx'
import FineListPage from '../pages/fines/FineListPage.jsx'
import LoanListPage from '../pages/loans/LoanListPage.jsx'
import MemberListPage from '../pages/members/MemberListPage.jsx'
import NotificationListPage from '../pages/notifications/NotificationListPage.jsx'
import RequireAuth from './RequireAuth.jsx'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<MainLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="books" element={<BookListPage />} />
          <Route path="members" element={<MemberListPage />} />
          <Route path="loans" element={<LoanListPage />} />
          <Route path="fines" element={<FineListPage />} />
          <Route path="notifications" element={<NotificationListPage />} />
        </Route>
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
