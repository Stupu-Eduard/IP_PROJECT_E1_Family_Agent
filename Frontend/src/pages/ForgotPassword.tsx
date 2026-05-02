import { useState } from 'react';
import * as yup from 'yup';
import { Link } from 'react-router-dom';

// ── Schema validare (NEATINSĂ) ─────────────────────────────────────────────
const resetSchema = yup.object().shape({
    email: yup.string().required('Adresa de email este obligatorie.').email('Email invalid.'),
});

export default function ForgotPassword() {
    // ── State (NEATINS) ────────────────────────────────────────────────────
    const [email,   setEmail]   = useState('');
    const [status,  setStatus]  = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
    const [message, setMessage] = useState('');

    // ── handleReset (NEATINS) ──────────────────────────────────────────────
    const handleReset = async (e: React.FormEvent) => {
        e.preventDefault();
        setStatus('idle');
        setMessage('');
        try {
            await resetSchema.validate({ email });
            setStatus('loading');
            await new Promise((resolve) => setTimeout(resolve, 1500));
            setStatus('success');
            setMessage('Dacă adresa există în sistem, vei primi un link pentru resetarea parolei.');
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
                        Reset
                    </div>

                    <h1 style={{
                        fontSize: 64, fontWeight: 400, letterSpacing: '-2.5px',
                        lineHeight: 0.98, margin: '0 0 24px', color: 'var(--color-ink)',
                    }}>
                        Se<br />
                        <em style={{ color: 'var(--color-primary)', fontStyle: 'italic' }}>întâmplă.</em>
                    </h1>

                    <p style={{ fontSize: 16, color: 'var(--color-muted)', lineHeight: 1.55, margin: '0 0 32px', maxWidth: 380 }}>
                        Toți uităm parole. Îți trimitem un link sigur pe email — funcționează 30 de minute.
                    </p>

                    {/* Card info */}
                    <div style={{
                        display: 'flex', alignItems: 'flex-start', gap: 14,
                        padding: '14px 18px', background: '#fff',
                        border: '1px solid var(--color-border)', borderRadius: 14, maxWidth: 360,
                    }}>
                        <div style={{
                            width: 36, height: 36, borderRadius: 10,
                            background: 'var(--color-primary-tint)', color: 'var(--color-primary)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                        }}>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M12 3 4 6v6c0 4.5 3.4 8.4 8 9 4.6-.6 8-4.5 8-9V6l-8-3z"/><path d="m9 12 2 2 4-4"/>
                            </svg>
                        </div>
                        <div>
                            <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-ink)', marginBottom: 2 }}>Link securizat</div>
                            <div style={{ fontSize: 12, color: 'var(--color-muted)', lineHeight: 1.5 }}>
                                Linkul expiră în 30 de minute și poate fi folosit o singură dată.
                            </div>
                        </div>
                    </div>
                </div>

                {/* Dreapta — form */}
                <div className="fade-up" style={{ maxWidth: 380, width: '100%', justifySelf: 'end' }}>

                    {/* Mesaje status (NEATINSE) */}
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
                            <div style={{ fontWeight: 600, marginBottom: 4 }}>✓ Email trimis!</div>
                            {message}
                        </div>
                    )}

                    <form onSubmit={handleReset} style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>

                        {/* Email */}
                        <div style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
                            <div className="label" style={{ fontSize: 10, marginBottom: 6 }}>EMAIL</div>
                            <input
                                type="email" value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="adresa@exemplu.com"
                                disabled={status === 'success' || status === 'loading'}
                                style={{
                                    width: '100%', border: 'none', outline: 'none',
                                    background: 'transparent', fontFamily: 'inherit',
                                    fontSize: 18, color: 'var(--color-ink)', padding: 0,
                                    opacity: status === 'success' ? 0.5 : 1,
                                }}
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={status === 'success' || status === 'loading'}
                            className="btn btn-primary"
                            style={{
                                marginTop: 28, padding: '14px 20px', borderRadius: 14,
                                fontSize: 14, justifyContent: 'space-between',
                                opacity: status === 'success' || status === 'loading' ? 0.6 : 1,
                                cursor: status === 'success' ? 'not-allowed' : 'pointer',
                            }}
                        >
                            <span>{status === 'loading' ? 'Se procesează...' : status === 'success' ? 'Link trimis!' : 'Trimite link de resetare'}</span>
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

                    {status === 'success' && (
                        <div style={{ marginTop: 20, textAlign: 'center' }}>
                            <Link to="/login" style={{
                                fontSize: 13, fontWeight: 600, color: 'var(--color-ink)',
                                textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 6,
                            }}>
                                ← Înapoi la autentificare
                            </Link>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}