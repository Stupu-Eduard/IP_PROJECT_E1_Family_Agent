import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Camera, MapPin, MapPinOff } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import { fetchExpenses, type ApiExpenseListDto } from '../services/expenses';
import { api } from '../services/api';

interface BudgetSummary {
    totalBudget: number;
    totalSpent: number;
    balance: number;
}

const categoryIcon = (cat: string | null) => {
    const c = (cat || '').toLowerCase();
    // Mâncare & sub
    if (c === 'supermarket') return '🛒';
    if (c === 'restaurant') return '🍽️';
    if (c === 'cafenea') return '☕';
    if (c === 'lactate') return '🥛';
    if (c.includes('fructe') || c.includes('legume')) return '🥦';
    if (c.includes('mâncare') || c.includes('mancare') || c.includes('aliment') || c.includes('food')) return '🛒';
    // Transport & sub
    if (c === 'taxi') return '🚕';
    if (c.includes('transport public')) return '🚌';
    if (c.includes('carburant') || c.includes('benzin') || c.includes('combustibil')) return '⛽';
    if (c === 'parcare') return '🅿️';
    if (c.includes('service auto')) return '🔧';
    if (c === 'rovinieta') return '🛣️';
    if (c.includes('transport')) return '🚗';
    // Sănătate & sub
    if (c.includes('medicamente')) return '💊';
    if (c.includes('consultat')) return '🩺';
    if (c.includes('sanat') || c.includes('medic') || c.includes('health')) return '🏥';
    // Educatie & sub
    if (c.includes('rechizite')) return '✏️';
    if (c.includes('cursuri') || c.includes('curs')) return '🎓';
    if (c.includes('gradinit')) return '🧒';
    if (c.includes('extrascolar')) return '⚽';
    if (c.includes('educa') || c.includes('carte') || c.includes('school') || c.includes('scoala')) return '📚';
    // Divertisment & sub
    if (c.includes('streaming')) return '📺';
    if (c.includes('cinema') || c.includes('film')) return '🎬';
    if (c.includes('divertis') || c.includes('entertain') || c.includes('joc')) return '🎮';
    // Servicii & sub
    if (c.includes('utilit')) return '💡';
    if (c.includes('telefonie') || c.includes('telefon')) return '📞';
    if (c === 'internet') return '🌐';
    if (c.includes('asigurar')) return '🛡️';
    if (c.includes('abonament') || c.includes('servicii') || c.includes('serviciu')) return '📋';
    // Shopping & sub
    if (c.includes('haine') || c.includes('imbracaminte') || c.includes('cloth')) return '👗';
    if (c.includes('electronic')) return '💻';
    if (c.includes('ingrijire personala') || c.includes('cosmetice')) return '🧴';
    if (c.includes('jucarii') || c.includes('jucărie')) return '🧸';
    if (c.includes('carti') || c.includes('carte')) return '📖';
    if (c.includes('shopping') || c.includes('cumpar')) return '🛍️';
    // Numerar & sub
    if (c.includes('bancomat')) return '🏧';
    if (c.includes('numerar') || c.includes('cash')) return '💵';
    // Pentru casa & sub
    if (c.includes('chirie')) return '🏠';
    if (c.includes('curatenie') || c.includes('menaj')) return '🧹';
    if (c.includes('mobila') || c.includes('mobilă')) return '🛋️';
    if (c.includes('reparatii') || c.includes('reparații')) return '🔨';
    if (c.includes('decorat')) return '🎨';
    if (c.includes('pentru casa') || c.includes('locuint')) return '🏠';
    // Fallback
    return '💳';
};

const formatDate = (dateStr: string | null) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const now = new Date();
    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    const time = d.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' });
    if (d.toDateString() === now.toDateString()) return `Astăzi · ${time}`;
    if (d.toDateString() === yesterday.toDateString()) return `Ieri · ${time}`;
    return d.toLocaleDateString('ro-RO', { day: 'numeric', month: 'short' }) + ` · ${time}`;
};

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
    const token = useAuthStore((state) => state.token);
    const [locationStatus, setLocationStatus] = useState<'pending' | 'active' | 'denied'>('pending');
    const [expenses, setExpenses] = useState<ApiExpenseListDto[]>([]);
    const [budget, setBudget] = useState<BudgetSummary | null>(null);
    const [loadingExpenses, setLoadingExpenses] = useState(true);
    const [loadingBudget, setLoadingBudget] = useState(true);

    const { firstName, lastInitial, avatarLetter } = (() => {
        try {
            const payload = JSON.parse(atob(token!.split('.')[1]));
            const parts = (payload.name as string || '').trim().split(' ');
            const first = parts[0] || 'Utilizator';
            const last  = parts.length > 1 ? parts[parts.length - 1] : '';
            return {
                firstName:    first,
                lastInitial:  last ? `${last[0].toUpperCase()}.` : '',
                avatarLetter: first[0].toUpperCase(),
            };
        } catch {
            return { firstName: 'Utilizator', lastInitial: '', avatarLetter: 'U' };
        }
    })();

    const currentMonth = new Date().toLocaleString('ro-RO', { month: 'long' }).toUpperCase();
    const today = new Date();
    const daysLeft = new Date(today.getFullYear(), today.getMonth() + 1, 0).getDate() - today.getDate();

    useEffect(() => {
        fetchExpenses()
            .then(data => setExpenses(data.slice(0, 3)))
            .catch(() => {})
            .finally(() => setLoadingExpenses(false));
    }, []);

    useEffect(() => {
        api.get<BudgetSummary>('/api/v1/budgets/child-summary')
            .then(r => setBudget(r.data))
            .catch(() => setBudget({ totalBudget: 0, totalSpent: 0, balance: 0 }))
            .finally(() => setLoadingBudget(false));
    }, []);

    // ── Logica Real-time Location Sync ──────────────────────────────────────
    useEffect(() => {
        if (!token || !navigator.geolocation) return;

        let watchId: number;

        const sendLocation = (lat: number, lng: number, restricted = false) => {
            let childId = 2;
            let parentId = 1;
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                childId = payload.userId || payload.jti || 2;
                parentId = payload.familyId || 1;
            } catch (e) {
                console.warn("Eroare la extragerea ID-ului din token, folosim ID-ul de test.");
            }

            fetch("http://localhost:8080/api/v1/child/location/sync", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({
                    childId,
                    parentId,
                    latitude: lat,
                    longitude: lng,
                    placeTypes: restricted ? ["bar"] : ["school"]
                })
            }).catch(err => console.error("❌ Eroare sync locație:", err));
        };

        watchId = navigator.geolocation.watchPosition(
            (pos) => {
                setLocationStatus('active');
                sendLocation(pos.coords.latitude, pos.coords.longitude);
            },
            (err) => {
                console.error("🚫 Locație refuzată sau eroare:", err.message);
                setLocationStatus('denied');
                sendLocation(0, 0);
            },
            { enableHighAccuracy: true }
        );

        return () => navigator.geolocation.clearWatch(watchId);
    }, [token]);

    const totalBudget = budget?.totalBudget ?? 0;
    const totalSpent  = budget?.totalSpent  ?? 0;
    const balance     = budget?.balance     ?? 0;
    const spentPct    = totalBudget > 0 ? Math.min(Math.round((totalSpent / totalBudget) * 100), 100) : 0;

    return (
        <div style={{ maxWidth: 960, margin: '0 auto', width: '100%' }}>

            {/* ── Header ──────────────────────────────────────────────────────── */}
            <div className="fade-up" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
                <div style={{ display: 'flex', gap: 8 }}>
                    <div className="chip chip-live">SESIUNE COPIL</div>
                    {locationStatus === 'active' && (
                        <div className="chip" style={{ background: '#E8F5EE', color: '#2E7B4F', border: '1px solid #CDE8D8', display: 'flex', alignItems: 'center', gap: 4 }}>
                            <MapPin size={12} /> GPS Activ
                        </div>
                    )}
                    {locationStatus === 'denied' && (
                        <div className="chip" style={{ background: '#FEECEC', color: '#E24B4A', border: '1px solid #FAD1D1', display: 'flex', alignItems: 'center', gap: 4 }}>
                            <MapPinOff size={12} /> GPS Dezactivat
                        </div>
                    )}
                </div>
                <button
                    className="btn btn-accent"
                    style={{ fontSize: 13 }}
                    onClick={() => navigate('/add-expense')}
                >
                    <Camera size={15} /> Scanează bonul
                </button>
            </div>

            <h1 className="h1 fade-up" style={{ marginBottom: 8 }}>
                Salut, <span style={{ color: 'var(--color-primary)' }}>{firstName}</span> 👋
            </h1>
            <div className="fade-up" style={{ color: 'var(--color-muted)', fontSize: 14, marginBottom: 24, lineHeight: 1.6 }}>
                Ce ai cumpărat astăzi? Scanează bonul și suma se scade din buget.
            </div>

            {/* ── Card sold + avatar ──────────────────────────────────────────── */}
            <div className="card fade-up" style={{ marginBottom: 16, padding: '22px 24px' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 24, alignItems: 'center' }}>

                    {/* Stânga — sold */}
                    <div>
                        <div className="label" style={{ marginBottom: 10 }}>SOLD DISPONIBIL · {currentMonth}</div>
                        <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginBottom: 6 }}>
                            {loadingBudget ? (
                                <div style={{ height: 52, width: 100, background: 'var(--color-surface)', borderRadius: 8, animation: 'pulse 1.5s ease-in-out infinite' }} />
                            ) : (
                                <>
                                    <span style={{ fontSize: 52, fontWeight: 700, color: 'var(--color-ink)', letterSpacing: '-2px', lineHeight: 1 }}>
                                        {Number(balance).toFixed(2)}
                                    </span>
                                    <span style={{ fontSize: 16, fontWeight: 500, color: 'var(--color-muted)' }}>RON</span>
                                </>
                            )}
                        </div>
                        <div style={{ fontSize: 12.5, color: 'var(--color-muted)', marginBottom: 14 }}>
                            {totalBudget > 0
                                ? `din ${Number(totalBudget).toFixed(2)} RON alocați de părinți · resetare în ${daysLeft} zile`
                                : `Niciun buget setat · resetare în ${daysLeft} zile`}
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
                            <span>Cheltuit <strong style={{ color: 'var(--color-ink)' }}>{Number(totalSpent).toFixed(2)} RON</strong></span>
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
                            {avatarLetter}
                        </div>
                        <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--color-ink)' }}>{firstName}{lastInitial ? ` ${lastInitial}` : ''}</div>
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

                    {loadingExpenses ? (
                        <div style={{ padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 12 }}>
                            {[1, 2, 3].map(i => (
                                <div key={i} style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                                    <div style={{ width: 36, height: 36, borderRadius: 10, background: 'var(--color-surface)' }} />
                                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
                                        <div style={{ height: 12, width: '60%', background: 'var(--color-surface)', borderRadius: 4 }} />
                                        <div style={{ height: 10, width: '40%', background: 'var(--color-surface)', borderRadius: 4 }} />
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : expenses.length === 0 ? (
                        <div style={{ padding: '28px 20px', textAlign: 'center', color: 'var(--color-muted)', fontSize: 13 }}>
                            Nicio cheltuială înregistrată
                        </div>
                    ) : (
                        <div className="stagger">
                            {expenses.map((e) => (
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
                                        {categoryIcon(e.category)}
                                    </div>
                                    <div style={{ flex: 1, minWidth: 0 }}>
                                        <div style={{ fontSize: 13.5, fontWeight: 500, color: 'var(--color-ink)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                            {e.description || e.category || 'Cheltuială'}
                                        </div>
                                        <div style={{ fontSize: 11.5, color: 'var(--color-muted)', marginTop: 2 }}>
                                            {e.location?.store ? `${e.location.store} · ` : ''}{formatDate(e.expenseDate)}
                                        </div>
                                    </div>
                                    <div className="row-amount" style={{ fontSize: 14, fontWeight: 500, color: 'var(--color-ink)', whiteSpace: 'nowrap' }}>
                                        {Number(e.amount).toFixed(2)} <span style={{ fontSize: 10, color: 'var(--color-muted-2)', fontWeight: 400 }}>RON</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Obiectivul meu — coming soon */}
                <div className="card fade-up" style={{ background: 'var(--color-ink)', border: 'none', padding: '20px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                    <div>
                        <div style={{ fontSize: 10, fontWeight: 600, letterSpacing: '1.2px', color: 'rgba(255,255,255,0.45)', textTransform: 'uppercase', marginBottom: 12 }}>
                            OBIECTIVUL MEU
                        </div>
                        <div style={{ fontSize: 20, fontWeight: 600, color: '#fff', letterSpacing: '-0.5px', marginBottom: 8 }}>
                            Setează un obiectiv
                        </div>
                        <div style={{ fontSize: 12.5, color: 'rgba(255,255,255,0.55)', lineHeight: 1.5 }}>
                            Economisește din soldul lunar pentru ceva ce îți dorești.
                        </div>
                    </div>
                    <div style={{
                        marginTop: 24, padding: '10px 14px',
                        background: 'rgba(255,255,255,0.08)', borderRadius: 10,
                        fontSize: 12, color: 'rgba(255,255,255,0.4)',
                        display: 'flex', alignItems: 'center', gap: 6,
                    }}>
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><circle cx="12" cy="12" r="10"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>
                        Disponibil în curând
                    </div>
                </div>
            </div>
        </div>
    )
}
