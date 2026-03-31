import { useState } from 'react';

// Tipizarea definită conform cerinței
interface ExpenseDTO {
  amount: number;
  category: string;
  date: string;
}

export default function AddExpense() {
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('');
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);

    // 1. Validări locale stricte (DoD #2)
    const numericAmount = parseFloat(amount);
    
    if (!amount || isNaN(numericAmount) || numericAmount <= 0) {
      setError('Suma trebuie să fie un număr strict mai mare ca 0.');
      return;
    }
    if (!category) {
      setError('Te rugăm să selectezi o categorie.');
      return;
    }
    if (!date) {
      setError('Te rugăm să alegi data tranzacției.');
      return;
    }

    // 2. Împachetarea datelor (DoD #3)
    const newExpense: ExpenseDTO = {
      amount: numericAmount,
      category: category,
      date: date,
    };

    // Imprimare în consolă pentru demonstrarea testării
    console.log('DTO Generat cu succes:', newExpense);
    
    setSuccess(true);
    setAmount('');
    setCategory('');
    setDate('');
    
    // Resetare mesaj de succes după 3 secunde
    setTimeout(() => setSuccess(false), 3000);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#fdfdfc] p-6">
      <div className="max-w-md w-full bg-white rounded-3xl shadow-[0_20px_60px_-15px_rgba(102,130,102,0.15)] p-10 border border-gray-100 animate-fade-in-up">
        
        <div className="mb-8">
          <h2 className="text-3xl font-extrabold text-gray-950 tracking-tight">Adaugă Cheltuială</h2>
          <p className="text-gray-600 mt-2 font-medium">Înregistrează o nouă tranzacție manuală</p>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 text-red-800 rounded-2xl text-sm border border-red-200 font-medium flex items-center gap-3">
            <span className="text-xl">⚠️</span> {error}
          </div>
        )}

        {success && (
          <div className="mb-6 p-4 bg-sage-50 text-sage-800 rounded-2xl text-sm border border-sage-200 font-medium flex items-center gap-3">
            <span className="text-xl">✅</span> Cheltuiala a fost validată și împachetată!
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Input Numeric (DoD #1) */}
          <div className="group">
            <label className="block text-sm font-semibold text-gray-800 mb-1.5 ml-1">Sumă (RON)</label>
            <input
              type="number"
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="w-full px-5 py-3.5 border border-gray-200 rounded-2xl transition-all duration-200 focus:border-sage-400 focus:ring-4 focus:ring-sage-100 focus:outline-none placeholder:text-gray-400"
              placeholder="ex: 150.50"
            />
          </div>

          {/* Meniu Dropdown (DoD #1) */}
          <div className="group">
            <label className="block text-sm font-semibold text-gray-800 mb-1.5 ml-1">Categorie</label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full px-5 py-3.5 border border-gray-200 rounded-2xl transition-all duration-200 focus:border-sage-400 focus:ring-4 focus:ring-sage-100 focus:outline-none bg-white text-gray-700 cursor-pointer"
            >
              <option value="" disabled>Selectează o categorie</option>
              <option value="alimente">Alimente & Supermarket</option>
              <option value="transport">Transport & Auto</option>
              <option value="facturi">Facturi & Utilități</option>
              <option value="divertisment">Divertisment & Ieșiri</option>
              <option value="altele">Altele</option>
            </select>
          </div>

          {/* Date Picker (DoD #1) */}
          <div className="group">
            <label className="block text-sm font-semibold text-gray-800 mb-1.5 ml-1">Dată</label>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="w-full px-5 py-3.5 border border-gray-200 rounded-2xl transition-all duration-200 focus:border-sage-400 focus:ring-4 focus:ring-sage-100 focus:outline-none text-gray-700 cursor-pointer"
            />
          </div>

          <button
            type="submit"
            className="w-full bg-sage-600 text-white font-bold py-4 px-6 rounded-2xl shadow-lg shadow-sage-500/20 transition-all duration-300 transform hover:bg-sage-700 hover:shadow-xl hover:-translate-y-0.5 active:scale-[0.98] mt-4"
          >
            Salvează Cheltuiala
          </button>
        </form>
      </div>
    </div>
  );
}