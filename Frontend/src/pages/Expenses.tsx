import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchExpenses } from '../services/expenses';
import { fetchCategoryNames, fetchUserNames } from '../services/lookups';
import {
    ArrowLeft, Filter, Plus,
    ChevronDown, MapPin, User, Calendar,
    ChevronLeft, ChevronRight, Search
} from 'lucide-react';

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

export default function Expenses() {
    const navigate = useNavigate();

    const [currentPage, setCurrentPage] = useState(1);
    const totalPages = 2;

    const [expenses, setExpenses] = useState<ExpenseListDTO[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);

    const [availableCategories, setAvailableCategories] = useState<string[]>([]);
    const [availablePeople, setAvailablePeople] = useState<string[]>([]);

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
            } catch {
                // ignore (fallback: dropdowns will still work if user types/selects)
            }
        };

        void run();
        return () => controller.abort();
    }, []);

    // ==========================================
    // STĂRILE FILTRELOR
    // ==========================================
    const [selectedDate, setSelectedDate] = useState('');
    const [selectedCategory, setSelectedCategory] = useState('');
    const [selectedPerson, setSelectedPerson] = useState('');

    useEffect(() => {
        let isCancelled = false;
        const controller = new AbortController();

        const run = async () => {
            setIsLoading(true);
            setLoadError(null);

            try {
                const data = await fetchExpenses(
                    {
                        date: selectedDate || undefined,
                        category: selectedCategory || undefined,
                        person: selectedPerson || undefined,
                    },
                    controller.signal,
                );

                if (isCancelled) return;

                const mapped: ExpenseListDTO[] = data.map((expense) => {
                    const isoDate = expense.expenseDate ?? '';
                    const datePart = isoDate ? isoDate.slice(0, 10) : '';
                    const date = datePart ? datePart.split('-').reverse().join('.') : '';

                    const store = expense.location?.store ?? '';
                    const address = expense.location?.address ?? '';
                    const city = expense.location?.city ?? '';
                    const country = expense.location?.country ?? '';
                    const location = [store, address, city, country].filter(Boolean).join(', ') || 'Fără locație';

                    const amountNumber = typeof expense.amount === 'number' ? expense.amount : Number(expense.amount);

                    return {
                        id: expense.id,
                        date,
                        category: expense.category ?? 'Fără categorie',
                        description: expense.description ?? '',
                        amount: Number.isFinite(amountNumber) ? amountNumber : 0,
                        location,
                        locationId: expense.location?.id,
                        locationCity: expense.location?.city ?? undefined,
                        locationCountry: expense.location?.country ?? undefined,
                        lat: expense.location?.lat ?? undefined,
                        lng: expense.location?.lng ?? undefined,
                        person: expense.person ?? 'N/A',
                    };
                });

                setExpenses(mapped);
            } catch (err) {
                if (isCancelled) return;
                setExpenses([]);
                setLoadError('Nu am putut încărca cheltuielile din backend. Verifică VITE_API_BASE_URL și backend-ul pe 8080.');
            } finally {
                if (isCancelled) return;
                setIsLoading(false);
            }
        };

        void run();

        return () => {
            isCancelled = true;
            controller.abort();
        };
    }, [selectedCategory, selectedDate, selectedPerson]);

    const openMap = (expense: ExpenseListDTO) => {
        navigate('/expenses/map', {
            state: {
                lat: expense.lat,
                lng: expense.lng,
                locationId: expense.locationId,
                locationLabel: expense.location,
                locationCity: expense.locationCity,
                locationCountry: expense.locationCountry,
                description: expense.description,
            },
        });
    };

    const filteredExpenses = expenses;


    // ==========================================
    // UI / PREZENTARE
    // ==========================================
    const inputStyle = "w-full bg-white border border-[#EDE9E3] rounded-[10px] px-4 py-2.5 text-[13px] text-[#2D2926] placeholder:text-[#C4B9AC] focus:outline-none focus:border-[#C4B9AC] transition-colors appearance-none";

    return (
        <div className="px-6 lg:px-10 pt-10 pb-20 max-w-[960px] mx-auto w-full flex-1">

                {/* Header: Titlu și Butoane */}
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
                    <button
                        onClick={() => navigate('/add-expense')}
                        className="bg-[#2D2926] text-white px-5 py-2.5 rounded-[10px] text-[14px] font-medium flex items-center justify-center gap-2 hover:opacity-90 transition-opacity shadow-[0_4px_12px_rgba(45,41,38,0.15)]"
                    >
                        <Plus size={18} /><span>Adaugă</span>
                    </button>
                </div>

                {/* Filtre Funcționale */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mb-8 fade-in-up" style={{ animationDelay: '0.1s' }}>
                    <div className="relative">
                        <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C]" size={16} />
                        <input
                            type="date"
                            className={`${inputStyle} pl-10`}
                            title="Perioadă"
                            value={selectedDate}
                            onChange={(e) => setSelectedDate(e.target.value)}
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
                    <button
                        onClick={() => { setSelectedDate(''); setSelectedCategory(''); setSelectedPerson(''); }}
                        className="bg-white border border-[#EDE9E3] rounded-[10px] px-4 py-2.5 text-[13px] font-medium text-[#2D2926] flex items-center justify-center gap-2 hover:border-[#C4B9AC] transition-colors"
                        title="Resetează filtrele"
                    >
                        <Filter size={16} /><span>Resetează Filtre</span>
                    </button>
                </div>

                {loadError && (
                    <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-5 mb-6 shadow-sm text-[13px] text-[#9A8A7C] fade-in-up" style={{ animationDelay: '0.15s' }}>
                        {loadError}
                    </div>
                )}

                {isLoading && (
                    <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-10 flex flex-col items-center justify-center text-center shadow-sm fade-in-up" style={{ animationDelay: '0.2s' }}>
                        <div className="text-[15px] font-medium text-[#2D2926]">Se încarcă cheltuielile…</div>
                        <div className="text-[13px] text-[#9A8A7C] mt-1">Așteaptă puțin.</div>
                    </div>
                )}

                {/* Fallback pentru lipsa rezultatelor */}
                {!isLoading && filteredExpenses.length === 0 && (
                    <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-10 flex flex-col items-center justify-center text-center shadow-sm fade-in-up">
                        <div className="w-16 h-16 bg-[#FAF8F5] rounded-full flex items-center justify-center mb-4 text-[#C4B9AC]">
                            <Search size={24} />
                        </div>
                        <div className="text-[15px] font-medium text-[#2D2926]">Nu s-au găsit cheltuieli</div>
                        <div className="text-[13px] text-[#9A8A7C] mt-1">Nu există nicio înregistrare care să corespundă filtrelor selectate.</div>
                    </div>
                )}

                {/* --- Versiune Mobil (Carduri) --- */}
                {!isLoading && filteredExpenses.length > 0 && (
                    <div className="md:hidden flex flex-col gap-3 mb-8 fade-in-up" style={{ animationDelay: '0.2s' }}>
                        {filteredExpenses.map((expense) => (
                            <div key={expense.id} className="bg-white border border-[#EDE9E3] rounded-[14px] p-5 shadow-[0_4px_20px_rgba(0,0,0,0.02)]">
                                <div className="flex justify-between items-start mb-3">
                                    <div>
                                        <span className="inline-block px-3 py-1 bg-[#FFF8F2] text-[#7A5C44] rounded-[6px] border border-[#F0DFD0] text-[11px] font-medium mb-2.5">
                                            {expense.category}
                                        </span>
                                        <h3 className="text-[15px] font-medium text-[#2D2926] leading-tight">{expense.description}</h3>
                                    </div>
                                    <span className="text-[16px] font-medium text-[#2D2926] tracking-tight whitespace-nowrap">{expense.amount.toFixed(2)} RON</span>
                                </div>
                                <div className="flex flex-col gap-2 text-[12px] text-[#9A8A7C] mt-4 pt-4 border-t border-[#EDE9E3]/50">
                                    <div className="flex items-center gap-2"><Calendar size={14} className="text-[#D4C9BC]" /> {expense.date}</div>
                                    <button
                                        type="button"
                                        onClick={() => openMap(expense)}
                                        className="flex items-center gap-2 text-left hover:text-[#2D2926] transition-colors"
                                    >
                                        <MapPin size={14} className="text-[#D4C9BC]" />
                                        <span className="underline underline-offset-2">{expense.location}</span>
                                    </button>
                                    <div className="flex items-center gap-2"><User size={14} className="text-[#D4C9BC]" /> {expense.person}</div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {/* --- Versiune Desktop (Tabel) --- */}
                {!isLoading && filteredExpenses.length > 0 && (
                    <div className="hidden md:block bg-white border border-[#EDE9E3] rounded-[14px] overflow-hidden shadow-[0_4px_20px_rgba(0,0,0,0.02)] fade-in-up" style={{ animationDelay: '0.2s' }}>
                        <table className="w-full text-left border-collapse">
                            <thead>
                            <tr className="bg-[#FAF8F5] border-b border-[#EDE9E3]">
                                <th className="px-6 py-4 text-[11px] font-medium text-[#B8A99A] uppercase tracking-[1px] w-[15%]">
                                    <div className="flex items-center gap-1.5">Dată</div>
                                </th>
                                <th className="px-6 py-4 text-[11px] font-medium text-[#B8A99A] uppercase tracking-[1px] w-[25%]">Categorie</th>
                                <th className="px-6 py-4 text-[11px] font-medium text-[#B8A99A] uppercase tracking-[1px] w-[30%]">Descriere & Locație</th>
                                <th className="px-6 py-4 text-[11px] font-medium text-[#B8A99A] uppercase tracking-[1px] w-[15%]">Persoană</th>
                                <th className="px-6 py-4 text-[11px] font-medium text-[#B8A99A] uppercase tracking-[1px] text-right w-[15%]">
                                    <div className="flex items-center justify-end gap-1.5">Sumă</div>
                                </th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-[#EDE9E3]">
                            {filteredExpenses.map((expense) => (
                                <tr key={expense.id} className="hover:bg-[#FAF8F5]/60 transition-colors group">
                                    <td className="px-6 py-4 text-[13px] text-[#9A8A7C] font-medium">{expense.date}</td>
                                    <td className="px-6 py-4">
                                            <span className="inline-block px-3 py-1 bg-[#FFF8F2] text-[#7A5C44] rounded-[6px] border border-[#F0DFD0] text-[11px] font-medium">
                                                {expense.category}
                                            </span>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="text-[14px] font-medium text-[#2D2926] mb-0.5">{expense.description}</div>
                                        <button
                                            type="button"
                                            onClick={() => openMap(expense)}
                                            className="text-[12px] text-[#B8A99A] flex items-center gap-1.5 mt-1 hover:text-[#2D2926] transition-colors"
                                        >
                                            <MapPin size={12} />
                                            <span className="underline underline-offset-2">{expense.location}</span>
                                        </button>
                                    </td>
                                    <td className="px-6 py-4 text-[13px] text-[#9A8A7C] flex items-center gap-2"><User size={14} className="text-[#D4C9BC]"/> {expense.person}</td>
                                    <td className="px-6 py-4 text-[15px] font-medium text-right text-[#2D2926]">{expense.amount.toFixed(2)} RON</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>

                        {/* Paginare Design Nou */}
                        <div className="bg-white px-6 py-4 border-t border-[#EDE9E3] flex items-center justify-between">
                            <span className="text-[13px] text-[#9A8A7C]">
                                Pagina <span className="font-medium text-[#2D2926]">{currentPage}</span> din {totalPages}
                            </span>
                            <div className="flex gap-2">
                                <button
                                    onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                                    disabled={currentPage === 1}
                                    className={`w-8 h-8 rounded-[8px] flex items-center justify-center transition-colors ${
                                        currentPage === 1
                                            ? 'border border-[#EDE9E3] text-[#D4C9BC] bg-[#FAF8F5] cursor-not-allowed'
                                            : 'border border-[#EDE9E3] text-[#2D2926] bg-white hover:border-[#C4B9AC]'
                                    }`}
                                >
                                    <ChevronLeft size={16} />
                                </button>

                                <button
                                    onClick={() => setCurrentPage(1)}
                                    className={`w-8 h-8 rounded-[8px] text-[13px] font-medium transition-colors ${
                                        currentPage === 1
                                            ? 'bg-[#2D2926] text-white border border-[#2D2926]'
                                            : 'border border-[#EDE9E3] text-[#9A8A7C] bg-white hover:border-[#C4B9AC] hover:text-[#2D2926]'
                                    }`}
                                >
                                    1
                                </button>

                                <button
                                    onClick={() => setCurrentPage(2)}
                                    className={`w-8 h-8 rounded-[8px] text-[13px] font-medium transition-colors ${
                                        currentPage === 2
                                            ? 'bg-[#2D2926] text-white border border-[#2D2926]'
                                            : 'border border-[#EDE9E3] text-[#9A8A7C] bg-white hover:border-[#C4B9AC] hover:text-[#2D2926]'
                                    }`}
                                >
                                    2
                                </button>

                                <button
                                    onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                                    disabled={currentPage === totalPages}
                                    className={`w-8 h-8 rounded-[8px] flex items-center justify-center transition-colors ${
                                        currentPage === totalPages
                                            ? 'border border-[#EDE9E3] text-[#D4C9BC] bg-[#FAF8F5] cursor-not-allowed'
                                            : 'border border-[#EDE9E3] text-[#2D2926] bg-white hover:border-[#C4B9AC]'
                                    }`}
                                >
                                    <ChevronRight size={16} />
                                </button>
                            </div>
                        </div>
                    </div>
                )}

        </div>
    );
}