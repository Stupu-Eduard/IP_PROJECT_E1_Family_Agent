import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

// Importăm componentele unificate
import LoginForm from './pages/LoginForm'; // sau './components/LoginForm' (depinde unde l-ai lăsat)
import ExpenseForm from './components/ExpenseForm';

const App: React.FC = () => {
    return (
        <BrowserRouter>
            {/* Tot ce este în afara <Routes> se va vedea pe orice pagină
        (ex: un Navbar sus sau un Footer jos, dacă veți adăuga pe viitor)
      */}

            <Routes>
                {/* Ruta pentru Login */}
                <Route path="/login" element={<LoginForm />} />

                {/* Ruta pentru formularul de cheltuieli */}
                <Route path="/adauga-cheltuiala" element={<ExpenseForm />} />

                {/* Ruta de fallback (Dacă utilizatorul intră pe o pagină care nu există,
          cum ar fi ruta principală '/', îl trimitem forțat la Login)
        */}
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </BrowserRouter>
    );
};

export default App;