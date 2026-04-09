import React, { useState } from 'react';
import type { ExpenseDTO } from '../types/expense';

// -> NEW: 1. Tipizare strictă (Union Type) pentru categorii
type ExpenseCategory = 'mancare' | 'facturi' | 'transport' | 'divertisment' | '';

const ExpenseForm: React.FC = () => {
  // Stările pentru câmpurile formularului
  const [amount, setAmount] = useState<number | ''>('');

  // -> NEW: Am înlocuit <string> cu <ExpenseCategory>
  const [category, setCategory] = useState<ExpenseCategory>('');
  const [date, setDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); // Resetăm erorile la fiecare încercare

    // -> NEW: 3. Safety net - Validare suplimentară client-side
    if (!amount || !category || !date) {
      setError('Toate câmpurile sunt obligatorii!');
      return;
    }

    // Validare locală client-side: Suma strict pozitivă
    if (Number(amount) <= 0) {
      setError('Suma trebuie să fie strict mai mare ca 0!');
      return;
    }

    // Activăm starea de loading (spinner-ul / textul pe buton)
    setLoading(true);

    // Împachetăm datele exact cum cere interfața ta
    const payload: ExpenseDTO = {
      amount: Number(amount),
      category,
      date,
    };

    // În Sprintul 1, folosim Mock Data, așa că doar afișăm în consolă
    console.log('Date pregătite pentru trimitere:', payload);

    // Simulăm o întârziere de rețea de 1 secundă ca să vezi starea de loading
    setTimeout(() => {
      setLoading(false);
      alert('Cheltuială adăugată cu succes (Simulare)!');

      // Resetăm formularul după trimitere
      setAmount('');
      setCategory('');
      // -> NEW: 2. Resetare completă a formularului (aducem data la ziua curentă)
      setDate(new Date().toISOString().split('T')[0]);
    }, 1000);
  };

  return (
      <div className="w-full max-w-md mx-auto p-6 bg-white rounded-xl shadow-lg border border-gray-100">
        <h2 className="text-2xl font-bold mb-6 text-gray-800 text-center">Adaugă Cheltuială</h2>

        {/* Zona de afișare a erorilor de validare */}
        {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-sm text-center">
              {error}
            </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-5">

          {/* Input Numeric pentru Sumă */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Sumă (RON) *</label>
            <input
                type="number"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                placeholder="Ex: 50.50"
                required
            />
          </div>

          {/* Meniu Dropdown pentru Categorie */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Categorie *</label>
            <select
                value={category}
                // -> NEW: Folosim "as ExpenseCategory" pentru a potrivi tipul impus mai sus
                onChange={(e) => setCategory(e.target.value as ExpenseCategory)}
                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-white transition-all"
                required
            >
              <option value="" disabled>Selectează o categorie...</option>
              <option value="mancare">Mâncare & Alimente</option>
              <option value="facturi">Facturi & Utilități</option>
              <option value="transport">Transport</option>
              <option value="divertisment">Divertisment</option>
            </select>
          </div>

          {/* Date Picker pentru Dată */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Dată *</label>
            <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                required
            />
          </div>

          {/* Buton de Salvare cu Loading State */}
          <button
              type="submit"
              disabled={loading}
              className={`w-full py-3 mt-2 rounded-lg text-white font-bold transition-all ${
                  loading ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 shadow-md hover:shadow-lg'
              }`}
          >
            {loading ? 'Se salvează...' : 'Salvează Cheltuiala'}
          </button>
        </form>
      </div>
  );
};

export default ExpenseForm;