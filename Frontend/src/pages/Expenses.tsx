import { useEffect, useState } from 'react';
import { useExpenseStore } from '../store/expenseStore';
import { useNavigate } from 'react-router-dom';
import { fetchExpenses, deleteExpense, updateExpense } from '../services/expenses';
import { fetchCategoryNames } from '../services/lookups';
import { useAuthStore } from '../store/authStore';
import { decodeJwtPayload } from '../utils/jwt';
import { familyApi } from '../services/api';
import { ChevronDown, MapPin, User, Calendar, ChevronLeft, ChevronRight, Search, Filter, Plus, ArrowLeft, Pencil, Trash2, X } from 'lucide-react';

interface ExpenseListDTO {
    id: number;
    date: string;
    category: string;
    description: string;
    amount: number;
    location: string;
    locationId?: number;
    locationStore?: string;
    locationCity?: string;
    locationCountry?: string;
    lat?: number;
    lng?: number;
    person: string;
    rawDate?: string;
    sourceType?: string;
}

const avatarStyle = (name: string) => {
    if (name.startsWith('E')) return { background: 'linear-gradient(135deg, #C97B4B, #E8A87C)' };
    if (name.startsWith('M')) return { background: 'linear-gradient(135deg, #9A8A7C, #B8A99A)' };
    return { background: 'linear-gradient(135deg, #B5956A, #D4B896)' };
};

const CHILD_CATEGORIES = ['Mâncare', 'Transport', 'Educație', 'Divertisment', 'Sănătate', 'Shopping'];

export default function Expenses() {
    const navigate = useNavigate();
    const token = useAuthStore((s) => s.token);
    const payload = decodeJwtPayload(token ?? '') as any;
    const isChild  = (payload?.role ?? '') === 'Child';
    const familyId: number | null = payload?.familyId ?? null;

    const [currentPage, setCurrentPage] = useState(1);
    const ITEMS_PER_PAGE = 20;

    const [expenses,            setExpenses]            = useState<ExpenseListDTO[]>([]);
    const [isLoading,           setIsLoading]           = useState(true);
    const [loadError,           setLoadError]           = useState<string | null>(null);
    const [availableCategories, setAvailableCategories] = useState<string[]>([]);
    const [availablePeople,     setAvailablePeople]     = useState<string[]>([]);
    const expenseVersion     = useExpenseStore((s) => s.version);
    const notifyExpenseAdded = useExpenseStore((s) => s.notifyExpenseAdded);

    useEffect(() => {
        if (isChild) {
            setAvailableCategories(CHILD_CATEGORIES);
            return;
        }
        const controller = new AbortController();
        const run = async () => {
            try {
                const catsPromise = fetchCategoryNames(controller.signal);
                const peoplePromise = familyId
                    ? familyApi.getMembers(familyId).then(r => r.data.map(m => m.name).filter(Boolean))
                    : Promise.resolve([]);
                const [cats, people] = await Promise.all([catsPromise, peoplePromise]);
                setAvailableCategories((cats ?? []).filter(Boolean));
                setAvailablePeople(people);
            } catch {}
        };
        void run();
        return () => controller.abort();
    }, [familyId]);

    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [selectedCategory, setSelectedCategory] = useState('');
    const [selectedPerson,   setSelectedPerson]   = useState('');

    type EditForm = { amount: string; description: string; category: string; date: string; store: string; city: string };
    const [editingExpense,  setEditingExpense]  = useState<ExpenseListDTO | null>(null);
    const [editForm,        setEditForm]        = useState<EditForm>({ amount: '', description: '', category: '', date: '', store: '', city: '' });
    const [showChildConfirm, setShowChildConfirm] = useState(false);
    const [deletingId,      setDeletingId]      = useState<number | null>(null);
    const [actionError,     setActionError]     = useState<string | null>(null);
    const [isSaving,        setIsSaving]        = useState(false);

    const openEdit = (expense: ExpenseListDTO) => {
        setEditingExpense(expense);
        setEditForm({
            amount:      expense.amount.toString(),
            description: expense.description,
            category:    expense.category === 'Fără categorie' ? '' : expense.category,
            date:        expense.rawDate ?? '',
            store:       expense.locationStore ?? '',
            city:        expense.locationCity  ?? '',
        });
        setShowChildConfirm(false);
        setActionError(null);
    };

    const handleEditSave = () => {
        if (isChild) { setShowChildConfirm(true); } else { void submitEdit(); }
    };

    const submitEdit = async () => {
        if (!editingExpense) return;
        setIsSaving(true);
        try {
            await updateExpense(editingExpense.id, {
                amount:       parseFloat(editForm.amount),
                description:  editForm.description,
                categoryName: editForm.category,
                date:         editForm.date,
                storeName:    editForm.store  || undefined,
                city:         editForm.city   || undefined,
            });
            setEditingExpense(null);
            setShowChildConfirm(false);
            notifyExpenseAdded();
        } catch {
            setActionError('Nu s-a putut salva cheltuiala.');
            setShowChildConfirm(false);
        } finally {
            setIsSaving(false);
        }
    };

    const handleDelete = async () => {
        if (deletingId === null) return;
        setIsSaving(true);
        try {
            await deleteExpense(deletingId);
            setDeletingId(null);
            notifyExpenseAdded();
        } catch {
            setActionError('Nu s-a putut șterge cheltuiala.');
            setDeletingId(null);
        } finally {
            setIsSaving(false);
        }
    };

    useEffect(() => {
        let isCancelled = false;
        const controller = new AbortController();
        const run = async () => {
            setIsLoading(true);
            setLoadError(null);
            try {
                const data = await fetchExpenses(
                    {
                        date: undefined,
                        category: selectedCategory || undefined,
                        person: isChild ? undefined : (selectedPerson || undefined),
                    },
                    controller.signal,
                );
                if (isCancelled) return;
                const mapped: ExpenseListDTO[] = data.map((expense) => {
                    const isoDate  = expense.expenseDate ?? '';
                    const datePart = isoDate ? isoDate.slice(0, 10) : '';
                    const date     = datePart ? datePart.split('-').reverse().join('.') : '';
                    const store    = expense.location?.store    ?? '';
                    const address  = expense.location?.address ?? '';
                    const city     = expense.location?.city    ?? '';
                    const country  = expense.location?.country ?? '';
                    const location = [store, address, city, country].filter(Boolean).join(', ') || 'Fără locație';
                    const amountNumber = typeof expense.amount === 'number' ? expense.amount : Number(expense.amount);
                    return {
                        id: expense.id,
                        date,
                        category:        expense.category    ?? 'Fără categorie',
                        description:     expense.description ?? '',
                        amount:          Number.isFinite(amountNumber) ? amountNumber : 0,
                        location,
                        locationId:      expense.location?.id      ?? undefined,
                        locationStore:   expense.location?.store   ?? undefined,
                        locationCity:    expense.location?.city    ?? undefined,
                        locationCountry: expense.location?.country ?? undefined,
                        lat:             expense.location?.lat     ?? undefined,
                        lng:             expense.location?.lng     ?? undefined,
                        person:          expense.person            ?? 'N/A',
                        rawDate:         datePart,
                        sourceType:      expense.sourceType        ?? 'manual',
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

        return () => {
            isCancelled = true;
            controller.abort();
        };
    }, [selectedCategory, selectedPerson, expenseVersion]);

    useEffect(() => {
        setCurrentPage(1);
    }, [selectedCategory, selectedPerson, startDate, endDate, expenseVersion]);

    const openMap = (expense: ExpenseListDTO) => {
        navigate('/expenses/map', {
            state: {
                lat:             expense.lat,
                lng:             expense.lng,
                locationId:      expense.locationId,
                locationLabel:   expense.location,
                locationCity:    expense.locationCity,
                locationCountry: expense.locationCountry,
                description:     expense.description,
            },
        });
    };

    const filteredExpenses = expenses.filter((e) => {
        if (startDate && e.rawDate && e.rawDate < startDate) return false;
        if (endDate && e.rawDate && e.rawDate > endDate) return false;
        return true;
    });

    const totalPages = Math.max(1, Math.ceil(filteredExpenses.length / ITEMS_PER_PAGE));
    const pagedExpenses = filteredExpenses.slice(
        (currentPage - 1) * ITEMS_PER_PAGE,
        currentPage * ITEMS_PER_PAGE
    );

    const inputStyle = "w-full bg-white border border-[#EDE9E3] rounded-[10px] h-10 px-3 text-[13px] text-[#2D2926] focus:outline-none focus:border-[#C4B9AC] transition-colors appearance-none";

    return (
        <div style={{ maxWidth: 960, margin: '0 auto', width: '100%' }}>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 fade-in-up">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="w-10 h-10 bg-white border border-[#EDE9E3] rounded-[10px] flex items-center justify-center text-[#2D2926] hover:border-[#C4B9AC] transition-colors shadow-sm"
                    >
                        <ArrowLeft size={18} />
                    </button>
                    <h2 className="text-[24px] font-medium text-[#2D2926] tracking-tight">Istoric Cheltuieli</h2>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={() => navigate('/expenses/all-map', { state: { expenses: filteredExpenses, filters: { startDate, endDate, selectedCategory, selectedPerson } } })}
                        className="bg-[#FAF8F5] border border-[#EDE9E3] text-[#2D2926] px-5 py-2.5 rounded-[10px] text-[14px] font-medium flex items-center justify-center gap-2 hover:bg-[#F0ECE7] transition-colors shadow-sm"
                    >
                        <MapPin size={18} />
                        <span>Vezi pe Hartă</span>
                    </button>
                    <button
                        onClick={() => navigate('/add-expense')}
                        className="bg-[#2D2926] text-white px-5 py-2.5 rounded-[10px] text-[14px] font-medium flex items-center justify-center gap-2 hover:opacity-90 transition-opacity shadow-[0_4px_12px_rgba(45,41,38,0.15)]"
                    >
                        <Plus size={18} /><span>Adaugă</span>
                    </button>
                </div>
            </div>

            <div className={`grid grid-cols-1 ${isChild ? 'md:grid-cols-4' : 'md:grid-cols-5'} gap-3 mb-8 fade-in-up`} style={{ animationDelay: '0.1s' }}>
                <div className="relative">
                    <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C]" size={16} />
                    <input
                        type="date"
                        className={`${inputStyle} pl-10`}
                        title="Data de început"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                    />
                </div>
                <div className="relative">
                    <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C]" size={16} />
                    <input
                        type="date"
                        className={`${inputStyle} pl-10`}
                        title="Data de final"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                    />
                </div>
                <div className="relative">
                    <select
                        className={inputStyle}
                        value={selectedCategory}
                        onChange={(e) => setSelectedCategory(e.target.value)}
                    >
                        <option value="">Toate Categoriile</option>
                        {availableCategories.map((c) => (
                            <option key={c} value={c}>
                                {c}
                            </option>
                        ))}
                    </select>
                    <ChevronDown className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C] pointer-events-none" size={16} />
                </div>
                {!isChild && (
                <div className="relative">
                    <select
                        className={inputStyle}
                        value={selectedPerson}
                        onChange={(e) => setSelectedPerson(e.target.value)}
                    >
                        <option value="">Orice Persoană</option>
                        {availablePeople.map((p) => (
                            <option key={p} value={p}>
                                {p}
                            </option>
                        ))}
                    </select>
                    <ChevronDown className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C] pointer-events-none" size={16} />
                </div>
                )}
                <button
                    onClick={() => { setStartDate(''); setEndDate(''); setSelectedCategory(''); setSelectedPerson(''); }}
                    className="bg-white border border-[#EDE9E3] rounded-[10px] px-4 py-2.5 text-[13px] font-medium text-[#2D2926] flex items-center justify-center gap-2 hover:border-[#C4B9AC] transition-colors"
                    title="Resetează filtrele"
                >
                    <Filter size={16} /> Resetează
                </button>
            </div>

            {loadError && (
                <div className="card fade-up" style={{ padding: '14px 18px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10, fontSize: 13, color: 'var(--color-muted)' }}>
                    <Search size={16} style={{ flexShrink: 0 }} /> {loadError}
                </div>
            )}

            {isLoading && (
                <div className="card fade-up" style={{ padding: 0, overflow: 'hidden' }}>
                    {[1, 2, 3, 4].map((i) => (
                        <div key={i} style={{ display: 'grid', gridTemplateColumns: isChild ? '90px 1.4fr 1fr 120px 80px' : '90px 1.4fr 1fr 1fr 120px 80px', padding: '18px 24px', borderBottom: '1px solid var(--color-border)', gap: 16 }}>
                            <div className="skeleton" style={{ height: 14, width: '80%' }} />
                            <div className="skeleton" style={{ height: 14, width: '60%' }} />
                            <div className="skeleton" style={{ height: 14, width: '70%' }} />
                            {!isChild && <div className="skeleton" style={{ height: 14, width: '40%' }} />}
                            <div className="skeleton" style={{ height: 14, width: '50%', marginLeft: 'auto' }} />
                            <div />
                        </div>
                    ))}
                </div>
            )}

            {!isLoading && filteredExpenses.length === 0 && (
                <div className="card fade-up" style={{ padding: 48, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
                    <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'var(--color-surface)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16, color: 'var(--color-muted-3)' }}>
                        <Search size={22} />
                    </div>
                    <div style={{ fontSize: 15, fontWeight: 500, color: 'var(--color-ink)', marginBottom: 6 }}>Nu s-au găsit cheltuieli</div>
                    <div style={{ fontSize: 13, color: 'var(--color-muted)' }}>Nu există nicio înregistrare care să corespundă filtrelor selectate.</div>
                </div>
            )}

            {!isLoading && filteredExpenses.length > 0 && (
                <div className="stagger" style={{ display: 'none' }}>
                    {pagedExpenses.map((expense) => (
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
                                {!isChild && (
                                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                    <User size={13} style={{ color: 'var(--color-muted-4)' }} /> {expense.person}
                                </div>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {!isLoading && filteredExpenses.length > 0 && (
                <div className="card fade-up" style={{ padding: 0, overflow: 'hidden' }}>
                    <div style={{ display: 'grid', gridTemplateColumns: isChild ? '90px 1.4fr 1fr 120px 80px' : '90px 1.4fr 1fr 1fr 120px 80px', padding: '12px 24px', borderBottom: '1px solid var(--color-border)', background: 'var(--color-bg)' }}>
                        {(isChild ? ['DATĂ', 'DESCRIERE', 'LOCAȚIE', 'SUMĂ', ''] : ['DATĂ', 'DESCRIERE', 'LOCAȚIE', 'PERSOANĂ', 'SUMĂ', '']).map((h, i, arr) => (
                            <div key={i} className="label" style={{ textAlign: i === arr.length - 2 ? 'right' : 'left' }}>{h}</div>
                        ))}
                    </div>

                    <div className="stagger">
                        {pagedExpenses.map((expense) => (
                            <div
                                key={expense.id}
                                className="row-clickable fade-up"
                                style={{ display: 'grid', gridTemplateColumns: isChild ? '90px 1.4fr 1fr 120px 80px' : '90px 1.4fr 1fr 1fr 120px 80px', padding: '16px 24px', borderBottom: '1px solid var(--color-border)', alignItems: 'center' }}
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

                                {!isChild && (
                                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                    <div className="avatar avatar-sm" style={avatarStyle(expense.person)}>
                                        {expense.person.charAt(0)}
                                    </div>
                                    <span style={{ fontSize: 12.5, color: 'var(--color-muted)' }}>{expense.person}</span>
                                </div>
                                )}

                                <div className="row-amount" style={{ textAlign: 'right', fontSize: 14.5, fontWeight: 500, color: 'var(--color-ink)' }}>
                                    {expense.amount.toFixed(2)} <span style={{ color: 'var(--color-muted-2)', fontSize: 11, fontWeight: 400 }}>RON</span>
                                </div>

                                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 4 }}>
                                    {(!isChild || expense.sourceType === 'manual') && (
                                        <button
                                            type="button"
                                            title="Editează"
                                            onClick={() => openEdit(expense)}
                                            style={{ width: 30, height: 30, borderRadius: 7, border: '1px solid var(--color-border)', background: 'var(--color-bg)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--color-muted)' }}
                                        >
                                            <Pencil size={13} />
                                        </button>
                                    )}
                                    {!isChild && (
                                        <button
                                            type="button"
                                            title="Șterge"
                                            onClick={() => { setDeletingId(expense.id); setActionError(null); }}
                                            style={{ width: 30, height: 30, borderRadius: 7, border: '1px solid #FDE8E8', background: '#FFF5F5', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#C0392B' }}
                                        >
                                            <Trash2 size={13} />
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>

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
                            {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
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

            {/* Edit modal */}
            {editingExpense && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', zIndex: 50, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
                    <div style={{ background: '#fff', borderRadius: 14, width: '100%', maxWidth: 460, padding: 28, boxShadow: '0 8px 32px rgba(0,0,0,0.18)' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                            <span style={{ fontSize: 16, fontWeight: 600, color: 'var(--color-ink)' }}>Editează cheltuiala</span>
                            <button type="button" onClick={() => setEditingExpense(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--color-muted)' }}><X size={18} /></button>
                        </div>
                        {actionError && <div style={{ marginBottom: 14, fontSize: 12.5, color: '#C0392B', background: '#FFF5F5', border: '1px solid #FDE8E8', borderRadius: 8, padding: '8px 12px' }}>{actionError}</div>}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                            <label style={{ fontSize: 12, color: 'var(--color-muted)', fontWeight: 500 }}>Sumă (RON)
                                <input type="number" min="0.01" step="0.01" value={editForm.amount}
                                    onChange={e => setEditForm(f => ({ ...f, amount: e.target.value }))}
                                    className={inputStyle} style={{ marginTop: 4 }} />
                            </label>
                            <label style={{ fontSize: 12, color: 'var(--color-muted)', fontWeight: 500 }}>Descriere
                                <input type="text" value={editForm.description}
                                    onChange={e => setEditForm(f => ({ ...f, description: e.target.value }))}
                                    className={inputStyle} style={{ marginTop: 4 }} />
                            </label>
                            <label style={{ fontSize: 12, color: 'var(--color-muted)', fontWeight: 500 }}>Categorie
                                <select value={editForm.category} onChange={e => setEditForm(f => ({ ...f, category: e.target.value }))}
                                    className={inputStyle} style={{ marginTop: 4 }}>
                                    <option value="">Selectează categoria</option>
                                    {availableCategories.map(c => <option key={c} value={c}>{c}</option>)}
                                </select>
                            </label>
                            <label style={{ fontSize: 12, color: 'var(--color-muted)', fontWeight: 500 }}>Dată
                                <input type="date" value={editForm.date}
                                    onChange={e => setEditForm(f => ({ ...f, date: e.target.value }))}
                                    className={inputStyle} style={{ marginTop: 4 }} />
                            </label>
                            <label style={{ fontSize: 12, color: 'var(--color-muted)', fontWeight: 500 }}>Magazin (opțional)
                                <input type="text" value={editForm.store}
                                    onChange={e => setEditForm(f => ({ ...f, store: e.target.value }))}
                                    className={inputStyle} style={{ marginTop: 4 }} />
                            </label>
                            <label style={{ fontSize: 12, color: 'var(--color-muted)', fontWeight: 500 }}>Oraș (opțional)
                                <input type="text" value={editForm.city}
                                    onChange={e => setEditForm(f => ({ ...f, city: e.target.value }))}
                                    className={inputStyle} style={{ marginTop: 4 }} />
                            </label>
                        </div>
                        {showChildConfirm ? (
                            <div style={{ marginTop: 20, background: '#FFF8F0', border: '1px solid #F5CBA7', borderRadius: 10, padding: 16 }}>
                                <div style={{ fontSize: 13.5, color: 'var(--color-ink)', marginBottom: 14 }}>Ești sigur că vrei să salvezi modificările?</div>
                                <div style={{ display: 'flex', gap: 8 }}>
                                    <button type="button" onClick={() => setShowChildConfirm(false)}
                                        style={{ flex: 1, height: 36, borderRadius: 8, border: '1px solid var(--color-border)', background: '#fff', fontSize: 13, cursor: 'pointer', color: 'var(--color-ink)' }}>
                                        Înapoi
                                    </button>
                                    <button type="button" onClick={() => void submitEdit()} disabled={isSaving}
                                        style={{ flex: 1, height: 36, borderRadius: 8, border: 'none', background: '#2D2926', color: '#fff', fontSize: 13, cursor: 'pointer', opacity: isSaving ? 0.6 : 1 }}>
                                        {isSaving ? 'Se salvează...' : 'Confirmă'}
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <div style={{ display: 'flex', gap: 8, marginTop: 20 }}>
                                <button type="button" onClick={() => setEditingExpense(null)}
                                    style={{ flex: 1, height: 38, borderRadius: 9, border: '1px solid var(--color-border)', background: '#fff', fontSize: 13.5, cursor: 'pointer', color: 'var(--color-ink)' }}>
                                    Anulează
                                </button>
                                <button type="button" onClick={handleEditSave} disabled={isSaving}
                                    style={{ flex: 1, height: 38, borderRadius: 9, border: 'none', background: '#2D2926', color: '#fff', fontSize: 13.5, cursor: 'pointer', opacity: isSaving ? 0.6 : 1 }}>
                                    {isSaving ? 'Se salvează...' : 'Salvează'}
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* Delete confirmation modal */}
            {deletingId !== null && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', zIndex: 50, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
                    <div style={{ background: '#fff', borderRadius: 14, width: '100%', maxWidth: 380, padding: 28, boxShadow: '0 8px 32px rgba(0,0,0,0.18)' }}>
                        <div style={{ fontSize: 16, fontWeight: 600, color: 'var(--color-ink)', marginBottom: 10 }}>Șterge cheltuiala</div>
                        <div style={{ fontSize: 13.5, color: 'var(--color-muted)', marginBottom: 20 }}>Această acțiune este ireversibilă. Ești sigur că vrei să ștergi cheltuiala?</div>
                        {actionError && <div style={{ marginBottom: 14, fontSize: 12.5, color: '#C0392B', background: '#FFF5F5', border: '1px solid #FDE8E8', borderRadius: 8, padding: '8px 12px' }}>{actionError}</div>}
                        <div style={{ display: 'flex', gap: 8 }}>
                            <button type="button" onClick={() => setDeletingId(null)}
                                style={{ flex: 1, height: 38, borderRadius: 9, border: '1px solid var(--color-border)', background: '#fff', fontSize: 13.5, cursor: 'pointer', color: 'var(--color-ink)' }}>
                                Anulează
                            </button>
                            <button type="button" onClick={() => void handleDelete()} disabled={isSaving}
                                style={{ flex: 1, height: 38, borderRadius: 9, border: 'none', background: '#C0392B', color: '#fff', fontSize: 13.5, cursor: 'pointer', opacity: isSaving ? 0.6 : 1 }}>
                                {isSaving ? 'Se șterge...' : 'Șterge'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}