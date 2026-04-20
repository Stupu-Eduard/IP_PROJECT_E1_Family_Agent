import React, { useState } from 'react';
import type { ExpenseDTO } from '../types/ExpenseDTO';

const ExpenseForm: React.FC = () => {
  // Stările pentru câmpurile formularului
  const [amount, setAmount] = useState<number | ''>('');
  const [category, setCategory] = useState<string>('');
  const [date, setDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<boolean>(false);

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

    console.log('Date pregătite pentru trimitere:', payload);

    setTimeout(() => {
      setLoading(false);
      setSuccess(true);
      
      setAmount('');
      setCategory('');
      setDate(new Date().toISOString().split('T')[0]);

      setTimeout(() => setSuccess(false), 3000);
    }, 1000);
  };

  return (
    <div className="page-shell page-shell--centered">
      <div className="surface-card surface-card--large fade-in-up">

        {/* Header Section */}
        <div className="panel-header">
          <div className="brand-mark brand-mark--sage">
            <svg
              viewBox="0 0 24 24"
              className="brand-mark__icon brand-mark__icon--sage"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"
                fill="currentColor"
              />
            </svg>
          </div>
          <h1 className="panel-title">Adaugă Cheltuială</h1>
          <p className="panel-subtitle">Înregistrează cheltuielile tale ușor și rapid</p>
        </div>

        {/* Success Message */}
        {success && (
          <div className="status-message status-message--success">
            ✓ Cheltuială adăugată cu succes!
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="status-message status-message--error">
            ✗ {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="form-stack">

          {/* Amount Input */}
          <div className="form-field">
            <label className="form-label">
              Sumă (RON) <span className="required-mark">*</span>
            </label>
            <input
              type="number"
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
              className="form-control"
              placeholder="Ex: 50.50"
              required
            />
          </div>

          {/* Category Dropdown */}
          <div className="form-field">
            <label className="form-label">
              Categorie <span className="required-mark">*</span>
            </label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="form-control form-control--select"
              required
            >
              <option value="" disabled>Selectează o categorie...</option>
              <option value="mancare">🍕 Mâncare & Alimente</option>
              <option value="facturi">📄 Facturi & Utilități</option>
              <option value="transport">🚗 Transport</option>
              <option value="divertisment">🎮 Divertisment</option>
            </select>
          </div>

          {/* Date Picker */}
          <div className="form-field">
            <label className="form-label">
              Dată <span className="required-mark">*</span>
            </label>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="form-control"
              required
            />
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading}
            className={`form-submit ${loading ? 'form-submit--loading' : ''}`}
          >
            {loading ? (
              <span>
                <span className="form-submit__spinner"></span>
                Se salvează...
              </span>
            ) : (
              'Salvează Cheltuiala'
            )}
          </button>
        </form>
      </div>
    </div>
  );
};

export default ExpenseForm;

