import React, { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, Loader2 } from 'lucide-react';
import type { ApiExpenseListDto, ExpenseMutationPayload } from '../services/expenses';
import { createExpense, processReceiptOCR, updateExpense } from '../services/expenses';
import { useExpensesStore } from '../store/expensesStore';
import { ImageUploader } from './ImageUploader';
const IcoArrowLeft = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M19 12H5" />
    <path d="m11 5-7 7 7 7" />
  </svg>
)
const IcoCheck = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="m4 12 5 5 11-12" />
  </svg>
)
const IcoCamera = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 8h4l2-3h6l2 3h4v11H3z" />
    <circle cx="12" cy="13" r="3.6" />
  </svg>
)
type ExpenseFormLocationState = {
  expense?: ApiExpenseListDto
}
const formatDateForInput = (value?: string | null) => {
  if (!value) return new Date().toISOString().split('T')[0]
  return value.includes('T') ? value.split('T')[0] : value
}
const mapExpenseToPayload = (state: {
  amount: number | ''
  category: string
  date: string
  description: string
  person: string
  userId?: string | number | null
}): ExpenseMutationPayload => ({
  amount: Number(state.amount),
  category: state.category.trim(),
  expenseDate: state.date,
  description: state.description.trim() || undefined,
  person: state.person.trim() || undefined,
  userId: state.userId ?? undefined,
})
const ExpenseForm: React.FC = () => {
  const navigate = useNavigate()
  const params = useParams<{ id?: string }>()
  const location = useLocation()
  const locationState = (location.state ?? null) as ExpenseFormLocationState | null
  const expenseId = params.id ? Number(params.id) : null
  const isEditMode = Number.isFinite(expenseId ?? NaN)
  const upsertExpense = useExpensesStore((state) => state.upsertExpense)
  const storeExpense = useExpensesStore((state) =>
    isEditMode && expenseId ? state.expenses.find((expense) => expense.id === expenseId) : undefined,
  )
  const selectedExpense = useMemo(() => locationState?.expense ?? storeExpense, [locationState?.expense, storeExpense])
  const [amount, setAmount] = useState<number | ''>('')
  const [category, setCategory] = useState('')
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])
  const [description, setDescription] = useState('')
  const [person, setPerson] = useState('')
  const [userId, setUserId] = useState<string | number | null | undefined>(undefined)
  const [loading, setLoading] = useState(false)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [error, setError] = useState('')
  const [ocrError, setOcrError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  useEffect(() => {
    if (!isEditMode) {
      setAmount('')
      setCategory('')
      setDate(new Date().toISOString().split('T')[0])
      setDescription('')
      setPerson('')
      setUserId(undefined)
      return
    }
    if (!selectedExpense) return
    setAmount(typeof selectedExpense.amount === 'number' ? selectedExpense.amount : Number(selectedExpense.amount))
    setCategory(selectedExpense.category ?? '')
    setDate(formatDateForInput(selectedExpense.expenseDate))
    setDescription(selectedExpense.description ?? '')
    setPerson(selectedExpense.person ?? '')
    setUserId(selectedExpense.userId ?? selectedExpense.ownerId ?? selectedExpense.createdById ?? selectedExpense.personId ?? undefined)
  }, [isEditMode, selectedExpense])
  const handleOcrProcess = async (file: File) => {
    setIsAnalyzing(true)
    setOcrError(null)
    setError('')
    try {
      const data = await processReceiptOCR(file)
      if (data.amount) setAmount(data.amount)
      if (data.category) setCategory(data.category)
      if (data.date) setDate(formatDateForInput(data.date))
    } catch {
      setOcrError('Nu am putut citi automat toate datele. Te rugăm să le completezi manual.')
    } finally {
      setIsAnalyzing(false)
    }
  }
  const resetForm = () => {
    setAmount('')
    setCategory('')
    setDate(new Date().toISOString().split('T')[0])
    setDescription('')
    setPerson('')
    setUserId(undefined)
    setOcrError(null)
  }
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess(false)
    if (Number(amount) <= 0) {
      setError('Suma trebuie să fie strict mai mare ca 0!')
      return
    }
    if (!category.trim()) {
      setError('Selectează o categorie înainte de salvare.')
      return
    }
    setLoading(true)
    const payload = mapExpenseToPayload({ amount, category, date, description, person, userId })
    try {
      if (isEditMode && expenseId) {
        const updatedExpense = await updateExpense(expenseId, payload)
        upsertExpense(updatedExpense)
        navigate('/expenses', { replace: true, state: { updatedExpense } })
        return
      }
      const createdExpense = await createExpense(payload)
      upsertExpense(createdExpense)
      setSuccess(true)
      resetForm()
      window.setTimeout(() => setSuccess(false), 3000)
    } catch {
      setError(isEditMode ? 'Nu am putut actualiza cheltuiala. Încearcă din nou.' : 'Nu am putut salva cheltuiala. Încearcă din nou.')
    } finally {
      setLoading(false)
    }
  }
  const isInputDisabled = loading
  const pageTitle = isEditMode ? 'Editează cheltuiala' : 'Adaugă o cheltuială nouă'
  const pageDescription = isEditMode
    ? 'Actualizează datele tranzacției selectate și salvează schimbările în istoric.'
    : 'Trage un bon, fă-i o poză sau introdu manual. AI-ul detectează magazinul, suma, categoria și locația.'
  const submitLabel = isEditMode ? 'Salvează modificările' : 'Salvează cheltuiala'
  const successMessage = isEditMode ? 'Cheltuiala a fost actualizată cu succes!' : 'Cheltuială adăugată cu succes!'
  const backTarget = isEditMode ? '/expenses' : '/dashboard'
  return (
    <div style={{ maxWidth: 860, margin: '0 auto', width: '100%' }}>
      <div className="fade-up" style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 8 }}>
        <button className="btn btn-ghost btn-icon" onClick={() => navigate(backTarget)} aria-label="Înapoi">
          <IcoArrowLeft />
        </button>
        <div className="chip chip-live">{isEditMode ? 'EDITARE · DATE PRECOMPLETATE' : 'OCR · GATA DE SCANARE'}</div>
      </div>
      <h1 className="h1 fade-up" style={{ marginBottom: 8 }}>{pageTitle}</h1>
      <div className="fade-up" style={{ color: 'var(--color-muted)', fontSize: 14, marginBottom: 28, lineHeight: 1.6, maxWidth: 560 }}>
        {pageDescription}
      </div>
      {success && (
        <div className="fade-up" style={{
          display: 'flex', alignItems: 'center', gap: 10,
          background: 'var(--color-primary-tint)', border: '1px solid var(--color-primary-edge)',
          borderRadius: 12, padding: '12px 16px', marginBottom: 20,
          color: '#7A5C44', fontSize: 13, fontWeight: 500,
        }}>
          <span style={{ width: 20, height: 20, borderRadius: '50%', background: 'var(--color-primary)', color: '#fff', display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
            <IcoCheck />
          </span>
          {successMessage}
        </div>
      )}
      {error && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          background: '#FEF2F2', border: '1px solid #FECACA',
          borderRadius: 12, padding: '12px 16px', marginBottom: 20,
          color: '#DC2626', fontSize: 13, fontWeight: 500,
        }}>
          <AlertCircle size={16} style={{ flexShrink: 0 }} /> {error}
        </div>
      )}
      {ocrError && (
        <div style={{
          display: 'flex', alignItems: 'flex-start', gap: 10,
          background: '#FFFBEB', border: '1px solid #FDE68A',
          borderRadius: 12, padding: '12px 16px', marginBottom: 20,
          color: '#92400E', fontSize: 13,
        }}>
          <AlertCircle size={16} style={{ flexShrink: 0, marginTop: 1 }} /> {ocrError}
        </div>
      )}
      {isEditMode && !selectedExpense && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          background: '#FFFBEB', border: '1px solid #FDE68A',
          borderRadius: 12, padding: '12px 16px', marginBottom: 20,
          color: '#92400E', fontSize: 13,
        }}>
          <AlertCircle size={16} style={{ flexShrink: 0, marginTop: 1 }} />
          Nu am găsit datele tranzacției în sesiunea curentă. Completează câmpurile manual și salvează modificările.
        </div>
      )}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 18, alignItems: 'start' }}>
        <div className="card card-xl" style={{ padding: 0, overflow: 'hidden', position: 'relative' }}>
          {isAnalyzing && (
            <div style={{
              position: 'absolute', inset: 0, background: 'rgba(255,255,255,0.85)',
              backdropFilter: 'blur(4px)', zIndex: 20,
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              borderRadius: 24, gap: 18,
            }}>
              <div className="ring-spin">
                <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-primary)' }}>OCR</span>
              </div>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 16, fontWeight: 500, color: 'var(--color-ink)', letterSpacing: '-0.2px' }}>
                  Procesăm bonul tău…
                </div>
                <div style={{ fontSize: 12, color: 'var(--color-muted)', marginTop: 4 }}>
                  FamilyAgent citește datele
                </div>
              </div>
              <div style={{ width: '100%', maxWidth: 280, display: 'flex', flexDirection: 'column', gap: 10 }}>
                <div className="field-skeleton" style={{ width: '40%' }} />
                <div className="field-skeleton" style={{ width: '80%' }} />
                <div className="field-skeleton" style={{ width: '60%' }} />
              </div>
            </div>
          )}
          <div style={{ padding: '22px 28px 0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div className="label" style={{ marginBottom: 6 }}>{isEditMode ? 'CHELTUIALĂ EXISTENTĂ' : 'BON FISCAL · SCANARE'}</div>
              <div style={{ fontSize: 18, fontWeight: 500, letterSpacing: '-0.3px' }}>{isEditMode ? 'Editează înregistrarea' : 'Atașează bonul'}</div>
            </div>
            <div style={{ display: 'flex', gap: 6 }}>
              <span className={`chip ${!isAnalyzing ? '' : 'chip-neutral'}`}>1 · Încarcă</span>
              <span className={`chip ${isAnalyzing ? '' : 'chip-neutral'}`}>2 · OCR</span>
              <span className={`chip ${success ? '' : 'chip-neutral'}`}>3 · Confirmă</span>
            </div>
          </div>
          <div style={{ padding: '18px 28px 28px' }}>
            <ImageUploader
              onImageSelect={(file) => {
                if (file) {
                  void handleOcrProcess(file)
                } else {
                  setOcrError(null)
                }
              }}
            />
            <div style={{ marginTop: 14, display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, color: 'var(--color-muted)' }}>
              <IcoCamera />
              JPG, PNG · max 5 MB · OCR completează câmpurile automat
            </div>
          </div>
        </div>
        <div className="card" style={{}}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 18 }}>
            <div className="label">DETALII CHELTUIALĂ</div>
            {isAnalyzing && (
              <span style={{ fontSize: 11, color: 'var(--color-primary)', fontWeight: 500, display: 'flex', alignItems: 'center', gap: 5 }}>
                <span style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--color-primary)', display: 'inline-block', animation: 'pulse-dot 1.4s ease-out infinite' }} />
                OCR completează automat
              </span>
            )}
          </div>
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            <div>
              <label htmlFor="amount" className="label" style={{ display: 'block', marginBottom: 8 }}>
                Sumă (RON) <span style={{ color: 'var(--color-primary)' }}>*</span>
              </label>
              <input
                id="amount"
                type="number"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                className="input"
                placeholder="Ex: 50.50"
                required
                disabled={isInputDisabled}
                style={{ opacity: isInputDisabled ? 0.6 : 1 }}
              />
            </div>
            <div>
              <label htmlFor="category" className="label" style={{ display: 'block', marginBottom: 8 }}>
                Categorie <span style={{ color: 'var(--color-primary)' }}>*</span>
              </label>
              <div style={{ position: 'relative' }}>
                <select
                  id="category"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="input"
                  style={{ appearance: 'none', cursor: 'pointer', opacity: isInputDisabled ? 0.6 : 1 }}
                  required
                  disabled={isInputDisabled}
                >
                  <option value="" disabled>Selectează o categorie...</option>
                  <option value="mancare">🍕 Mâncare & Alimente</option>
                  <option value="facturi">📄 Facturi & Utilități</option>
                  <option value="transport">🚗 Transport</option>
                  <option value="divertisment">🎮 Divertisment</option>
                </select>
                <div style={{ position: 'absolute', right: 14, top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none', color: 'var(--color-muted)' }}>
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M2.5 4.5L6 8L9.5 4.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /></svg>
                </div>
              </div>
            </div>
            <div>
              <label htmlFor="description" className="label" style={{ display: 'block', marginBottom: 8 }}>
                Descriere
              </label>
              <input
                id="description"
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="input"
                placeholder="Ex: Mega Image, benzină, factură utilități"
                disabled={isInputDisabled}
                style={{ opacity: isInputDisabled ? 0.6 : 1 }}
              />
            </div>
            <div>
              <label htmlFor="person" className="label" style={{ display: 'block', marginBottom: 8 }}>
                Persoană
              </label>
              <input
                id="person"
                type="text"
                value={person}
                onChange={(e) => setPerson(e.target.value)}
                className="input"
                placeholder="Ex: Eduard"
                disabled={isInputDisabled}
                style={{ opacity: isInputDisabled ? 0.6 : 1 }}
              />
            </div>
            <div>
              <label htmlFor="date" className="label" style={{ display: 'block', marginBottom: 8 }}>
                Dată <span style={{ color: 'var(--color-primary)' }}>*</span>
              </label>
              <input
                id="date"
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="input"
                required
                disabled={isInputDisabled}
                style={{ opacity: isInputDisabled ? 0.6 : 1 }}
              />
            </div>
            <button
              type="submit"
              disabled={isInputDisabled}
              className="btn btn-primary"
              style={{ marginTop: 4, width: '100%', justifyContent: 'center' }}
            >
              {loading ? (
                <>
                  <Loader2 size={16} style={{ animation: 'ring-rotate 0.8s linear infinite' }} />
                  Se salvează...
                </>
              ) : (
                submitLabel
              )}
            </button>
          </form>
          <div style={{
            marginTop: 18, paddingTop: 18, borderTop: '1px solid var(--color-border)',
            display: 'flex', alignItems: 'center', gap: 10,
          }}>
            <div style={{ width: 32, height: 32, borderRadius: 8, background: 'var(--color-surface)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 15, flexShrink: 0 }}>🔐</div>
            <div style={{ fontSize: 11.5, color: 'var(--color-muted)', lineHeight: 1.5 }}>
              Procesare locală + criptată. Bonurile nu sunt stocate fără confirmarea ta.
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
export default ExpenseForm
