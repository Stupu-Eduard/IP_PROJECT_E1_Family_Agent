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
    const payload = btoa(
      JSON.stringify({
        sub: email,
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

  return (
      <div className="page-shell page-shell--centered">

        <div className="surface-card surface-card--large fade-in-up">

          {/* Header Section */}
          <div className="panel-header">
            <div className="brand-mark brand-mark--sage">
              <svg
                  viewBox="0 0 24 24"
                  className="brand-mark__icon brand-mark__icon--sage"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
              >
                <path d="M9 22V8" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M15 22V8" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M9 10L12 7L15 10" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M12 7V2" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M6 16H18" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </div>
            <h1 className="panel-title">FamilyAgent</h1>
            <p className="panel-subtitle">Gestionează cheltuielile familiei</p>
          </div>

          {/* Error Display */}
          {error && (
              <div className="status-message status-message--error">
                <span className="status-message__icon">⚠️</span>
                <span>{error}</span>
              </div>
          )}

          <form onSubmit={handleLogin} className="form-stack">
            {/* Email Field */}
            <div className="form-field">
              <label className="form-label">Email</label>
              <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="form-control"
                  placeholder="username@exemplu.com"
              />
            </div>

            {/* Password Field */}
            <div className="form-field">
              <div className="form-link-row">
                <label className="form-label">Parolă</label>
                <a href="#" className="form-link">Ai uitat?</a>
              </div>
              <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="form-control"
                  placeholder="••••••••"
              />
            </div>

            {/* Login Button */}
            <button
                type="submit"
                disabled={isLoading}
                className={`form-submit ${isLoading ? 'form-submit--loading' : ''}`}
            >
              {isLoading ? (
                  <>
                    <div className="form-submit__spinner"></div>
                    Se procesează...
                  </>
              ) : 'Intră în cont'}
            </button>
          </form>

          {/* Demo Credentials */}
          <div className="info-card">
            <p className="info-card__title">💡 Date de test:</p>
            <p className="info-card__text">Email: test@example.com</p>
            <p className="info-card__text">Parola: password123</p>
          </div>

          {/* Divider */}
          <div className="form-note">
            <p>
              Aceasta este o versiune demo. Autentificarea este simulată.
            </p>
          </div>
        </div>
      </div>
  );
}