import { Navigate, Route, Routes } from 'react-router-dom'
import MainLayout from '../layouts/MainLayout.jsx'
import ReaderLayout from '../layouts/ReaderLayout.jsx'
import DashboardPage from '../pages/DashboardPage.jsx'
import NotFoundPage from '../pages/NotFoundPage.jsx'
import LoginPage from '../pages/auth/LoginPage.jsx'
import RegisterPage from '../pages/auth/RegisterPage.jsx'
import BookListPage from '../pages/books/BookListPage.jsx'
import FineListPage from '../pages/fines/FineListPage.jsx'
import LoanListPage from '../pages/loans/LoanListPage.jsx'
import MemberListPage from '../pages/members/MemberListPage.jsx'
import NotificationListPage from '../pages/notifications/NotificationListPage.jsx'
import BookDetailPage from '../pages/reader/BookDetailPage.jsx'
import DiscoverPage from '../pages/reader/DiscoverPage.jsx'
import MyFinesPage from '../pages/reader/MyFinesPage.jsx'
import MyNotificationsPage from '../pages/reader/MyNotificationsPage.jsx'
import MyLoansPage from '../pages/reader/MyLoansPage.jsx'
import { LIBRARIAN, MEMBER } from '../utils/roles.js'
import RequireAuth from './RequireAuth.jsx'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="login" element={<LoginPage />} />
      <Route path="register" element={<RegisterPage />} />

      {/* Librarians: the management screens */}
      <Route element={<RequireAuth role={LIBRARIAN} />}>
        <Route element={<MainLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="books" element={<BookListPage />} />
          <Route path="members" element={<MemberListPage />} />
          <Route path="loans" element={<LoanListPage />} />
          <Route path="fines" element={<FineListPage />} />
          <Route path="notifications" element={<NotificationListPage />} />
        </Route>
      </Route>

      {/* Readers: the catalogue, their loans and their fines (borrowing is at the librarian's counter) */}
      <Route element={<RequireAuth role={MEMBER} />}>
        <Route path="me" element={<ReaderLayout />}>
          <Route index element={<DiscoverPage />} />
          <Route path="books/:id" element={<BookDetailPage />} />
          <Route path="loans" element={<MyLoansPage />} />
          <Route path="fines" element={<MyFinesPage />} />
          <Route path="notifications" element={<MyNotificationsPage />} />
          <Route path="*" element={<Navigate to="/me" replace />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
