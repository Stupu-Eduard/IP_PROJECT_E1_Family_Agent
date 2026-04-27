import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import PrivateRoute from './components/PrivateRoute'
import ProtectedLayout from './components/ProtectedLayout'
import Dashboard from './pages/Dashboard'
import KidDashboard from './pages/KidDashboard'
import LoginForm from './pages/LoginForm'
import RegisterForm from './pages/RegisterForm'
import ForgotPassword from './pages/ForgotPassword'
import ExpenseForm from './components/ExpenseForm'
import Expenses from './pages/Expenses'
import ExpenseMap from './pages/ExpenseMap'
import ExpensesMapAll from './pages/ExpensesMapAll'
import Reports from './pages/Reports'
import FamilySettings from './pages/FamilySettings'
import ChatAI from './components/ChatAi'

function App() {
    const location = useLocation();

    const isAuthPage = ['/login', '/register', '/forgot-password'].includes(location.pathname.toLowerCase());

    return (
        <div className="min-h-screen w-full flex flex-col bg-[#FAF8F5] font-sans text-[#2D2926]">
            <Routes>
                {/* Rute Publice */}
                <Route path="/login" element={<LoginForm />} />
                <Route path="/register" element={<RegisterForm />} />
                <Route path="/forgot-password" element={<ForgotPassword />} />

                {/* Rute Protejate */}
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

                <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>

            {!isAuthPage && <ChatAI />}
        </div>
    )
}

export default App