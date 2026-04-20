import { useState } from 'react';
import * as yup from 'yup';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuthStore } from "../store/authStore";
import { isTokenExpired } from '../utils/jwt';


const loginSchema = yup.object().shape({
  email: yup
      .string()
      .required('Adresa de email este obligatorie.')
      .email('Te rugăm să introduci o adresă de email validă.'),
  password: yup
      .string()
      .required('Parola este obligatorie.')
      .min(6, 'Parola trebuie să aibă minimum 6 caractere.')
});

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const token = useAuthStore((state) => state.token);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const login = useAuthStore((state) => state.login);

  if (isAuthenticated && token && !isTokenExpired(token)) {
    return <Navigate to="/dashboard" replace />;
  }

  const createMockJwt = () => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));

    // Alocăm rolul de 'Child' strict pentru credențialele copilului
    const assignedRole = email.toLowerCase() === 'copil@example.com' ? 'Child' : 'Parent';

    const payload = btoa(
        JSON.stringify({
          sub: email,
          role: assignedRole,
          exp: Math.floor(Date.now() / 1000) + 60 * 60,
        }),
    );

    return `${header}.${payload}.mock_signature`;
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      await loginSchema.validate({ email, password });
      setIsLoading(true);

      const mockApiCall = new Promise<{ token: string }>((resolve, reject) => {
        setTimeout(() => {
          if (email && password) {
            resolve({ token: createMockJwt() });
          } else {
            reject(new Error("Eroare de la server: Date incorecte."));
          }
        }, 1500);
      });

      const response = await mockApiCall;
      login(response.token);
      navigate('/dashboard', { replace: true });

    } catch (err: unknown) {
      if (err instanceof yup.ValidationError) {
        setError(err.message);
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('A apărut o eroare neașteptată.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Clasă reutilizabilă pentru a menține codul curat
  const inputStyle = "w-full bg-white border border-brand-border rounded-[10px] px-4 py-3 text-sm text-brand-dark placeholder:text-brand-muted focus:outline-none focus:border-brand-muted transition-colors";

  return (
      <div className="flex-1 w-full flex items-center justify-center p-6">
        <div className="w-full max-w-md bg-white border border-brand-border rounded-[14px] p-8 shadow-[0_4px_20px_rgba(0,0,0,0.02)] flex flex-col fade-in-up">

          {/* Header Section */}
          <div className="flex flex-col items-center mb-8 text-center">
            <div className="w-12 h-12 rounded-[10px] bg-brand-dark flex items-center justify-center text-white font-bold text-xl mb-4 shadow-sm">
              FA
            </div>
            <h1 className="text-2xl font-medium text-brand-dark tracking-tight mb-1">FamilyAgent</h1>
            <p className="text-[13px] text-brand-muted">Gestionează cheltuielile familiei eficient.</p>
          </div>

          {/* Error Display */}
          {error && (
              <div className="bg-red-50 border border-red-100 text-red-600 px-4 py-3 rounded-[10px] text-sm mb-6 flex items-center gap-2">
                <span>⚠️</span>
                <span>{error}</span>
              </div>
          )}

          <form onSubmit={handleLogin} className="flex flex-col gap-5">
            {/* Email Field */}
            <div>
              <label className="block text-[11px] tracking-[1px] text-brand-muted font-medium mb-2 uppercase">Email</label>
              <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className={inputStyle}
                  placeholder="username@exemplu.com"
              />
            </div>

            {/* Password Field */}
            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="block text-[11px] tracking-[1px] text-brand-muted font-medium uppercase">Parolă</label>
                <a href="#" className="text-xs font-medium text-brand-muted hover:text-brand-dark transition-colors">Ai uitat?</a>
              </div>
              <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className={inputStyle}
                  placeholder="••••••••"
              />
            </div>

            {/* Login Button */}
            <button
                type="submit"
                disabled={isLoading}
                className="mt-2 w-full bg-brand-dark text-white rounded-[10px] py-3.5 text-sm font-medium flex items-center justify-center gap-2 transition-all hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? (
                  <>
                    <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Se procesează...
                  </>
              ) : 'Intră în cont'}
            </button>
          </form>

          {/* Demo Credentials */}
          <div className="mt-8 bg-[#FFF8F2] border border-[#F0DFD0] rounded-[10px] p-4 text-sm text-[#7A5C44]">
            <p className="font-medium mb-1 flex items-center gap-1.5">
              <span>💡</span> Date de test:
            </p>
            <p className="text-[13px] opacity-90">Cont Părinte: test@example.com</p>
            <p className="text-[13px] opacity-90">Cont Copil: copil@example.com</p>
            <p className="text-[13px] opacity-90">Parola: password123</p>
          </div>

          {/* Divider */}
          <div className="mt-6 text-center">
            <p className="text-[11px] text-brand-muted">
              Aceasta este o versiune demo. Autentificarea este simulată.
            </p>
          </div>
        </div>
      </div>
  );
}