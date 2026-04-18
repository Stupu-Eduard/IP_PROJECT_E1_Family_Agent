import { Navigate, Route, Routes } from 'react-router-dom'
import PrivateRoute from './components/PrivateRoute'
import Dashboard from './pages/Dashboard'
import LoginForm from './pages/LoginForm'
import ExpenseForm from './components/ExpenseForm' // 1. IMPORTĂ COMPONENTA AICI

function App() {
    return (
        <div className="min-h-screen w-full flex flex-col bg-brand-bg font-sans text-brand-dark">
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

                {/* 2. ADAUGĂ RUTA ASTA - FĂRĂ EA NU SE DESCHIDE NIMIC */}
                <Route
                    path="/add-expense"
                    element={(
                        <PrivateRoute>
                            <ExpenseForm />
                        </PrivateRoute>
                    )}
                />

                {/* Ruta Catch-all - acum va ignora /add-expense pentru că are potrivire mai sus */}
                <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
        </div>
    )
}

export default App