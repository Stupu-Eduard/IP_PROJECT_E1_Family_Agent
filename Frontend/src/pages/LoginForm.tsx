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

  const [email,     setEmail]     = useState('');
  const [password,  setPassword]  = useState('');
  const [error,     setError]     = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const token = useAuthStore((state) => state.token);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const loginStore      = useAuthStore((state) => state.login);

  if (isAuthenticated && token && !isTokenExpired(token)) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      await loginSchema.validate({ email, password });
      setIsLoading(true);
      
      const response = await loginWithEmailPassword(email, password);
      
      if (response.token) {
        loginStore(response.token);
        navigate('/dashboard', { replace: true });
      } else {
        setError('Eroare la autentificare: Token lipsă.');
      }
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

        {/* Grid 2 coloane */}
        <div style={{
          display: 'grid', gridTemplateColumns: '1fr 1fr',
          gap: 80, flex: 1, alignItems: 'center',
          maxWidth: 1000, margin: '0 auto', width: '100%',
        }}>

          {/* Stânga — copy editorial */}
          <div className="fade-up">
            <div style={{
              fontSize: 11, letterSpacing: 2, fontWeight: 600,
              color: 'var(--color-primary)', textTransform: 'uppercase',
              marginBottom: 24, display: 'inline-flex', alignItems: 'center', gap: 8,
            }}>
              <span style={{ width: 24, height: 1, background: 'var(--color-primary)', display: 'inline-block' }} />
              Capitolul 12
            </div>

            <h1 style={{
              fontSize: 64, fontWeight: 400, letterSpacing: '-2.5px',
              lineHeight: 0.98, margin: '0 0 24px', color: 'var(--color-ink)',
            }}>
              Bună,<br />
              <em style={{ color: 'var(--color-primary)', fontStyle: 'italic', fontWeight: 400 }}>Utilizatorule.</em>
            </h1>

            <p style={{ fontSize: 16, color: 'var(--color-muted)', lineHeight: 1.55, margin: '0 0 32px', maxWidth: 380 }}>
              Vizualizează și gestionează cheltuielile familiei tale într-un mod inteligent și sigur.
            </p>
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

              {/* Securitate */}
              <div style={{ marginTop: 18, fontSize: 11.5, color: 'var(--color-muted)', display: 'flex', alignItems: 'center', gap: 6 }}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 3 4 6v6c0 4.5 3.4 8.4 8 9 4.6-.6 8-4.5 8-9V6l-8-3z"/><path d="m9 12 2 2 4-4"/>
                </svg>
                Conexiune criptată · Datele rămân la tine.
              </div>
            </form>
          </div>
        </div>
      </div>
  );
}
