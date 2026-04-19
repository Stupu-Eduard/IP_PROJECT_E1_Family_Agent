import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import PrivateRoute from './components/PrivateRoute'
import Dashboard from './pages/Dashboard'
import LoginForm from './pages/LoginForm'
import ExpenseForm from './components/ExpenseForm'
import Expenses from './pages/Expenses'
import ExpenseMap from './pages/ExpenseMap'
import Reports from './pages/Reports'
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
                <Route path="/add-expense" element={<PrivateRoute><ExpenseForm /></PrivateRoute>} />
                <Route path="/expenses" element={<PrivateRoute><Expenses /></PrivateRoute>} />
                <Route path="/expenses/map" element={<PrivateRoute><ExpenseMap /></PrivateRoute>} />
                <Route path="/reports" element={<PrivateRoute><Reports /></PrivateRoute>} />

                {/* Ruta Catch-all (Redirecționare fallback) */}
                <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>

            {/* Randăm Chat-ul GLOBAL, flotant, dar îl ascundem pe ecranul de Login */}
            {!isLoginPage && <ChatAI />}
        </div>
    )
}
export default App