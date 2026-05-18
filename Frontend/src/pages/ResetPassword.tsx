import { useMemo, useState } from 'react';
import * as yup from 'yup';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';

const resetSchema = yup.object().shape({
    newPassword: yup.string().required('Parola este obligatorie.').min(8, 'Minim 8 caractere.'),
    confirmPassword: yup.string().required('Confirmă parola.').oneOf([yup.ref('newPassword')], 'Parolele nu coincid.'),
});

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

export default function ResetPassword() {
    const navigate = useNavigate();
    const resetPayload = useMemo(() => {
        const raw = sessionStorage.getItem('resetPayload');
        if (!raw) return null;
        try {
            return JSON.parse(raw) as {
                email: string;
                question1: string;
                answer1: string;
                question2: string;
                answer2: string;
            };
        } catch {
            return null;
        }
    }, []);

    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
    const [message, setMessage] = useState('');

    const handleReset = async (e: React.FormEvent) => {
        e.preventDefault();
        setStatus('idle');
        setMessage('');
        try {
            if (!resetPayload) {
                throw new Error('Lipsesc datele de verificare. Reîncepe resetarea.');
            }
            await resetSchema.validate({ newPassword, confirmPassword });
            setStatus('loading');
            await api.post('/api/v1/auth/reset-password', {
                ...resetPayload,
                newPassword,
            });
            sessionStorage.removeItem('resetPayload');
            setStatus('success');
            setMessage('Parola a fost actualizată.');
            navigate('/login', { replace: true });
        } catch (err: any) {
            setStatus('error');
            setMessage(err.message);
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
                    Înapoi la{' '}
                    <Link to="/login" style={{ color: 'var(--color-ink)', fontWeight: 600, textDecoration: 'none' }}>
                        Autentificare
                    </Link>
                </div>
            </div>

            {/* Grid 2 coloane */}
            <div style={{
                display: 'grid', gridTemplateColumns: '1fr 1fr',
                gap: 80, flex: 1, alignItems: 'center',
                maxWidth: 1000, margin: '0 auto', width: '100%'
            }}>

                {/* Stânga — copy */}
                <div className="fade-up">
                    <div style={{
                        fontSize: 11, letterSpacing: 2, fontWeight: 600,
                        color: 'var(--color-primary)', textTransform: 'uppercase',
                        marginBottom: 24, display: 'inline-flex', alignItems: 'center', gap: 8,
                    }}>
                        <span style={{ width: 24, height: 1, background: 'var(--color-primary)', display: 'inline-block' }} />
                        Resetare
                    </div>

                    <h1 style={{
                        fontSize: 64, fontWeight: 400, letterSpacing: '-2.5px',
                        lineHeight: 0.98, margin: '0 0 24px', color: 'var(--color-ink)'
                    }}>
                        Parolă
                        <br />
                        <em style={{ color: 'var(--color-primary)', fontStyle: 'italic' }}>nouă.</em>
                    </h1>

                    <p style={{ fontSize: 16, color: 'var(--color-muted)', lineHeight: 1.55, margin: '0 0 32px', maxWidth: 380 }}>
                        Alege o parolă nouă și confirm-o pentru a-ți continua accesul.
                    </p>
                </div>

                {/* Dreapta — form */}
                <div className="fade-up" style={{ maxWidth: 380, width: '100%', justifySelf: 'end' }}>

                    {status === 'error' && (
                        <div style={{
                            background: '#FEF2F2', border: '1px solid #FECACA',
                            borderRadius: 10, padding: '10px 14px', marginBottom: 20,
                            fontSize: 13, color: '#DC2626', display: 'flex', alignItems: 'center', gap: 8,
                        }}>
                            ⚠ {message}
                        </div>
                    )}

                    {status === 'success' && (
                        <div style={{
                            background: '#F0FDF4', border: '1px solid #BBF7D0',
                            borderRadius: 10, padding: '14px 16px', marginBottom: 20,
                            fontSize: 13, color: '#166534',
                        }}>
                            <div style={{ fontWeight: 600, marginBottom: 4 }}>✓ Resetare reușită!</div>
                            {message}
                        </div>
                    )}

                    <form onSubmit={handleReset} style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
                        <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                            <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>PAROLĂ NOUĂ</div>
                            <input
                                type="password" value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                placeholder="••••••••"
                                disabled={status === 'loading'}
                                style={{
                                    width: '100%', border: 'none', outline: 'none',
                                    background: 'transparent', fontFamily: 'inherit',
                                    fontSize: 18, color: 'var(--color-ink)', padding: 0,
                                    letterSpacing: '0.1em'
                                }}
                            />
                            <PasswordStrength value={newPassword} />
                        </div>

                        <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                            <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>CONFIRMARE PAROLĂ</div>
                            <input
                                type="password" value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                placeholder="••••••••"
                                disabled={status === 'loading'}
                                style={{
                                    width: '100%', border: 'none', outline: 'none',
                                    background: 'transparent', fontFamily: 'inherit',
                                    fontSize: 18, color: 'var(--color-ink)', padding: 0,
                                    letterSpacing: '0.1em'
                                }}
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={status === 'loading'}
                            className="btn btn-primary"
                            style={{
                                marginTop: 28, padding: '14px 20px', borderRadius: 14,
                                fontSize: 14, justifyContent: 'space-between',
                                opacity: status === 'loading' ? 0.6 : 1,
                            }}
                        >
                            <span>{status === 'loading' ? 'Se procesează...' : 'Schimbă parola'}</span>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M5 12h14"/><path d="m13 5 7 7-7 7"/>
                            </svg>
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
}
