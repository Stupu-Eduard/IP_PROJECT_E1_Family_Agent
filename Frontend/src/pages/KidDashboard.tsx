import { useNavigate } from 'react-router-dom';
import { Camera } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import { getProfileDisplayName, getProfileInitials, getProfileRole } from '../utils/profile';

// ── Mock data (va fi înlocuit de API când e disponibil) ───────────────────
const myExpenses = [
    { id: 1, icon: '🛒', desc: 'Gustare la chioșc',      store: 'Carrefour · Astăzi · 13:20', amt: 8.50 },
    { id: 2, icon: '📚', desc: 'Caiete pentru școală',   store: 'Iulius Mall · Ieri · 17:05',  amt: 24.00 },
    { id: 3, icon: '🧃', desc: 'Suc + apă plată',        store: 'Mega Image · Luni · 09:40',   amt: 12.50 },
]

const budget    = 100   // buget lunar alocat de părinți
const spent     = 55    // cheltuit până acum
const balance   = budget - spent  // 45 RON
const spentPct  = Math.round((spent / budget) * 100)  // 55%

const goal = { name: 'Căști noi', saved: 180, target: 240, monthsLeft: 2 }
const goalPct = Math.round((goal.saved / goal.target) * 100)  // 75%

const quickActions = [
    { label: 'Scanează un bon', sub: 'Adăugare automată', nav: '/add-expense', dark: true,
        icon: <Camera size={18} color="white" /> },
    { label: 'Cheltuielile mele', sub: 'Vezi ce ai cumpărat', nav: '/expenses', dark: false,
        icon: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M5 3h14v18l-3-2-3 2-3-2-3 2-2-2V3z"/><path d="M9 8h6"/><path d="M9 12h6"/></svg> },
    { label: 'Familia mea', sub: 'Cine e în grup', nav: '/family', dark: false,
        icon: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="9" cy="8" r="3.2"/><path d="M2 21c0-3.3 3-6 7-6s7 2.7 7 6"/><circle cx="17" cy="9" r="2.6"/><path d="M22 20c0-2.6-2.2-4.7-5-4.7"/></svg> },
]

export default function KidDashboard() {
    const navigate = useNavigate()
    const token = useAuthStore((state) => state.token)
    const profile = useAuthStore((state) => state.profile)
    const hasAuthInfo = Boolean(token || profile)
    const userRole = hasAuthInfo ? getProfileRole(profile, token) : 'Child'
    const displayName = getProfileDisplayName(profile, token, 'Andrei P.')
    const initials = getProfileInitials(profile, token, displayName)
    const avatarUrl = profile?.avatarUrl ?? null

    return (
        <div style={{ maxWidth: 960, margin: '0 auto', width: '100%' }}>

            {/* ── Header ──────────────────────────────────────────────────────── */}
            <div className="fade-up" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
                <div className="chip chip-live">{userRole === 'Child' ? 'SESIUNE COPIL' : 'SESIUNE ACTIVĂ'} · {displayName.split(' ')[0]?.toUpperCase() ?? 'ANDREI'}</div>
                <button
                    className="btn btn-accent"
                    style={{ fontSize: 13 }}
                    onClick={() => navigate('/add-expense')}
                >
                    <Camera size={15} /> Scanează bonul
                </button>
            </div>

            <h1 className="h1 fade-up" style={{ marginBottom: 8 }}>
                Salut, <span style={{ color: 'var(--color-primary)' }}>{displayName.split(' ')[0]}</span> 👋
            </h1>
            <div className="fade-up" style={{ color: 'var(--color-muted)', fontSize: 14, marginBottom: 24, lineHeight: 1.6 }}>
                Ce ai cumpărat astăzi? Scanează bonul și suma se scade din buget.
            </div>

            {/* ── Card sold + avatar ──────────────────────────────────────────── */}
            <div className="card fade-up" style={{ marginBottom: 16, padding: '22px 24px' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 24, alignItems: 'center' }}>

                    {/* Stânga — sold */}
                    <div>
                        <div className="label" style={{ marginBottom: 10 }}>SOLD DISPONIBIL · APRILIE</div>
                        <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginBottom: 6 }}>
              <span style={{ fontSize: 52, fontWeight: 700, color: 'var(--color-ink)', letterSpacing: '-2px', lineHeight: 1 }}>
                {balance}
              </span>
                            <span style={{ fontSize: 16, fontWeight: 500, color: 'var(--color-muted)' }}>RON</span>
                        </div>
                        <div style={{ fontSize: 12.5, color: 'var(--color-muted)', marginBottom: 14 }}>
                            din {budget} RON alocați de părinți · resetare în 6 zile
                        </div>

                        {/* Progress bar */}
                        <div style={{ height: 8, background: '#F4F0EB', borderRadius: 6, overflow: 'hidden', marginBottom: 8 }}>
                            <div style={{
                                height: '100%',
                                width: `${spentPct}%`,
                                background: spentPct > 80
                                    ? 'linear-gradient(90deg, #E24B4A, #F09595)'
                                    : 'linear-gradient(90deg, var(--color-primary), var(--color-primary-soft))',
                                borderRadius: 6,
                                animation: 'bar-rise 1s var(--ease-out) 0.2s both',
                                transformOrigin: 'left',
                            }} />
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11.5, color: 'var(--color-muted)' }}>
                            <span>Cheltuit <strong style={{ color: 'var(--color-ink)' }}>{spent} RON</strong></span>
                            <span>{spentPct}% din buget</span>
                        </div>
                    </div>

                    {/* Dreapta — avatar */}
                    <div style={{ textAlign: 'center' }}>
                        <div style={{
                            width: 80, height: 80, borderRadius: 20,
                            background: 'linear-gradient(135deg, var(--color-primary), var(--color-primary-soft))',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontSize: 32, fontWeight: 700, color: 'white',
                            marginBottom: 10, boxShadow: '0 8px 20px rgba(201,123,75,0.3)',
                        }}>
                            {avatarUrl ? <img src={avatarUrl} alt="Avatar profil copil" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: 20 }} /> : initials}
                        </div>
                        <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--color-ink)' }}>{displayName}</div>
                        <div style={{
                            display: 'inline-flex', alignItems: 'center', gap: 4,
                            marginTop: 4, fontSize: 11, color: 'var(--color-muted)',
                            background: 'var(--color-surface)', borderRadius: 20, padding: '3px 10px',
                        }}>
                            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01z"/></svg>
                            Cont copil
                        </div>
                    </div>
                </div>
            </div>

            {/* ── Acțiuni rapide ──────────────────────────────────────────────── */}
            <div className="label fade-up" style={{ marginBottom: 12 }}>CE VREI SĂ FACI?</div>
            <div className="stagger" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 10, marginBottom: 20 }}>
                {quickActions.map((a) => (
                    <div
                        key={a.label}
                        className={`qa fade-up ${a.dark ? 'qa-dark' : ''}`}
                        onClick={() => navigate(a.nav)}
                        style={{ cursor: 'pointer' }}
                    >
                        <span className={`qa-icon ${a.dark ? 'qa-icon-dark' : ''}`}>{a.icon}</span>
                        <div style={{ flex: 1, minWidth: 0 }}>
                            <div style={{ fontSize: 13.5, fontWeight: 500, color: a.dark ? '#fff' : 'var(--color-ink)', letterSpacing: '-0.2px' }}>
                                {a.label}
                            </div>
                            <div style={{ fontSize: 11.5, color: a.dark ? 'rgba(255,255,255,0.55)' : 'var(--color-muted)', marginTop: 2 }}>
                                {a.sub}
                            </div>
                        </div>
                        <span className="qa-arrow" style={{ color: a.dark ? 'rgba(255,255,255,0.4)' : 'var(--color-muted-4)' }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2"><path d="M5 12h14"/><path d="m13 5 7 7-7 7"/></svg>
            </span>
                    </div>
                ))}
            </div>

            {/* ── Bottom: cheltuieli + obiectiv ───────────────────────────────── */}
            <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: 12 }}>

                {/* Cumpărăturile mele */}
                <div className="card fade-up" style={{ padding: 0, overflow: 'hidden' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 20px', borderBottom: '1px solid var(--color-border)' }}>
                        <div className="label">CUMPĂRĂTURILE MELE</div>
                        <button
                            style={{ fontSize: 12, color: 'var(--color-primary)', fontWeight: 500, background: 'none', border: 'none', cursor: 'pointer' }}
                            onClick={() => navigate('/expenses')}
                        >
                            Vezi tot →
                        </button>
                    </div>
                    <div className="stagger">
                        {myExpenses.map((e) => (
                            <div
                                key={e.id}
                                className="row-clickable fade-up"
                                style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 20px', borderBottom: '1px solid var(--color-border)' }}
                                onClick={() => navigate('/expenses')}
                            >
                                <div style={{
                                    width: 36, height: 36, borderRadius: 10,
                                    background: 'var(--color-primary-tint)',
                                    border: '1px solid var(--color-primary-edge)',
                                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                                    fontSize: 16, flexShrink: 0,
                                }}>
                                    {e.icon}
                                </div>
                                <div style={{ flex: 1, minWidth: 0 }}>
                                    <div style={{ fontSize: 13.5, fontWeight: 500, color: 'var(--color-ink)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                        {e.desc}
                                    </div>
                                    <div style={{ fontSize: 11.5, color: 'var(--color-muted)', marginTop: 2 }}>{e.store}</div>
                                </div>
                                <div className="row-amount" style={{ fontSize: 14, fontWeight: 500, color: 'var(--color-ink)', whiteSpace: 'nowrap' }}>
                                    {e.amt.toFixed(2)} <span style={{ fontSize: 10, color: 'var(--color-muted-2)', fontWeight: 400 }}>RON</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Obiectivul meu */}
                <div className="card fade-up" style={{ background: 'var(--color-ink)', border: 'none', padding: '20px' }}>
                    <div style={{ fontSize: 10, fontWeight: 600, letterSpacing: '1.2px', color: 'rgba(255,255,255,0.45)', textTransform: 'uppercase', marginBottom: 12 }}>
                        OBIECTIVUL MEU
                    </div>
                    <div style={{ fontSize: 20, fontWeight: 600, color: '#fff', letterSpacing: '-0.5px', marginBottom: 6 }}>
                        {goal.name}
                    </div>
                    <div style={{ fontSize: 12.5, color: 'rgba(255,255,255,0.55)', marginBottom: 18, lineHeight: 1.5 }}>
                        Pun deoparte din suma rămasă în fiecare lună.
                    </div>

                    <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginBottom: 12 }}>
                        <span style={{ fontSize: 32, fontWeight: 700, color: '#fff', letterSpacing: '-1px' }}>{goal.saved}</span>
                        <span style={{ fontSize: 14, color: 'rgba(255,255,255,0.45)', fontWeight: 400 }}>/ {goal.target} RON</span>
                    </div>

                    {/* Progress bar obiectiv */}
                    <div style={{ height: 6, background: 'rgba(255,255,255,0.12)', borderRadius: 4, overflow: 'hidden', marginBottom: 10 }}>
                        <div style={{
                            height: '100%',
                            width: `${goalPct}%`,
                            background: 'linear-gradient(90deg, var(--color-primary), var(--color-primary-soft))',
                            borderRadius: 4,
                            animation: 'bar-rise 1s var(--ease-out) 0.5s both',
                            transformOrigin: 'left',
                        }} />
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 11.5 }}>
                        <span style={{ color: 'rgba(255,255,255,0.45)' }}>{goalPct}% adunat</span>
                        <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--color-primary-soft)', fontWeight: 500 }}>
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><path d="M12 3v6"/><path d="M12 15v6"/><path d="M3 12h6"/><path d="M15 12h6"/></svg>
                            {goal.monthsLeft} luni rămase
            </span>
                    </div>
                </div>
            </div>
        </div>
    )
}