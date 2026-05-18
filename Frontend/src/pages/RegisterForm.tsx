import { useState } from 'react';
import * as yup from 'yup';
import { Navigate, useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { isTokenExpired } from '../utils/jwt';
import { api } from '../services/api';
import type { RegisterDTO } from '../types/AuthDTO';

// ── Schema validare (NEATINSĂ) ─────────────────────────────────────────────
const registerSchema = yup.object().shape({
    name:            yup.string().required('Numele este obligatoriu.').min(2, 'Minim 2 caractere.'),
    email:           yup.string().required('Email obligatoriu.').email('Email invalid.'),
    password:        yup.string().required('Parola este obligatorie.').min(8, 'Minim 8 caractere.'),
    confirmPassword: yup.string().required('Confirmă parola.').oneOf([yup.ref('password')], 'Parolele nu coincid.'),
    favoriteAnimal:  yup.string().required('Animalul preferat este obligatoriu.'),
    favoriteColor:   yup.string().required('Culoarea preferată este obligatorie.'),
    childhoodStreet: yup.string().required('Strada copilăriei este obligatorie.'),
});

// ── Password strength helper ───────────────────────────────────────────────
function PasswordStrength({ value }: { value: string }) {
    const score = (() => {
        let s = 0;
        if (value.length >= 8) s++;
        if (/[A-Z]/.test(value)) s++;
        if (/[0-9]/.test(value)) s++;
        if (/[^A-Za-z0-9]/.test(value)) s++;
        return s;
    })();
    const labels = ['Prea scurtă', 'Slabă', 'Acceptabilă', 'Bună', 'Puternică'];
    const colors = ['var(--color-muted-3)', '#D08C5C', 'var(--color-primary-soft)', 'var(--color-primary)', 'var(--color-ink)'];
    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 8 }}>
            <div style={{ display: 'flex', gap: 4, flex: 1 }}>
                {[0, 1, 2, 3].map((i) => (
                    <div key={i} style={{
                        flex: 1, height: 3, borderRadius: 2,
                        background: i < score ? colors[score] : 'var(--color-border)',
                        transition: 'background 0.3s ease',
                    }} />
                ))}
            </div>
            <span style={{ fontSize: 11, color: 'var(--color-muted)', minWidth: 80, textAlign: 'right' }}>
                {value ? labels[score] : 'Min. 8 caractere'}
            </span>
        </div>
    );
}

export default function RegisterForm() {
    const navigate = useNavigate();

    // ── State (NEATINS) ────────────────────────────────────────────────────
    const [name,            setName]            = useState('');
    const [email,           setEmail]           = useState('');
    const [password,        setPassword]        = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [favoriteAnimal,  setFavoriteAnimal]  = useState('');
    const [favoriteColor,   setFavoriteColor]   = useState('');
    const [childhoodStreet, setChildhoodStreet] = useState('');
    const [role,            setRole]            = useState<'Parent' | 'Child'>('Parent');
    const [error,           setError]           = useState('');
    const [isLoading,       setIsLoading]       = useState(false);

    const token           = useAuthStore((state) => state.token);
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const login           = useAuthStore((state) => state.login);

    // ── Redirect dacă autentificat (NEATINS) ───────────────────────────────
    if (isAuthenticated && token && !isTokenExpired(token)) {
        return <Navigate to="/dashboard" replace />;
    }

    // ── handleRegister (NEATINS) ───────────────────────────────────────────
    const handleRegister = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        try {
            await registerSchema.validate({
                name,
                email,
                password,
                confirmPassword,
                favoriteAnimal,
                favoriteColor,
                childhoodStreet,
            });
            setIsLoading(true);
            const payload: RegisterDTO = { name, email, password, favoriteAnimal, favoriteColor, childhoodStreet, role };
            const response = await api.post<{ token: string }>('/api/v1/auth/register', payload);
            if (response.data.token) {
                login(response.data.token);
                navigate('/dashboard', { replace: true });
            }
        } catch (err: unknown) {
            if (err instanceof yup.ValidationError) setError(err.message);
            else if (err instanceof Error) setError(err.message);
            else setError('Eroare neașteptată de rețea.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div style={{
            width: '100%', minHeight: '100vh',
            background: 'var(--color-bg)',
            display: 'flex', flexDirection: 'column',
            padding: '32px 56px 56px', fontFamily: 'inherit',
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
                    <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--color-ink)' }}>FamilyAgent</span>
                </div>
                <div style={{ fontSize: 12.5, color: 'var(--color-muted)' }}>
                    Ai deja cont?{' '}
                    <Link to="/login" style={{ color: 'var(--color-ink)', fontWeight: 600, textDecoration: 'none' }}>
                        Conectează-te
                    </Link>
                </div>
            </div>

            {/* Grid 2 coloane */}
            <div style={{
                display: 'grid', gridTemplateColumns: '1fr 1fr',
                gap: 80, flex: 1, alignItems: 'center',
                maxWidth: 1000, margin: '0 auto', width: '100%',
            }}>

                {/* Stânga — copy */}
                <div className="fade-up">
                    <div style={{
                        fontSize: 11, letterSpacing: 2, fontWeight: 600,
                        color: 'var(--color-primary)', textTransform: 'uppercase',
                        marginBottom: 24, display: 'inline-flex', alignItems: 'center', gap: 8,
                    }}>
                        <span style={{ width: 24, height: 1, background: 'var(--color-primary)', display: 'inline-block' }} />
                        Capitolul 1
                    </div>

                    <h1 style={{
                        fontSize: 64, fontWeight: 400, letterSpacing: '-2.5px',
                        lineHeight: 0.98, margin: '0 0 24px', color: 'var(--color-ink)',
                    }}>
                        Hai să<br />
                        <em style={{ color: 'var(--color-primary)', fontStyle: 'italic' }}>începem.</em>
                    </h1>

                    <p style={{ fontSize: 16, color: 'var(--color-muted)', lineHeight: 1.55, margin: '0 0 32px', maxWidth: 380 }}>
                        Adaugă primul membru al familiei în mai puțin de un minut. Începe cu tine.
                    </p>

                    {/* Feature chips */}
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                        {['OCR cu AI', 'Multi-rol', 'Hartă live', 'Buget familiar', 'Export Excel'].map((t) => (
                            <span key={t} className="chip chip-neutral" style={{ padding: '5px 12px' }}>{t}</span>
                        ))}
                    </div>
                </div>

                {/* Dreapta — form */}
                <div className="fade-up" style={{ maxWidth: 380, width: '100%', justifySelf: 'end' }}>

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

                    <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>

                        {/* Selector rol */}
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 8 }}>
                            {([
                                { value: 'Parent', label: 'Sunt Părinte', sub: 'Creez o familie nouă' },
                                { value: 'Child',  label: 'Sunt Copil',   sub: 'Aștept o invitație'  },
                            ] as const).map(({ value, label, sub }) => (
                                <button
                                    key={value}
                                    type="button"
                                    onClick={() => setRole(value)}
                                    style={{
                                        border: `1.5px solid ${role === value ? 'var(--color-ink)' : 'var(--color-border)'}`,
                                        borderRadius: 12, padding: '12px 14px', background: role === value ? 'var(--color-ink)' : 'transparent',
                                        color: role === value ? '#fff' : 'var(--color-ink)',
                                        cursor: 'pointer', textAlign: 'left', transition: 'all 0.15s ease',
                                        fontFamily: 'inherit',
                                    }}
                                >
                                    <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 2 }}>{label}</div>
                                    <div style={{ fontSize: 11, opacity: 0.7 }}>{sub}</div>
                                </button>
                            ))}
                        </div>

                        {/* Nume */}
                        <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                            <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>NUME</div>
                            <input
                                type="text" value={name} onChange={(e) => setName(e.target.value)}
                                placeholder="Ana Popescu" disabled={isLoading}
                                style={{ width: '100%', border: 'none', outline: 'none', background: 'transparent', fontFamily: 'inherit', fontSize: 18, color: 'var(--color-ink)', padding: 0 }}
                            />
                        </div>

                        {/* Email */}
                        <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                            <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>EMAIL</div>
                            <input
                                type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                                placeholder="username@exemplu.com" disabled={isLoading}
                                style={{ width: '100%', border: 'none', outline: 'none', background: 'transparent', fontFamily: 'inherit', fontSize: 18, color: 'var(--color-ink)', padding: 0 }}
                            />
                        </div>

                        {/* Parolă */}
                        <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                            <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>PAROLĂ</div>
                            <input
                                type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••" disabled={isLoading}
                                style={{ width: '100%', border: 'none', outline: 'none', background: 'transparent', fontFamily: 'inherit', fontSize: 18, color: 'var(--color-ink)', padding: 0, letterSpacing: '0.1em' }}
                            />
                            <PasswordStrength value={password} />
                        </div>

                        {/* Confirmare parolă */}
                        <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                            <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>CONFIRMARE PAROLĂ</div>
                            <input
                                type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)}
                                placeholder="••••••••" disabled={isLoading}
                                style={{ width: '100%', border: 'none', outline: 'none', background: 'transparent', fontFamily: 'inherit', fontSize: 18, color: 'var(--color-ink)', padding: 0, letterSpacing: '0.1em' }}
                            />
                        </div>

                        <div className="label" style={{ fontSize: 10, margin: '16px 0 6px' }}>
                            ÎNTREBĂRI DE SECURITATE
                        </div>

                        {/* Animal preferat */}
                        <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                            <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>CARE ESTE ANIMALUL TĂU PREFERAT?</div>
                            <input
                                type="text" value={favoriteAnimal} onChange={(e) => setFavoriteAnimal(e.target.value)}
                                placeholder="ex: pisica" disabled={isLoading}
                                style={{ width: '100%', border: 'none', outline: 'none', background: 'transparent', fontFamily: 'inherit', fontSize: 18, color: 'var(--color-ink)', padding: 0 }}
                            />
                        </div>

                        {/* Culoare preferată */}
                        <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                            <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>CARE ESTE CULOAREA TA PREFERATĂ?</div>
                            <input
                                type="text" value={favoriteColor} onChange={(e) => setFavoriteColor(e.target.value)}
                                placeholder="ex: albastru" disabled={isLoading}
                                style={{ width: '100%', border: 'none', outline: 'none', background: 'transparent', fontFamily: 'inherit', fontSize: 18, color: 'var(--color-ink)', padding: 0 }}
                            />
                        </div>

                        {/* Strada copilăriei */}
                        <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                            <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>PE CE STRADĂ AI LOCUIT ÎN COPILĂRIE?</div>
                            <input
                                type="text" value={childhoodStreet} onChange={(e) => setChildhoodStreet(e.target.value)}
                                placeholder="ex: Strada Lalelelor" disabled={isLoading}
                                style={{ width: '100%', border: 'none', outline: 'none', background: 'transparent', fontFamily: 'inherit', fontSize: 18, color: 'var(--color-ink)', padding: 0 }}
                            />
                        </div>

                        <button
                            type="submit" disabled={isLoading}
                            className="btn btn-primary"
                            style={{ marginTop: 28, padding: '14px 20px', borderRadius: 14, fontSize: 14, justifyContent: 'space-between', opacity: isLoading ? 0.7 : 1 }}
                        >
                            <span>{isLoading ? 'Se procesează...' : 'Creează contul'}</span>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M5 12h14"/><path d="m13 5 7 7-7 7"/>
                            </svg>
                        </button>

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