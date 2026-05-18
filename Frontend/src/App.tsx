import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import PrivateRoute from './components/PrivateRoute'
import ProtectedLayout from './components/ProtectedLayout'
import Dashboard from './pages/Dashboard'
import KidDashboard from './pages/KidDashboard'
import LoginForm from './pages/LoginForm'
import RegisterForm from './pages/RegisterForm'
import ForgotPassword from './pages/ForgotPassword'
import ResetPassword from './pages/ResetPassword'
import LandingPage from './pages/LandingPage'
import ExpenseForm from './components/ExpenseForm'
import Expenses from './pages/Expenses'
import ExpenseMap from './pages/ExpenseMap'
import ExpensesMapAll from './pages/ExpensesMapAll'
import Reports from './pages/Reports'
import FamilySettings from './pages/FamilySettings'
import AppErrorBoundary from './components/AppErrorBoundary'

const PUBLIC_PATHS = ['/', '/login', '/register', '/forgot-password', '/reset-password']

function DevCrashGate() {
    const location = useLocation()

    if (import.meta.env.DEV) {
        const params = new URLSearchParams(location.search)
        if (params.has('crash')) {
            throw new Error('Demo crash triggered via ?crash=1 (remove it to recover)')
        }
    }

    return null
}

function App() {
    const location = useLocation()
    PUBLIC_PATHS.includes(location.pathname.toLowerCase());
    return (
        <div style={{ width: '100%' }}>
            <AppErrorBoundary>
                <DevCrashGate />
                <Routes>
                    {/* Public */}
                    <Route path="/" element={<LandingPage />} />
                    <Route path="/login" element={<LoginForm />} />
                    <Route path="/register" element={<RegisterForm />} />
                    <Route path="/forgot-password" element={<ForgotPassword />} />
                    <Route path="/reset-password" element={<ResetPassword />} />

                    {/* Protected — sidebar layout */}
                    <Route element={<PrivateRoute><ProtectedLayout /></PrivateRoute>}>
                        <Route path="/dashboard" element={<Dashboard />} />
                        <Route path="/kid-dashboard" element={<KidDashboard />} />
                        <Route path="/add-expense" element={<ExpenseForm />} />
                        <Route path="/expenses" element={<Expenses />} />
                        <Route path="/expenses/map" element={<ExpenseMap />} />
                        <Route path="/reports" element={<Reports />} />
                        <Route path="/expenses/all-map" element={<ExpensesMapAll />} />
                        <Route path="/family" element={<FamilySettings />} />
                    </Route>

                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </AppErrorBoundary>

        </div>
    )
}

export default App