import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, Calendar, ChevronDown, ChevronLeft, ChevronRight, Edit2, Filter, MapPin, Plus, Search, Trash2 } from 'lucide-react'
import { decodeJwtPayload } from '../utils/jwt'
import { deleteExpense, fetchExpenses, type ApiExpenseListDto } from '../services/expenses'
import { fetchCategoryNames, fetchUserNames } from '../services/lookups'
import { useAuthStore } from '../store/authStore'
import { useExpensesStore } from '../store/expensesStore'
import ConfirmDeleteModal from '../components/ConfirmDeleteModal'
type ExpenseViewItem = {
  id: number
  date: string
  rawDate: string
  category: string
  description: string
  amount: number
  location: string
  locationId?: number
  locationCity?: string
  locationCountry?: string
  lat?: number
  lng?: number
  person: string
  userId?: string | number | null
  ownerId?: string | number | null
  createdById?: string | number | null
  personId?: string | number | null
}
const mapExpense = (expense: ApiExpenseListDto): ExpenseViewItem => {
  const rawDate = expense.expenseDate?.slice(0, 10) ?? ''
  const date = rawDate ? rawDate.split('-').reverse().join('.') : ''
  const locationParts = [expense.location?.store, expense.location?.address, expense.location?.city, expense.location?.country].filter(Boolean)
  const amount = typeof expense.amount === 'number' ? expense.amount : Number(expense.amount)
  return {
    id: expense.id,
    date,
    rawDate,
    category: expense.category ?? 'Fără categorie',
    description: expense.description ?? '',
    amount: Number.isFinite(amount) ? amount : 0,
    location: locationParts.length > 0 ? locationParts.join(', ') : 'Fără locație',
    locationId: expense.location?.id,
    locationCity: expense.location?.city ?? undefined,
    locationCountry: expense.location?.country ?? undefined,
    lat: expense.location?.lat ?? undefined,
    lng: expense.location?.lng ?? undefined,
    person: expense.person ?? 'N/A',
    userId: expense.userId ?? undefined,
    ownerId: expense.ownerId ?? undefined,
    createdById: expense.createdById ?? undefined,
    personId: expense.personId ?? undefined,
  }
}
const normalizeIdentity = (value: unknown) => (value === null || value === undefined ? null : String(value))
const avatarStyle = (name: string) => {
  if (name.startsWith('E')) return { background: 'linear-gradient(135deg, #C97B4B, #E8A87C)' }
  if (name.startsWith('M')) return { background: 'linear-gradient(135deg, #9A8A7C, #B8A99A)' }
  return { background: 'linear-gradient(135deg, #B5956A, #D4B896)' }
}

const toApiExpenseFallback = (expense: ExpenseViewItem): ApiExpenseListDto => ({
  id: expense.id,
  amount: expense.amount,
  category: expense.category,
  description: expense.description,
  expenseDate: expense.rawDate,
  person: expense.person,
  userId: expense.userId,
  ownerId: expense.ownerId,
  createdById: expense.createdById,
  personId: expense.personId,
  location: expense.locationId
    ? {
      id: expense.locationId,
      store: null,
      address: null,
      city: expense.locationCity ?? null,
      country: expense.locationCountry ?? null,
      lat: expense.lat ?? null,
      lng: expense.lng ?? null,
    }
    : null,
})

export default function Expenses() {
  const navigate = useNavigate()
  const location = useLocation()
  const initialFilters = (location.state as {
    initialFilters?: {
      selectedCategory?: string
      startDate?: string
      endDate?: string
    }
  } | null)?.initialFilters
  const token = useAuthStore((state) => state.token)
  const payload = token ? decodeJwtPayload(token) : null
  const currentUserIdentity = normalizeIdentity(payload?.userId ?? payload?.id ?? payload?.sub ?? payload?.email ?? payload?.name)
  const expenses = useExpensesStore((state) => state.expenses)
  const setExpenses = useExpensesStore((state) => state.setExpenses)
  const removeExpense = useExpensesStore((state) => state.removeExpense)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [availableCategories, setAvailableCategories] = useState<string[]>([])
  const [availablePeople, setAvailablePeople] = useState<string[]>([])
  const [startDate, setStartDate] = useState(initialFilters?.startDate ?? '')
  const [endDate, setEndDate] = useState(initialFilters?.endDate ?? '')
  const [selectedCategory, setSelectedCategory] = useState(initialFilters?.selectedCategory ?? '')
  const [selectedPerson, setSelectedPerson] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const [expenseToDelete, setExpenseToDelete] = useState<ExpenseViewItem | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  useEffect(() => {
    const controller = new AbortController()
    const run = async () => {
      try {
        const [categories, people] = await Promise.all([
          fetchCategoryNames(controller.signal),
          fetchUserNames(controller.signal),
        ])
        setAvailableCategories((categories ?? []).filter(Boolean))
        setAvailablePeople((people ?? []).filter(Boolean))
      } catch {
        // ignorați
      }
    }
    void run()
    return () => controller.abort()
  }, [])
  useEffect(() => {
    const controller = new AbortController()
    let cancelled = false
    const run = async () => {
      setIsLoading(true)
      setLoadError(null)
      try {
        const data = await fetchExpenses({
          category: selectedCategory || undefined,
          person: selectedPerson || undefined,
        }, controller.signal)
        if (!cancelled) setExpenses(Array.isArray(data) ? data : [])
      } catch {
        if (!cancelled) {
          setExpenses([])
          setLoadError('Nu am putut încărca cheltuielile din backend. Verifică VITE_API_BASE_URL și backend-ul pe 8080.')
        }
      } finally {
        if (!cancelled) setIsLoading(false)
      }
    }
    void run()
    return () => {
      cancelled = true
      controller.abort()
    }
  }, [selectedCategory, selectedPerson, setExpenses])
  const visibleExpenses = useMemo(() => {
    const sourceExpenses = Array.isArray(expenses) ? expenses : []
    return sourceExpenses.map(mapExpense).filter((expense) => {
      if (startDate && expense.rawDate < startDate) return false
      if (endDate && expense.rawDate > endDate) return false
      return true
    })
  }, [expenses, startDate, endDate])
  const canMutateExpense = (expense: ExpenseViewItem) => {
    const expenseIdentity = normalizeIdentity(expense.userId ?? expense.ownerId ?? expense.createdById ?? expense.personId)
    if (!currentUserIdentity || !expenseIdentity) return true
    return currentUserIdentity === expenseIdentity
  }
  const openMap = (expense: ExpenseViewItem) => {
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
    })
  }
  const openEdit = (expense: ApiExpenseListDto) => {
    navigate(`/expenses/${expense.id}/edit`, { state: { expense } })
  }
  const requestDelete = (expense: ExpenseViewItem) => {
    setDeleteError(null)
    setExpenseToDelete(expense)
  }
  const confirmDelete = async () => {
    if (!expenseToDelete) return
    const snapshot = expenses
    setIsDeleting(true)
    setDeleteError(null)
    removeExpense(expenseToDelete.id)
    try {
      await deleteExpense(expenseToDelete.id)
      setExpenseToDelete(null)
    } catch {
      setExpenses(snapshot)
      setDeleteError('Nu am putut șterge cheltuiala. Încearcă din nou.')
    } finally {
      setIsDeleting(false)
    }
  }
  const inputStyle = 'w-full bg-white border border-[#EDE9E3] rounded-[10px] h-10 px-3 text-[13px] text-[#2D2926] focus:outline-none focus:border-[#C4B9AC] transition-colors appearance-none'
  return (
    <div style={{ maxWidth: 960, margin: '0 auto', width: '100%' }}>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 fade-in-up">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate('/dashboard')} className="w-10 h-10 bg-white border border-[#EDE9E3] rounded-[10px] flex items-center justify-center text-[#2D2926] hover:border-[#C4B9AC] transition-colors shadow-sm">
            <ArrowLeft size={18} />
          </button>
          <h2 className="text-[24px] font-medium text-[#2D2926] tracking-tight">Istoric Cheltuieli</h2>
        </div>
        <div className="flex gap-2">
          <button onClick={() => navigate('/expenses/all-map', { state: { expenses: visibleExpenses, filters: { startDate, endDate, selectedCategory, selectedPerson } } })} className="bg-[#FAF8F5] border border-[#EDE9E3] text-[#2D2926] px-5 py-2.5 rounded-[10px] text-[14px] font-medium flex items-center justify-center gap-2 hover:bg-[#F0ECE7] transition-colors shadow-sm">
            <MapPin size={18} />
            <span>Vezi pe Hartă</span>
          </button>
          <button onClick={() => navigate('/add-expense')} className="bg-[#2D2926] text-white px-5 py-2.5 rounded-[10px] text-[14px] font-medium flex items-center justify-center gap-2 hover:opacity-90 transition-opacity shadow-[0_4px_12px_rgba(45,41,38,0.15)]">
            <Plus size={18} /><span>Adaugă</span>
          </button>
        </div>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-5 gap-3 mb-8 fade-in-up" style={{ animationDelay: '0.1s' }}>
        <div className="relative">
          <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C]" size={16} />
          <input type="date" className={`${inputStyle} pl-10`} title="Data de început" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        </div>
        <div className="relative">
          <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C]" size={16} />
          <input type="date" className={`${inputStyle} pl-10`} title="Data de final" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </div>
        <div className="relative">
          <select className={inputStyle} value={selectedCategory} onChange={(e) => setSelectedCategory(e.target.value)}>
            <option value="">Toate Categoriile</option>
            {availableCategories.map((category) => <option key={category} value={category}>{category}</option>)}
          </select>
          <ChevronDown className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C] pointer-events-none" size={16} />
        </div>
        <div className="relative">
          <select className={inputStyle} value={selectedPerson} onChange={(e) => setSelectedPerson(e.target.value)}>
            <option value="">Orice Persoană</option>
            {availablePeople.map((person) => <option key={person} value={person}>{person}</option>)}
          </select>
          <ChevronDown className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C] pointer-events-none" size={16} />
        </div>
        <button onClick={() => { setStartDate(''); setEndDate(''); setSelectedCategory(''); setSelectedPerson('') }} className="bg-white border border-[#EDE9E3] rounded-[10px] px-4 py-2.5 text-[13px] font-medium text-[#2D2926] flex items-center justify-center gap-2 hover:border-[#C4B9AC] transition-colors" title="Resetează filtrele">
          <Filter size={16} /> Resetează
        </button>
      </div>
      {loadError && <div className="card fade-up" style={{ padding: '14px 18px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10, fontSize: 13, color: 'var(--color-muted)' }}><Search size={16} style={{ flexShrink: 0 }} /> {loadError}</div>}
      {deleteError && <div className="card fade-up" style={{ padding: '14px 18px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10, fontSize: 13, color: '#DC2626', background: '#FEF2F2', borderColor: '#FECACA' }}><Trash2 size={16} style={{ flexShrink: 0 }} /> {deleteError}</div>}
      {isLoading && (
        <div className="card fade-up" style={{ padding: 0, overflow: 'hidden' }}>
          {[1, 2, 3, 4].map((i) => (
            <div key={i} style={{ display: 'grid', gridTemplateColumns: '90px 1.4fr 1fr 1fr 130px 120px', padding: '18px 24px', borderBottom: '1px solid var(--color-border)', gap: 16 }}>
              <div className="skeleton" style={{ height: 14, width: '80%' }} />
              <div className="skeleton" style={{ height: 14, width: '60%' }} />
              <div className="skeleton" style={{ height: 14, width: '70%' }} />
              <div className="skeleton" style={{ height: 14, width: '40%' }} />
              <div className="skeleton" style={{ height: 14, width: '50%', marginLeft: 'auto' }} />
              <div className="skeleton" style={{ height: 14, width: '60%', marginLeft: 'auto' }} />
            </div>
          ))}
        </div>
      )}
      {!isLoading && visibleExpenses.length === 0 && (
        <div className="card fade-up" style={{ padding: 48, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
          <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'var(--color-surface)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16, color: 'var(--color-muted-3)' }}><Search size={22} /></div>
          <div style={{ fontSize: 15, fontWeight: 500, color: 'var(--color-ink)', marginBottom: 6 }}>Nu s-au găsit cheltuieli</div>
          <div style={{ fontSize: 13, color: 'var(--color-muted)' }}>Nu există nicio înregistrare care să corespundă filtrelor selectate.</div>
        </div>
      )}
      {!isLoading && visibleExpenses.length > 0 && (
        <div className="card fade-up" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '90px 1.4fr 1fr 1fr 130px 120px', padding: '12px 24px', borderBottom: '1px solid var(--color-border)', background: 'var(--color-bg)' }}>
            {['DATĂ', 'DESCRIERE', 'LOCAȚIE', 'PERSOANĂ', 'SUMĂ', 'ACȚIUNI'].map((header, index) => <div key={header} className="label" style={{ textAlign: index >= 4 ? 'right' : 'left' }}>{header}</div>)}
          </div>
          <div className="stagger">
            {visibleExpenses.map((expense) => (
              <div key={expense.id} className="row-clickable fade-up" style={{ display: 'grid', gridTemplateColumns: '90px 1.4fr 1fr 1fr 130px 120px', padding: '16px 24px', borderBottom: '1px solid var(--color-border)', alignItems: 'center' }}>
                <div style={{ fontSize: 12.5, color: 'var(--color-muted)', fontWeight: 500 }}>{expense.date}</div>
                <div>
                  <span className="chip" style={{ fontSize: 10.5, padding: '2px 8px', marginBottom: 6 }}>{expense.category}</span>
                  <div style={{ fontSize: 13.5, fontWeight: 500, color: 'var(--color-ink)', marginTop: 4 }}>{expense.description}</div>
                </div>
                <div>
                  <button type="button" onClick={() => openMap(expense)} style={{ display: 'flex', alignItems: 'center', gap: 6, background: 'none', border: 'none', cursor: 'pointer', color: 'var(--color-muted)', fontSize: 12.5 }}>
                    <MapPin size={13} style={{ color: 'var(--color-muted-3)', flexShrink: 0 }} />
                    <span style={{ textDecoration: 'underline', textUnderlineOffset: 2 }}>{expense.location}</span>
                  </button>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <div className="avatar avatar-sm" style={avatarStyle(expense.person)}>{expense.person.charAt(0)}</div>
                  <span style={{ fontSize: 12.5, color: 'var(--color-muted)' }}>{expense.person}</span>
                </div>
                <div className="row-amount" style={{ textAlign: 'right', fontSize: 14.5, fontWeight: 500, color: 'var(--color-ink)' }}>
                  {expense.amount.toFixed(2)} <span style={{ color: 'var(--color-muted-2)', fontSize: 11, fontWeight: 400 }}>RON</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                  {canMutateExpense(expense) ? (
                    <>
                      <button type="button" onClick={() => openEdit(expenses.find((item) => item.id === expense.id) ?? toApiExpenseFallback(expense))} className="btn btn-ghost btn-icon" style={{ width: 34, height: 34 }} aria-label="Editează cheltuiala"><Edit2 size={15} /></button>
                      <button type="button" onClick={() => requestDelete(expense)} className="btn btn-ghost btn-icon" style={{ width: 34, height: 34, color: '#DC2626' }} aria-label="Șterge cheltuiala"><Trash2 size={15} /></button>
                    </>
                  ) : <span style={{ fontSize: 11.5, color: 'var(--color-muted-2)' }}>Doar citire</span>}
                </div>
              </div>
            ))}
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 24px', borderTop: '1px solid var(--color-border)' }}>
            <div style={{ fontSize: 12, color: 'var(--color-muted)' }}>Pagina <strong style={{ color: 'var(--color-ink)' }}>{currentPage}</strong> din 2</div>
            <div style={{ display: 'flex', gap: 6 }}>
              <button onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))} disabled={currentPage === 1} className="btn btn-ghost btn-icon" style={{ width: 34, height: 34, opacity: currentPage === 1 ? 0.4 : 1 }}><ChevronLeft size={15} /></button>
              {[1, 2].map((page) => <button key={page} onClick={() => setCurrentPage(page)} className={page === currentPage ? 'btn btn-primary' : 'btn btn-ghost'} style={{ width: 34, height: 34, padding: 0, fontSize: 13 }}>{page}</button>)}
              <button onClick={() => setCurrentPage((prev) => Math.min(prev + 1, 2))} disabled={currentPage === 2} className="btn btn-ghost btn-icon" style={{ width: 34, height: 34, opacity: currentPage === 2 ? 0.4 : 1 }}><ChevronRight size={15} /></button>
            </div>
          </div>
        </div>
      )}
      <ConfirmDeleteModal
        open={expenseToDelete !== null}
        title="Ștergi cheltuiala?"
        description={expenseToDelete ? `Ești sigur că vrei să ștergi înregistrarea „${expenseToDelete.description || expenseToDelete.category}” din ${expenseToDelete.date}?` : undefined}
        isLoading={isDeleting}
        onCancel={() => { if (!isDeleting) setExpenseToDelete(null) }}
        onConfirm={() => void confirmDelete()}
      />
    </div>
  )
}
