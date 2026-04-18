import { Navigate, Route, Routes } from 'react-router-dom'
import PrivateRoute from './components/PrivateRoute'
import Dashboard from './pages/Dashboard'
import LoginForm from './pages/LoginForm'
import ExpenseForm from './components/ExpenseForm'
import Expenses from './pages/Expenses'

function App() {
    return (
        <div className="min-h-screen w-full flex flex-col bg-[#FAF8F5] font-sans text-[#2D2926]">
            <Routes>
                {/* Ruta Publică */}
                <Route path="/login" element={<LoginForm />} />

                {/* Rute Protejate */}
                <Route
                    path="/dashboard"
                    element={(
                        <PrivateRoute>
                            <Dashboard />
                        </PrivateRoute>
                    )}
                />

                <Route
                    path="/add-expense"
                    element={(
                        <PrivateRoute>
                            <ExpenseForm />
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

                {/* Ruta Catch-all: Dacă utilizatorul introduce un URL invalid, este trimis la dashboard */}
                <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
        </div>
    )
}

export default App