import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import {
    ArrowLeft, Search, Filter, Edit2, Trash2, Plus,
    ChevronDown, MapPin, User, Calendar,
    ChevronLeft, ChevronRight
} from 'lucide-react';

interface ExpenseListDTO {
    id: number;
    date: string;
    category: string;
    description: string;
    amount: number;
    location: string;
    person: string;
}

export default function Expenses() {

    const navigate = useNavigate();
    const logout = useAuthStore((state) => state.logout);
    const [searchTerm, setSearchTerm] = useState('');

    const [currentPage, setCurrentPage] = useState(1);
    const totalPages = 2;

    const handleLogout = () => {
        logout();
        navigate('/login', { replace: true });
    };

    const [expenses] = useState<ExpenseListDTO[]>([
        { id: 1, date: '12.04.2026', category: '🍕 Mâncare & Alimente', description: 'Cumpărături Kaufland', amount: 245.50, location: 'Kaufland', person: 'Maria' },
        { id: 2, date: '11.04.2026', category: '📄 Facturi & Utilități', description: 'Factură Energie', amount: 180.00, location: 'Online', person: 'Ion' },
        { id: 3, date: '10.04.2026', category: '🚗 Transport', description: 'Plin Benzină', amount: 320.00, location: 'OMV', person: 'Maria' },
        { id: 4, date: '09.04.2026', category: '🎮 Divertisment', description: 'Abonament Netflix', amount: 60.00, location: 'Online', person: 'Ion' },
    ]);



    // Stil reutilizabil pentru inputuri și select-uri
    const inputStyle = "w-full bg-white border border-[#EDE9E3] rounded-[10px] px-4 py-2.5 text-[13px] text-[#2D2926] placeholder:text-[#C4B9AC] focus:outline-none focus:border-[#C4B9AC] transition-colors appearance-none";

    return (
        <div className="min-h-screen bg-[#FAF8F5] font-sans flex flex-col">

            {/* Topbar consistent cu Dashboard */}
            <nav className="sticky top-0 z-10 bg-[#FAF8F5] border-b border-[#EDE9E3] px-6 lg:px-10 py-4 flex items-center justify-between">
                <div className="flex items-center gap-2.5 cursor-pointer" onClick={() => navigate('/dashboard')}>
                    <div className="w-8 h-8 rounded-[8px] bg-[#2D2926] flex items-center justify-center text-[13px] font-medium text-[#FAF8F5] tracking-tight">FA</div>
                    <span className="text-[15px] font-medium text-[#2D2926] tracking-tight">FamilyAgent</span>
                </div>
                <button
                    type="button"
                    onClick={handleLogout}
                    className="text-[12px] font-medium text-[#8C7E6E] px-3.5 py-1.5 border border-[#E2DDD7] rounded-[20px] bg-white hover:border-[#C4B9AC] hover:text-[#2D2926] transition-colors"
                >
                    Logout
                </button>
            </nav>

            {/* Container Principal */}
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

                {/* Filtre */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mb-8 fade-in-up" style={{ animationDelay: '0.1s' }}>
                    <div className="relative">
                        <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C]" size={16} />
                        <input type="date" className={`${inputStyle} pl-10`} title="Perioadă" />
                    </div>
                    <div className="relative">
                        <select className={inputStyle}>
                            <option value="">Toate Categoriile</option>
                            <option value="mancare">🍕 Mâncare & Alimente</option>
                            <option value="facturi">📄 Facturi & Utilități</option>
                            <option value="transport">🚗 Transport</option>
                            <option value="divertisment">🎮 Divertisment</option>
                        </select>
                        <ChevronDown className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C] pointer-events-none" size={16} />
                    </div>
                    <div className="relative">
                        <select className={inputStyle}>
                            <option value="">Orice Persoană</option>
                            <option value="maria">Maria</option>
                            <option value="ion">Ion</option>
                        </select>
                        <ChevronDown className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C] pointer-events-none" size={16} />
                    </div>
                    <button className="bg-white border border-[#EDE9E3] rounded-[10px] px-4 py-2.5 text-[13px] font-medium text-[#2D2926] flex items-center justify-center gap-2 hover:border-[#C4B9AC] transition-colors">
                        <Filter size={16} /><span>Aplică Filtre</span>
                    </button>
                </div>

                {/* --- Versiune Mobil (Carduri) --- */}
                <div className="md:hidden flex flex-col gap-3 mb-8 fade-in-up" style={{ animationDelay: '0.2s' }}>
                    {expenses.map((expense) => (
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
                                <div className="flex items-center gap-2"><MapPin size={14} className="text-[#D4C9BC]" /> {expense.location}</div>
                                <div className="flex items-center gap-2"><User size={14} className="text-[#D4C9BC]" /> {expense.person}</div>
                            </div>
                        </div>
                    ))}
                </div>

                {/* --- Versiune Desktop (Tabel) --- */}
                <div className="hidden md:block bg-white border border-[#EDE9E3] rounded-[14px] overflow-hidden shadow-[0_4px_20px_rgba(0,0,0,0.02)] fade-in-up" style={{ animationDelay: '0.2s' }}>
                    <table className="w-full text-left border-collapse">
                        <thead>
                        <tr className="bg-[#FAF8F5] border-b border-[#EDE9E3]">
                            <th className="px-6 py-4 text-[11px] font-medium text-[#B8A99A] uppercase tracking-[1px] cursor-pointer hover:text-[#2D2926] transition-colors w-[15%]">
                                <div className="flex items-center gap-1.5">Dată <ChevronDown size={14} /></div>
                            </th>
                            <th className="px-6 py-4 text-[11px] font-medium text-[#B8A99A] uppercase tracking-[1px] w-[25%]">Categorie</th>
                            <th className="px-6 py-4 text-[11px] font-medium text-[#B8A99A] uppercase tracking-[1px] w-[30%]">Descriere & Locație</th>
                            <th className="px-6 py-4 text-[11px] font-medium text-[#B8A99A] uppercase tracking-[1px] w-[15%]">Persoană</th>
                            <th className="px-6 py-4 text-[11px] font-medium text-[#B8A99A] uppercase tracking-[1px] text-right cursor-pointer hover:text-[#2D2926] transition-colors w-[15%]">
                                <div className="flex items-center justify-end gap-1.5">Sumă <ChevronDown size={14} /></div>
                            </th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-[#EDE9E3]">
                        {expenses.map((expense) => (
                            <tr key={expense.id} className="hover:bg-[#FAF8F5]/60 transition-colors group">
                                <td className="px-6 py-4 text-[13px] text-[#9A8A7C] font-medium">{expense.date}</td>
                                <td className="px-6 py-4">
                                        <span className="inline-block px-3 py-1 bg-[#FFF8F2] text-[#7A5C44] rounded-[6px] border border-[#F0DFD0] text-[11px] font-medium">
                                            {expense.category}
                                        </span>
                                </td>
                                <td className="px-6 py-4">
                                    <div className="text-[14px] font-medium text-[#2D2926] mb-0.5">{expense.description}</div>
                                    <div className="text-[12px] text-[#B8A99A] flex items-center gap-1.5 mt-1"><MapPin size={12}/> {expense.location}</div>
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

            </div>
        </div>
    );
}