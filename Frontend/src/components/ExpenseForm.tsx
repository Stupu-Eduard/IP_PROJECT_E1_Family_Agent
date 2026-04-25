import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { ArrowLeft, Loader2, AlertCircle } from 'lucide-react';
import type { ExpenseDTO } from '../types/ExpenseDTO';
import { processReceiptOCR } from '../services/expenses';
import { ImageUploader } from './ImageUploader';

const ExpenseForm: React.FC = () => {
  const navigate = useNavigate();
  const logout = useAuthStore((state) => state.logout);

  // Stări Formular
  const [amount, setAmount] = useState<number | ''>('');
  const [category, setCategory] = useState<string>('');
  const [date, setDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [receiptFile, setReceiptFile] = useState<File | null>(null);

  // Stări Procesare (Salvare și OCR)
  const [loading, setLoading] = useState<boolean>(false);
  const [isAnalyzing, setIsAnalyzing] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [ocrError, setOcrError] = useState<string | null>(null);
  const [success, setSuccess] = useState<boolean>(false);

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const handleOcrProcess = async (file: File) => {
    setIsAnalyzing(true);
    setOcrError(null);
    setError('');

    try {
      const data = await processReceiptOCR(file);

      // Populare automată a câmpurilor din răspunsul OCR
      if (data.amount) setAmount(data.amount);
      if (data.category) setCategory(data.category);

      if (data.date) {
        // Conversie din ISO sau format extins în YYYY-MM-DD pentru input date
        const formattedDate = data.date.includes('T') ? data.date.split('T')[0] : data.date;
        setDate(formattedDate);
      }
    } catch (err) {
      console.error("Eroare OCR:", err);
      setOcrError("Nu am putut citi automat toate datele. Te rugăm să le completezi manual.");
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);

    if (Number(amount) <= 0) {
      setError('Suma trebuie să fie strict mai mare ca 0!');
      return;
    }

    setLoading(true);

    const payload: ExpenseDTO = {
      amount: Number(amount),
      category,
      date,
    };

    // Simulare API Call pentru salvarea cheltuielii
    setTimeout(() => {
      setLoading(false);
      setSuccess(true);

      // Resetare formular după succes
      setAmount('');
      setCategory('');
      setDate(new Date().toISOString().split('T')[0]);
      setReceiptFile(null);
      setOcrError(null);

      setTimeout(() => setSuccess(false), 3000);
    }, 1000);
  };

  const isInputDisabled = loading || isAnalyzing;
  const inputClasses = `w-full bg-white border border-[#EDE9E3] rounded-[10px] px-4 py-3 text-[14px] text-[#2D2926] placeholder:text-[#C4B9AC] focus:outline-none focus:border-[#C4B9AC] transition-colors ${isInputDisabled ? 'opacity-60 cursor-not-allowed' : ''}`;

  return (
      <div className="flex-1 w-full bg-[#FAF8F5] font-sans flex flex-col">
        {/* Topbar Navigation */}
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

        {/* Form Container */}
        <div className="flex-1 flex justify-center py-12 px-6">
          <div className="relative w-full max-w-md bg-white border border-[#EDE9E3] rounded-[14px] p-8 shadow-[0_4px_20px_rgba(0,0,0,0.02)] fade-in-up h-fit overflow-hidden">

            {/* Overlay de analiză OCR (Spinner & Blur) */}
            {isAnalyzing && (
                <div className="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-20 flex flex-col items-center justify-center rounded-[14px] animate-in fade-in duration-300">
                  <div className="flex flex-col items-center gap-3">
                    <div className="relative">
                      <div className="w-12 h-12 border-4 border-[#F4F0EB] border-t-[#C97B4B] rounded-full animate-spin"></div>
                      <Loader2 className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-[#C97B4B]/30 w-5 h-5" />
                    </div>
                    <div className="text-center">
                      <p className="text-[14px] font-bold text-[#2D2926]">Analizăm bonul...</p>
                      <p className="text-[11px] text-[#9A8A7C] font-medium">FamilyAgent citește datele</p>
                    </div>
                  </div>
                </div>
            )}

            {/* Header Section */}
            <div className="flex items-center gap-4 mb-8">
              <button
                  onClick={() => navigate('/dashboard')}
                  className="w-10 h-10 bg-white border border-[#EDE9E3] rounded-[10px] flex items-center justify-center text-[#2D2926] hover:border-[#C4B9AC] transition-colors shadow-sm shrink-0"
              >
                <ArrowLeft size={18} />
              </button>
              <div>
                <h1 className="text-[22px] font-medium text-[#2D2926] tracking-tight mb-1">Adaugă Cheltuială</h1>
                <p className="text-[13px] text-[#9A8A7C]">Înregistrează cheltuielile tale ușor și rapid</p>
              </div>
            </div>

            {/* Notificări de stare */}
            {success && (
                <div className="bg-[#FFF8F2] border border-[#F0DFD0] text-[#7A5C44] px-4 py-3 rounded-[10px] text-[13px] font-medium mb-6 flex items-center gap-2">
                  <span>✓</span> Cheltuială adăugată cu succes!
                </div>
            )}

            {error && (
                <div className="bg-red-50 border border-red-100 text-red-600 px-4 py-3 rounded-[10px] text-[13px] font-medium mb-6 flex items-center gap-2">
                  <span>✗</span> {error}
                </div>
            )}

            {ocrError && (
                <div className="bg-amber-50 border border-amber-100 text-amber-700 px-4 py-3 rounded-[10px] text-[12px] font-medium mb-6 flex items-start gap-2 animate-in slide-in-from-top-1">
                  <AlertCircle size={16} className="shrink-0 mt-0.5" />
                  <span>{ocrError}</span>
                </div>
            )}

            <form onSubmit={handleSubmit} className="flex flex-col gap-5">
              {/* Amount Input */}
              <div>
                <label className="block text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-2 uppercase">
                  Sumă (RON) <span className="text-[#C97B4B]">*</span>
                </label>
                <input
                    type="number"
                    step="0.01"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                    className={inputClasses}
                    placeholder="Ex: 50.50"
                    required
                    disabled={isInputDisabled}
                />
              </div>

              {/* Category Dropdown */}
              <div>
                <label className="block text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-2 uppercase">
                  Categorie <span className="text-[#C97B4B]">*</span>
                </label>
                <div className="relative">
                  <select
                      value={category}
                      onChange={(e) => setCategory(e.target.value)}
                      className={`${inputClasses} appearance-none cursor-pointer`}
                      required
                      disabled={isInputDisabled}
                  >
                    <option value="" disabled>Selectează o categorie...</option>
                    <option value="mancare">🍕 Mâncare & Alimente</option>
                    <option value="facturi">📄 Facturi & Utilități</option>
                    <option value="transport">🚗 Transport</option>
                    <option value="divertisment">🎮 Divertisment</option>
                  </select>
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-[#9A8A7C]">
                    <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M2.5 4.5L6 8L9.5 4.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  </div>
                </div>
              </div>

              {/* Date Picker */}
              <div>
                <label className="block text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-2 uppercase">
                  Dată <span className="text-[#C97B4B]">*</span>
                </label>
                <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className={inputClasses}
                    required
                    disabled={isInputDisabled}
                />
              </div>

              {/* Image Uploader */}
              <div>
                <label className="block text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-2 uppercase">
                  Atașament Bon Fiscal (Opțional)
                </label>
                <ImageUploader
                    onImageSelect={(file) => {
                      setReceiptFile(file);
                      if (file) {
                        handleOcrProcess(file);
                      } else {
                        setOcrError(null);
                      }
                    }}
                />
              </div>

              {/* Submit Button */}
              <button
                  type="submit"
                  disabled={isInputDisabled}
                  className="mt-2 w-full bg-[#2D2926] text-white rounded-[10px] py-3.5 text-[14px] font-medium transition-all hover:opacity-90 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed flex justify-center items-center gap-2"
              >
                {loading ? (
                    <>
                      <Loader2 className="animate-spin h-4 w-4 text-white" />
                      Se salvează...
                    </>
                ) : (
                    'Salvează Cheltuiala'
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
  );
};

export default ExpenseForm;