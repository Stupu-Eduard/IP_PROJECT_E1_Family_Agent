import { useState } from 'react';
import * as yup from 'yup';
import { Navigate, useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { isTokenExpired } from '../utils/jwt';
import { getLoginErrorMessage, loginWithEmailPassword } from '../services/auth';

// ── Schema validare (NEATINSĂ) ─────────────────────────────────────────────
const loginSchema = yup.object().shape({
  email: yup.string().required('Adresa de email este obligatorie.').email('Te rugăm să introduci o adresă de email validă.'),
  password: yup.string().required('Parola este obligatorie.').min(6, 'Parola trebuie să aibă minimum 6 caractere.'),
});

export default function Login() {
  const navigate = useNavigate();

  // ── State (NEATINS) ──────────────────────────────────────────────────────
  const [email,     setEmail]     = useState('');
  const [password,  setPassword]  = useState('');
  const [error,     setError]     = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const token           = useAuthStore((state) => state.token);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const login           = useAuthStore((state) => state.login);

  // ── Redirect dacă deja autentificat (NEATINS) ────────────────────────────
  if (isAuthenticated && token && !isTokenExpired(token)) {
    return <Navigate to="/dashboard" replace />;
  }

  // ── Mock JWT (NEATINS) ────────────────────────────────────────────────────
  const createMockJwt = () => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const assignedRole = email.toLowerCase() === 'copil@example.com' ? 'Child' : 'Parent';
    const payload = btoa(JSON.stringify({
      sub: email, role: assignedRole,
      exp: Math.floor(Date.now() / 1000) + 60 * 60,
    }));
    return `${header}.${payload}.mock_signature`;
  };

  // ── handleLogin (NEATINS) ─────────────────────────────────────────────────
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await loginSchema.validate({ email, password });
      setIsLoading(true);
      await loginWithEmailPassword(email, password);
      login(createMockJwt());
      navigate('/dashboard', { replace: true });
    } catch (err: unknown) {
      if (err instanceof yup.ValidationError) setError(err.message);
      else setError(getLoginErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  // ── UI — Variation C (editorial / typography-first) ───────────────────────
  return (
      <div style={{
        width: '100%', minHeight: '100vh',
        background: 'var(--color-bg)',
        display: 'flex', flexDirection: 'column',
        padding: '32px 56px 56px',
        fontFamily: 'inherit',
      }}>

        {/* Topbar */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 56 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{
              width: 32, height: 32, borderRadius: 9,
              background: 'var(--color-ink)', color: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontWeight: 700, fontSize: 13,
            }}>FA</div>
            <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--color-ink)', letterSpacing: '-0.2px' }}>FamilyAgent</span>
          </div>
          <div style={{ fontSize: 12.5, color: 'var(--color-muted)' }}>
            Cont nou?{' '}
            <Link to="/register" style={{ color: 'var(--color-ink)', fontWeight: 600, textDecoration: 'none' }}>
              Înregistrează-te
            </Link>
          </div>
        </div>

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
              <em style={{ color: 'var(--color-primary)', fontStyle: 'italic', fontWeight: 400 }}>Ana.</em>
            </h1>

            <p style={{ fontSize: 16, color: 'var(--color-muted)', lineHeight: 1.55, margin: '0 0 32px', maxWidth: 380 }}>
              Săptămâna trecută familia ta a economisit 312 RON față de luna anterioară. Continuă tendința.
            </p>

            {/* Card hint */}
            <div style={{
              display: 'flex', alignItems: 'center', gap: 14,
              padding: '14px 18px', background: '#fff',
              border: '1px solid var(--color-border)', borderRadius: 14, maxWidth: 360,
            }}>
              <div style={{
                width: 36, height: 36, borderRadius: 10,
                background: 'var(--color-primary-tint)', color: 'var(--color-primary)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
              }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M5 3h14v18l-3-2-3 2-3-2-3 2-2-2V3z"/><path d="M9 8h6"/><path d="M9 12h6"/>
                </svg>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-ink)' }}>3 bonuri noi</div>
                <div style={{ fontSize: 12, color: 'var(--color-muted)' }}>Te așteaptă din ultima vizită</div>
              </div>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: 'var(--color-muted-3)' }}>
                <path d="M5 12h14"/><path d="m13 5 7 7-7 7"/>
              </svg>
            </div>
          </div>

          {/* Dreapta — form editorial fără borduri pe inputs */}
          <div className="fade-up" style={{ maxWidth: 380, width: '100%', justifySelf: 'end' }}>

            {/* Error */}
            {error && (
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  background: '#FEF2F2', border: '1px solid #FECACA',
                  borderRadius: 10, padding: '10px 14px', marginBottom: 20,
                  fontSize: 13, color: '#DC2626',
                }}>
                  ⚠ {error}
                </div>
            )}

            <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>

              {/* Email */}
              <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>EMAIL</div>
                <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="username@exemplu.com"
                    disabled={isLoading}
                    style={{
                      width: '100%', border: 'none', outline: 'none',
                      background: 'transparent', fontFamily: 'inherit',
                      fontSize: 18, color: 'var(--color-ink)', padding: 0,
                      opacity: isLoading ? 0.6 : 1,
                    }}
                />
              </div>

              {/* Parolă */}
              <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                  <span className="label" style={{ fontSize: 10 }}>PAROLĂ</span>
                  <Link to="/forgot-password" style={{
                    fontSize: 10, letterSpacing: 1, fontWeight: 600,
                    color: 'var(--color-primary)', textDecoration: 'none',
                  }}>
                    AI UITAT?
                  </Link>
                </div>
                <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    disabled={isLoading}
                    style={{
                      width: '100%', border: 'none', outline: 'none',
                      background: 'transparent', fontFamily: 'inherit',
                      fontSize: 18, color: 'var(--color-ink)', padding: 0,
                      letterSpacing: '0.1em', opacity: isLoading ? 0.6 : 1,
                    }}
                />
              </div>

              {/* Submit */}
              <button
                  type="submit"
                  disabled={isLoading}
                  className="btn btn-primary"
                  style={{ marginTop: 28, padding: '14px 20px', borderRadius: 14, fontSize: 14, justifyContent: 'space-between', opacity: isLoading ? 0.7 : 1 }}
              >
                <span>{isLoading ? 'Se procesează...' : 'Intră în cont'}</span>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M5 12h14"/><path d="m13 5 7 7-7 7"/>
                </svg>
              </button>

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