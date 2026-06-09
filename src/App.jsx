import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import LoginPage    from './pages/LoginPage'
import SignupPage   from './pages/SignupPage'
import ChatListPage from './pages/ChatListPage'
import ProfilePage  from './pages/ProfilePage'

function Protected({ children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to="/login" replace />
}

function GuestOnly({ children }) {
  const { user } = useAuth()
  return !user ? children : <Navigate to="/chat" replace />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login"   element={<GuestOnly><LoginPage /></GuestOnly>} />
      <Route path="/signup"  element={<GuestOnly><SignupPage /></GuestOnly>} />
      <Route path="/chat"    element={<Protected><ChatListPage /></Protected>} />
      <Route path="/profile" element={<Protected><ProfilePage /></Protected>} />
      <Route path="*"        element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
