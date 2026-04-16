import { Navigate, Route, Routes } from 'react-router-dom'
import PrivateRoute from './components/PrivateRoute'
import Dashboard from './pages/Dashboard'
import Expenses from './pages/Expenses'
import LoginForm from './pages/LoginForm'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginForm />} />
      <Route
        path="/dashboard"
        element={(
          <PrivateRoute>
            <Dashboard />
          </PrivateRoute>
        )}
      />
      <Route
        path="/expenses"
        element={(
          <PrivateRoute>
            <Expenses />
          </PrivateRoute>
        )}
      />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

export default App