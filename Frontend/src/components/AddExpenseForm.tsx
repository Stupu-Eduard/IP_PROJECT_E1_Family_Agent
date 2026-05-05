import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { ArrowLeft, PlusCircle } from 'lucide-react';
import { ImageUploader } from './ImageUploader';

export const AddExpenseForm = () => {
    const navigate = useNavigate();
    const logout = useAuthStore((state) => state.logout);
    const [receiptFile, setReceiptFile] = useState<File | null>(null);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (receiptFile) {
            console.log('Obiect File pregătit pentru API OCR:', receiptFile);
        }
    };

    const handleLogout = () => {
        logout();
        navigate('/login', { replace: true });
    };


    return (
        <div className="flex-1 w-full bg-[#FAF8F5] font-sans flex flex-col pb-20">

            {/* Topbar consistent cu restul aplicației */}
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

            {/* Container Formular */}
            <div className="flex-1 flex justify-center py-12 px-6">
                <div className="w-full max-w-lg bg-white border border-[#EDE9E3] rounded-[14px] p-8 shadow-[0_4px_20px_rgba(0,0,0,0.02)] fade-in-up h-fit">

                    {/* Header */}
                    <div className="flex items-center gap-4 mb-8">
                        <button
                            type="button"
                            onClick={() => navigate('/dashboard')}
                            className="w-10 h-10 bg-white border border-[#EDE9E3] rounded-[10px] flex items-center justify-center text-[#2D2926] hover:border-[#C4B9AC] transition-colors shadow-sm shrink-0"
                        >
                            <ArrowLeft size={18} />
                        </button>
                        <div>
                            <h1 className="text-[22px] font-medium text-[#2D2926] tracking-tight mb-1">Nouă Înregistrare</h1>
                            <p className="text-[13px] text-[#9A8A7C]">Completează datele cheltuielii tale</p>
                        </div>
                    </div>

                    <form onSubmit={handleSubmit} className="flex flex-col gap-6">

                        {/* Placeholder pentru restul câmpurilor stilizate */}
                        <div className="space-y-5">
                            <div className="flex flex-col gap-2">
                                <label className="text-[11px] tracking-[1px] text-[#B8A99A] font-medium uppercase">
                                    Detalii Tranzacție
                                </label>
                                <div className="p-4 bg-[#FAF8F5] border border-dashed border-[#EDE9E3] rounded-[10px] text-[13px] text-[#9A8A7C] text-center">
                                    Aici poți insera input-urile tale de Sumă, Categorie și Dată.
                                </div>
                            </div>

                            {/* Zona ImageUploader */}
                            <div className="flex flex-col gap-3">
                                <label className="text-[11px] tracking-[1px] text-[#B8A99A] font-medium uppercase">
                                    Atașament Bon Fiscal
                                </label>
                                <div className="rounded-[10px] overflow-hidden">
                                    <ImageUploader onImageSelect={setReceiptFile} />
                                </div>
                            </div>
                        </div>

                        {/* Buton Submit */}
                        <button
                            type="submit"
                            className="w-full bg-[#2D2926] text-white px-6 py-3.5 rounded-[10px] text-[14px] font-medium flex items-center justify-center gap-2 hover:opacity-95 transition-all active:scale-[0.98] shadow-[0_4px_12px_rgba(45,41,38,0.15)]"
                        >
                            <PlusCircle size={18} />
                            Salvează Cheltuiala
                        </button>
                    </form>

                    {/* Notă sub-formular */}
                    <p className="mt-6 text-center text-[11px] text-[#C4B9AC] leading-relaxed">
                        Informațiile vor fi procesate și salvate securizat în contul familiei tale.
                    </p>
                </div>
            </div>
        </div>
    );
};