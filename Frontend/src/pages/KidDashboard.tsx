import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { Camera, LogOut, Wallet } from 'lucide-react';

export default function KidDashboard() {
    const navigate = useNavigate();
    const logout = useAuthStore((state) => state.logout);

    const handleLogout = () => {
        logout();
        navigate('/login', { replace: true });
    };

    return (
        <div className="min-h-screen bg-[#FFFBF7] font-sans flex flex-col relative overflow-hidden">
            {/* Decorațiuni Fundal */}
            <div className="absolute top-[-50px] right-[-50px] w-40 h-40 bg-[#FDEFE6] rounded-full blur-3xl opacity-60 pointer-events-none"></div>
            <div className="absolute bottom-[-50px] left-[-50px] w-60 h-60 bg-[#F4F0EB] rounded-full blur-3xl opacity-60 pointer-events-none"></div>

            {/* Topbar */}
            <nav className="relative z-10 px-6 pt-6 pb-2 flex items-center justify-between">
                <div className="w-10 h-10 rounded-full bg-[#C97B4B] flex items-center justify-center text-white font-bold text-[18px] shadow-sm">
                    A
                </div>
                <button
                    onClick={handleLogout}
                    className="flex items-center gap-2 text-[13px] font-bold text-[#9A8A7C] hover:text-[#C97B4B] transition-colors bg-white px-4 py-2 rounded-[20px] shadow-sm"
                >
                    <LogOut size={16} /> Ieși
                </button>
            </nav>

            <div className="relative z-10 px-6 pt-6 pb-20 max-w-[500px] mx-auto w-full flex-1 flex flex-col">

                {/* Header Corectat */}
                <div className="mb-8 text-center fade-in-up">
                    <div className="inline-block bg-white p-3 rounded-[20px] shadow-sm mb-4 rotate-3 hover:rotate-0 transition-transform">
                        <span className="text-[32px]">🛍️</span>
                    </div>
                    <h1 className="text-[28px] font-bold text-[#2D2926] tracking-tight">Salut, Andrei!</h1>
                    <p className="text-[15px] text-[#9A8A7C] mt-1 font-medium">Ce ți-ai mai cumpărat astăzi?</p>
                </div>

                {/* Cardul Soldului Disponibil */}
                <div className="bg-white border-4 border-[#F4F0EB] rounded-[30px] p-8 mb-8 shadow-sm text-center fade-in-up transition-transform hover:-translate-y-1">
                    <div className="flex items-center justify-center gap-2 mb-2">
                        <Wallet className="text-[#C97B4B]" size={20} />
                        <span className="text-[14px] font-bold text-[#C97B4B] uppercase tracking-wider">Sold Disponibil</span>
                    </div>
                    <div className="text-[56px] font-black text-[#2D2926] leading-none mb-1">
                        45<span className="text-[24px] text-[#9A8A7C] ml-1">lei</span>
                    </div>
                    <div className="text-[13px] text-[#8C7E6E] bg-[#FAF8F5] inline-block px-4 py-1.5 rounded-full mt-3 font-medium">
                        Din bugetul alocat luna aceasta
                    </div>
                </div>

                {/* Butonul OCR Principal */}
                <button
                    onClick={() => navigate('/scan-receipt')}
                    className="w-full bg-[#2D2926] text-white py-5 rounded-[24px] text-[18px] font-bold shadow-[0_8px_20px_rgba(45,41,38,0.2)] hover:bg-[#1A1816] active:scale-[0.98] transition-all flex items-center justify-center gap-3 fade-in-up mb-8"
                >
                    <div className="bg-white/20 p-2 rounded-full">
                        <Camera size={22} className="text-white" />
                    </div>
                    Scanează Bonul
                </button>

                {/* Navigare Funcțională către Grup */}
                <button
                    onClick={() => navigate('/family')}
                    type="button"
                    className="w-full bg-white border border-[#EDE9E3] rounded-[24px] p-5 flex items-center justify-between cursor-pointer hover:border-[#C4B9AC] transition-colors fade-in-up shadow-sm text-left"
                >
                    <div className="flex items-center gap-4">
                        <div className="text-[24px] bg-[#F4F0EB] w-12 h-12 flex items-center justify-center rounded-full">👨‍👩‍👦</div>
                        <div>
                            <h3 className="text-[16px] font-bold text-[#2D2926]">Grupul Familiei</h3>
                            <p className="text-[13px] text-[#9A8A7C] font-medium">Vezi cine mai este în grup</p>
                        </div>
                    </div>
                    <div className="text-[#C4B9AC] font-bold text-[20px]">→</div>
                </button>

            </div>
        </div>
    );
}