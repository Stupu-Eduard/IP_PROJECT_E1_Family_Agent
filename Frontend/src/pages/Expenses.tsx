import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchExpenses } from '../services/expenses';
import { fetchCategoryNames, fetchUserNames } from '../services/lookups';
import { ChevronDown, MapPin, User, Calendar, ChevronLeft, ChevronRight, Search, Filter, Plus, ArrowLeft } from 'lucide-react';

interface ExpenseListDTO {
    id: number;
    date: string;
    category: string;
    description: string;
    amount: number;
    location: string;
    locationId?: number;
    locationCity?: string;
    locationCountry?: string;
    lat?: number;
    lng?: number;
    person: string;
}

// Avatar color per persoană
const avatarStyle = (name: string) => {
    if (name.startsWith('E')) return { background: 'linear-gradient(135deg, #C97B4B, #E8A87C)' };
    if (name.startsWith('M')) return { background: 'linear-gradient(135deg, #9A8A7C, #B8A99A)' };
    return { background: 'linear-gradient(135deg, #B5956A, #D4B896)' };
};

export default function Expenses() {
    const navigate = useNavigate();

    // ── Paginare (NEATINS) ─────────────────────────────────────────────────
    const [currentPage, setCurrentPage] = useState(1);
    const totalPages = 2;

    // ── State date (NEATINS) ───────────────────────────────────────────────
    const [expenses,            setExpenses]            = useState<ExpenseListDTO[]>([]);
    const [isLoading,           setIsLoading]           = useState(true);
    const [loadError,           setLoadError]           = useState<string | null>(null);
    const [availableCategories, setAvailableCategories] = useState<string[]>([]);
    const [availablePeople,     setAvailablePeople]     = useState<string[]>([]);

    // ── Fetch categorii și persoane (NEATINS) ──────────────────────────────
    useEffect(() => {
        const controller = new AbortController();
        const run = async () => {
            try {
                const [cats, people] = await Promise.all([
                    fetchCategoryNames(controller.signal),
                    fetchUserNames(controller.signal),
                ]);
                setAvailableCategories((cats ?? []).filter(Boolean));
                setAvailablePeople((people ?? []).filter(Boolean));
            } catch {}
        };
        void run();
        return () => controller.abort();
    }, []);

    // ── Filtre (NEATINS) ───────────────────────────────────────────────────
    const [selectedDate,     setSelectedDate]     = useState('');
    const [selectedCategory, setSelectedCategory] = useState('');
    const [selectedPerson,   setSelectedPerson]   = useState('');

    // ── Fetch cheltuieli (NEATINS) ─────────────────────────────────────────
    useEffect(() => {
        let isCancelled = false;
        const controller = new AbortController();
        const run = async () => {
            setIsLoading(true);
            setLoadError(null);
            try {
                const data = await fetchExpenses(
                    {
                        date:     selectedDate     || undefined,
                        category: selectedCategory || undefined,
                        person:   selectedPerson   || undefined,
                    },
                    controller.signal,
                );
                if (isCancelled) return;
                const mapped: ExpenseListDTO[] = data.map((expense) => {
                    const isoDate  = expense.expenseDate ?? '';
                    const datePart = isoDate ? isoDate.slice(0, 10) : '';
                    const date     = datePart ? datePart.split('-').reverse().join('.') : '';
                    const store    = expense.location?.store   ?? '';
                    const address  = expense.location?.address ?? '';
                    const city     = expense.location?.city    ?? '';
                    const country  = expense.location?.country ?? '';
                    const location = [store, address, city, country].filter(Boolean).join(', ') || 'Fără locație';
                    const amountNumber = typeof expense.amount === 'number' ? expense.amount : Number(expense.amount);
                    return {
                        id: expense.id,
                        date,
                        category:      expense.category    ?? 'Fără categorie',
                        description:   expense.description ?? '',
                        amount:        Number.isFinite(amountNumber) ? amountNumber : 0,
                        location,
                        locationId:      expense.location?.id      ?? undefined,
                        locationCity:    expense.location?.city    ?? undefined,
                        locationCountry: expense.location?.country ?? undefined,
                        lat:             expense.location?.lat     ?? undefined,
                        lng:             expense.location?.lng     ?? undefined,
                        person:          expense.person            ?? 'N/A',
                    };
                });
                setExpenses(mapped);
            } catch (err) {
                if (isCancelled) return;
                setExpenses([]);
                setLoadError('Nu am putut încărca cheltuielile din backend. Verifică VITE_API_BASE_URL și backend-ul pe 8080.');
            } finally {
                if (!isCancelled) setIsLoading(false);
            }
        };
        void run();
        return () => { isCancelled = true; controller.abort(); };
    }, [selectedCategory, selectedDate, selectedPerson]);

    // ── openMap (NEATINS) ──────────────────────────────────────────────────
    const openMap = (expense: ExpenseListDTO) => {
        navigate('/expenses/map', {
            state: {
                lat:           expense.lat,
                lng:           expense.lng,
                locationId:    expense.locationId,
                locationLabel: expense.location,
                locationCity:  expense.locationCity,
                locationCountry: expense.locationCountry,
                description:   expense.description,
            },
        });
    };

    const filteredExpenses = expenses;

    return (
        <div style={{ maxWidth: 960, margin: '0 auto', width: '100%' }}>

            {/* ── Header ──────────────────────────────────────────────────── */}
            <div className="fade-up" style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 8 }}>
                <button className="btn btn-ghost btn-icon" onClick={() => navigate('/dashboard')}>
                    <ArrowLeft size={16} />
                </button>
                <div className="chip chip-live">{filteredExpenses.length > 0 ? `${filteredExpenses.length} TRANZACȚII` : 'CHELTUIELI'}</div>
            </div>
            <h1 className="h1 fade-up" style={{ marginBottom: 8 }}>Istoric cheltuieli</h1>
            <div className="fade-up" style={{ color: 'var(--color-muted)', fontSize: 14, marginBottom: 24, lineHeight: 1.6 }}>
                Caută, filtrează și analizează tot ce a cheltuit familia ta.
            </div>

            {/* ── Filtre (logică neatinsă) ─────────────────────────────── */}
            <div className="card fade-up" style={{ padding: 16, marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>

                {/* Dată */}
                <div style={{ position: 'relative' }}>
                    <Calendar size={14} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--color-muted)', pointerEvents: 'none' }} />
                    <input
                        type="date"
                        className="input"
                        style={{ paddingLeft: 34, width: 160, fontSize: 13 }}
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                    />
                </div>

                {/* Categorie */}
                <div style={{ position: 'relative' }}>
                    <select
                        className="input"
                        style={{ width: 170, fontSize: 13, appearance: 'none', paddingRight: 32 }}
                        value={selectedCategory}
                        onChange={(e) => setSelectedCategory(e.target.value)}
                    >
                        <option value="">Toate Categoriile</option>
                        {availableCategories.map((c) => <option key={c} value={c}>{c}</option>)}
                    </select>
                    <ChevronDown size={14} style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--color-muted)', pointerEvents: 'none' }} />
                </div>

                {/* Persoană */}
                <div style={{ position: 'relative' }}>
                    <select
                        className="input"
                        style={{ width: 160, fontSize: 13, appearance: 'none', paddingRight: 32 }}
                        value={selectedPerson}
                        onChange={(e) => setSelectedPerson(e.target.value)}
                    >
                        <option value="">Orice Persoană</option>
                        {availablePeople.map((p) => <option key={p} value={p}>{p}</option>)}
                    </select>
                    <ChevronDown size={14} style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--color-muted)', pointerEvents: 'none' }} />
                </div>

                {/* Reset */}
                <button
                    className="btn btn-ghost"
                    style={{ fontSize: 13, padding: '10px 14px' }}
                    onClick={() => { setSelectedDate(''); setSelectedCategory(''); setSelectedPerson(''); }}
                >
                    <Filter size={14} /> Resetează
                </button>

                {/* Adaugă */}
                <button
                    className="btn btn-primary"
                    style={{ marginLeft: 'auto', fontSize: 13 }}
                    onClick={() => navigate('/add-expense')}
                >
                    <Plus size={15} /> Adaugă
                </button>
            </div>

            {/* ── Eroare backend (NEATINS) ─────────────────────────────── */}
            {loadError && (
                <div className="card fade-up" style={{ padding: '14px 18px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10, fontSize: 13, color: 'var(--color-muted)' }}>
                    <Search size={16} style={{ flexShrink: 0 }} /> {loadError}
                </div>
            )}

            {/* ── Loading skeleton (NEATINS — isLoading) ──────────────── */}
            {isLoading && (
                <div className="card fade-up" style={{ padding: 0, overflow: 'hidden' }}>
                    {[1, 2, 3, 4].map((i) => (
                        <div key={i} style={{ display: 'grid', gridTemplateColumns: '90px 1.4fr 1fr 1fr 130px', padding: '18px 24px', borderBottom: '1px solid var(--color-border)', gap: 16 }}>
                            <div className="skeleton" style={{ height: 14, width: '80%' }} />
                            <div className="skeleton" style={{ height: 14, width: '60%' }} />
                            <div className="skeleton" style={{ height: 14, width: '70%' }} />
                            <div className="skeleton" style={{ height: 14, width: '40%' }} />
                            <div className="skeleton" style={{ height: 14, width: '50%', marginLeft: 'auto' }} />
                        </div>
                    ))}
                </div>
            )}

            {/* ── Empty state (NEATINS — filteredExpenses.length === 0) ── */}
            {!isLoading && filteredExpenses.length === 0 && (
                <div className="card fade-up" style={{ padding: 48, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
                    <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'var(--color-surface)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16, color: 'var(--color-muted-3)' }}>
                        <Search size={22} />
                    </div>
                    <div style={{ fontSize: 15, fontWeight: 500, color: 'var(--color-ink)', marginBottom: 6 }}>Nu s-au găsit cheltuieli</div>
                    <div style={{ fontSize: 13, color: 'var(--color-muted)' }}>Nu există nicio înregistrare care să corespundă filtrelor selectate.</div>
                </div>
            )}

            {/* ── Carduri mobile (NEATINS — logică openMap) ───────────── */}
            {!isLoading && filteredExpenses.length > 0 && (
                <div className="stagger" style={{ display: 'none' /* md:block e gestionat de tabel */ }}>
                    {filteredExpenses.map((expense) => (
                        <div key={expense.id} className="card card-hover fade-up" style={{ marginBottom: 10 }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                                <div>
                                    <span className="chip" style={{ fontSize: 10.5, padding: '2px 8px', marginBottom: 8 }}>{expense.category}</span>
                                    <div style={{ fontSize: 15, fontWeight: 500, color: 'var(--color-ink)' }}>{expense.description}</div>
                                </div>
                                <div className="row-amount" style={{ fontSize: 16, fontWeight: 500, color: 'var(--color-ink)', whiteSpace: 'nowrap' }}>
                                    {expense.amount.toFixed(2)} <span style={{ fontSize: 11, color: 'var(--color-muted-2)', fontWeight: 400 }}>RON</span>
                                </div>
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 6, fontSize: 12, color: 'var(--color-muted)', paddingTop: 12, borderTop: '1px solid var(--color-border)' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                    <Calendar size={13} style={{ color: 'var(--color-muted-4)' }} /> {expense.date}
                                </div>
                                <button
                                    type="button"
                                    onClick={() => openMap(expense)}
                                    style={{ display: 'flex', alignItems: 'center', gap: 6, background: 'none', border: 'none', cursor: 'pointer', color: 'var(--color-muted)', fontSize: 12, textAlign: 'left' }}
                                >
                                    <MapPin size={13} style={{ color: 'var(--color-muted-4)' }} />
                                    <span style={{ textDecoration: 'underline', textUnderlineOffset: 2 }}>{expense.location}</span>
                                </button>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                    <User size={13} style={{ color: 'var(--color-muted-4)' }} /> {expense.person}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* ── Tabel desktop (NEATINS — logică openMap, paginare) ──── */}
            {!isLoading && filteredExpenses.length > 0 && (
                <div className="card fade-up" style={{ padding: 0, overflow: 'hidden' }}>

                    {/* Header tabel */}
                    <div style={{ display: 'grid', gridTemplateColumns: '90px 1.4fr 1fr 1fr 130px', padding: '12px 24px', borderBottom: '1px solid var(--color-border)', background: 'var(--color-bg)' }}>
                        {['DATĂ', 'DESCRIERE', 'LOCAȚIE', 'PERSOANĂ', 'SUMĂ'].map((h, i) => (
                            <div key={h} className="label" style={{ textAlign: i === 4 ? 'right' : 'left' }}>{h}</div>
                        ))}
                    </div>

                    {/* Rânduri (logică openMap neatinsă) */}
                    <div className="stagger">
                        {filteredExpenses.map((expense) => (
                            <div
                                key={expense.id}
                                className="row-clickable fade-up"
                                style={{ display: 'grid', gridTemplateColumns: '90px 1.4fr 1fr 1fr 130px', padding: '16px 24px', borderBottom: '1px solid var(--color-border)', alignItems: 'center' }}
                            >
                                <div style={{ fontSize: 12.5, color: 'var(--color-muted)', fontWeight: 500 }}>{expense.date}</div>

                                <div>
                                    <span className="chip" style={{ fontSize: 10.5, padding: '2px 8px', marginBottom: 6 }}>{expense.category}</span>
                                    <div style={{ fontSize: 13.5, fontWeight: 500, color: 'var(--color-ink)', marginTop: 4 }}>{expense.description}</div>
                                </div>

                                <div>
                                    <button
                                        type="button"
                                        onClick={() => openMap(expense)}
                                        style={{ display: 'flex', alignItems: 'center', gap: 6, background: 'none', border: 'none', cursor: 'pointer', color: 'var(--color-muted)', fontSize: 12.5 }}
                                    >
                                        <MapPin size={13} style={{ color: 'var(--color-muted-3)', flexShrink: 0 }} />
                                        <span style={{ textDecoration: 'underline', textUnderlineOffset: 2 }}>{expense.location}</span>
                                    </button>
                                </div>

                                {/* Avatar + nume persoană */}
                                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                    <div className="avatar avatar-sm" style={avatarStyle(expense.person)}>
                                        {expense.person.charAt(0)}
                                    </div>
                                    <span style={{ fontSize: 12.5, color: 'var(--color-muted)' }}>{expense.person}</span>
                                </div>

                                <div className="row-amount" style={{ textAlign: 'right', fontSize: 14.5, fontWeight: 500, color: 'var(--color-ink)' }}>
                                    {expense.amount.toFixed(2)} <span style={{ color: 'var(--color-muted-2)', fontSize: 11, fontWeight: 400 }}>RON</span>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* ── Paginare (NEATINS — setCurrentPage, currentPage, totalPages) */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 24px', borderTop: '1px solid var(--color-border)' }}>
                        <div style={{ fontSize: 12, color: 'var(--color-muted)' }}>
                            Pagina <strong style={{ color: 'var(--color-ink)' }}>{currentPage}</strong> din {totalPages}
                        </div>
                        <div style={{ display: 'flex', gap: 6 }}>
                            <button
                                onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                                disabled={currentPage === 1}
                                className="btn btn-ghost btn-icon"
                                style={{ width: 34, height: 34, opacity: currentPage === 1 ? 0.4 : 1 }}
                            >
                                <ChevronLeft size={15} />
                            </button>
                            {[1, 2].map((n) => (
                                <button
                                    key={n}
                                    onClick={() => setCurrentPage(n)}
                                    className={n === currentPage ? 'btn btn-primary' : 'btn btn-ghost'}
                                    style={{ width: 34, height: 34, padding: 0, fontSize: 13 }}
                                >
                                    {n}
                                </button>
                            ))}
                            <button
                                onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                                disabled={currentPage === totalPages}
                                className="btn btn-ghost btn-icon"
                                style={{ width: 34, height: 34, opacity: currentPage === totalPages ? 0.4 : 1 }}
                            >
                                <ChevronRight size={15} />
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}