import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider, useAuth } from './api/AuthContext'
import Channels from './pages/Channels'
import { Navigate } from 'react-router-dom'
import Setting from './pages/Setting'
import ProtectedRoute from './components/ProtectedRoute'
import Sidebar from './components/Sidebar'
import Landing from './pages/Landing'
import Dashboard from './pages/Dashboard'
import JobDetail from './pages/JobDetail'
import Alerts from './pages/Alerts'

function AppLayout({ children }) {
  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar />
      {children}
    </div>
  )
}

function AppRoutes() {
  const { user } = useAuth()
  return (
    <Routes>
      {/* Public */}
      <Route path="/" element={user ? <Navigate to="/dashboard" replace /> : <Landing />}/>

      {/* Protected — all wrapped in sidebar layout */}
      <Route path="/dashboard" element={
        <ProtectedRoute>
          <AppLayout><Dashboard /></AppLayout>
        </ProtectedRoute>
      }/>
      <Route path="/jobs/:id" element={
        <ProtectedRoute>
          <AppLayout><JobDetail /></AppLayout>
        </ProtectedRoute>
      }/>
      <Route path="/alerts" element={
        <ProtectedRoute>
          <AppLayout><Alerts /></AppLayout>
        </ProtectedRoute>
      }/>
      <Route path="/channels" element={
        <ProtectedRoute>
          <AppLayout><Channels /></AppLayout>
        </ProtectedRoute>
      }/>
      <Route path="/settings" element={
        <ProtectedRoute>
          <AppLayout><Setting /></AppLayout>
        </ProtectedRoute>
      }/>
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
