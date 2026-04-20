import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import PrivateRoute from './components/PrivateRoute'
import Dashboard from './pages/Dashboard'
import KidDashboard from './pages/KidDashboard'
import LoginForm from './pages/LoginForm'
import RegisterForm from './pages/RegisterForm'
import ForgotPassword from './pages/ForgotPassword' // Importul nou
import ExpenseForm from './components/ExpenseForm'
import Expenses from './pages/Expenses'
import Reports from './pages/Reports'
import FamilySettings from './pages/FamilySettings'
import ChatAI from './components/ChatAi'

function App() {
    const location = useLocation();

    // Am adăugat /forgot-password pe lista de pagini "curate"
    const isAuthPage = ['/login', '/register', '/forgot-password'].includes(location.pathname.toLowerCase());

    return (
        <div className="min-h-screen w-full flex flex-col bg-[#FAF8F5] font-sans text-[#2D2926]">
            <Routes>
                {/* Rute Publice */}
                <Route path="/login" element={<LoginForm />} />
                <Route path="/register" element={<RegisterForm />} />
                <Route path="/forgot-password" element={<ForgotPassword />} />

                {/* Rute Protejate */}
                <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
                <Route path="/kid-dashboard" element={<PrivateRoute><KidDashboard /></PrivateRoute>} />
                <Route path="/add-expense" element={<PrivateRoute><ExpenseForm /></PrivateRoute>} />
                <Route path="/expenses" element={<PrivateRoute><Expenses /></PrivateRoute>} />
                <Route path="/reports" element={<PrivateRoute><Reports /></PrivateRoute>} />
                <Route path="/family" element={<PrivateRoute><FamilySettings /></PrivateRoute>} />

                <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>

            {!isAuthPage && <ChatAI />}
        </div>
    )
}

export default App