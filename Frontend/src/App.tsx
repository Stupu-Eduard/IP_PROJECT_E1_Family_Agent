import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import PrivateRoute from './components/PrivateRoute'
import Dashboard from './pages/Dashboard'
import KidDashboard from './pages/KidDashboard'
import LoginForm from './pages/LoginForm'
import ExpenseForm from './components/ExpenseForm'
import Expenses from './pages/Expenses'
import Reports from './pages/Reports'
import FamilySettings from './pages/FamilySettings'
import ChatAI from './components/ChatAi'

function App() {
    const location = useLocation();
    const isLoginPage = location.pathname.toLowerCase() === '/login';

    return (
        <div className="min-h-screen w-full flex flex-col bg-[#FAF8F5] font-sans text-[#2D2926]">
            <Routes>
                {/* Ruta Publică */}
                <Route path="/login" element={<LoginForm />} />

                {/* Rute Protejate */}
                <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
                <Route path="/kid-dashboard" element={<PrivateRoute><KidDashboard /></PrivateRoute>} />
                <Route path="/add-expense" element={<PrivateRoute><ExpenseForm /></PrivateRoute>} />
                <Route path="/expenses" element={<PrivateRoute><Expenses /></PrivateRoute>} />
                <Route path="/reports" element={<PrivateRoute><Reports /></PrivateRoute>} />
                <Route path="/family" element={<PrivateRoute><FamilySettings /></PrivateRoute>} />

                {/* Ruta Catch-all */}
                <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>

            {/* Asistentul AI Global (ascuns pe Login) */}
            {!isLoginPage && <ChatAI />}
        </div>
    )
}

export default App