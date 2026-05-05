import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Calendar, RefreshCw, X, AlertCircle } from 'lucide-react'
import {
  ResponsiveContainer,
  Tooltip,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Legend,
} from 'recharts'
import { fetchCategorySummary, fetchMemberSummary } from '../services/summary'
import type { CategorySummaryDTO, MemberSummaryDTO } from '../types/SummaryDTO'

type TimeRange = '1W' | '1M' | '3M' | '1Y' | 'CUSTOM'

const PIE_COLORS = ['#C97B4B', '#D5A47C', '#9A8A7C', '#B8A99A', '#8E7056', '#E8D7C7']

const formatIsoDate = (value: Date) => value.toISOString().slice(0, 10)

const getPresetRange = (preset: Exclude<TimeRange, 'CUSTOM'>): { from: string; to: string } => {
  const end = new Date()
  const start = new Date(end)

  if (preset === '1W') start.setDate(end.getDate() - 6)
  if (preset === '1M') start.setMonth(end.getMonth() - 1)
  if (preset === '3M') start.setMonth(end.getMonth() - 3)
  if (preset === '1Y') start.setFullYear(end.getFullYear() - 1)

  return { from: formatIsoDate(start), to: formatIsoDate(end) }
}

const parseCustomDate = (dateStr: string): Date | null => {
  const dateRegex = /^(0[1-9]|[12][0-9]|3[01])\/(0[1-9]|1[012])\/\d{4}$/
  if (!dateRegex.test(dateStr)) return null

  const [day, month, year] = dateStr.split('/')
  const parsedDate = new Date(`${year}-${month}-${day}T00:00:00`)
  if (
    parsedDate.getFullYear() === Number(year)
    && parsedDate.getMonth() + 1 === Number(month)
    && parsedDate.getDate() === Number(day)
  ) {
    return parsedDate
  }

  return null
}

export default function Reports() {
  const navigate = useNavigate()

  const [timeRange, setTimeRange] = useState<TimeRange>('1M')
  const [showDatePicker, setShowDatePicker] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [activeCategory, setActiveCategory] = useState('')

  const [categorySummary, setCategorySummary] = useState<CategorySummaryDTO[]>([])
  const [memberSummary, setMemberSummary] = useState<MemberSummaryDTO[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)

  const startObj = parseCustomDate(startDate)
  const endObj = parseCustomDate(endDate)
  const isStartFormatError = startDate.length > 0 && !startObj
  const isEndFormatError = endDate.length > 0 && !endObj
  const hasFormatError = isStartFormatError || isEndFormatError
  const isChronologyError = Boolean(startObj && endObj && startObj > endObj)
  const isApplyDisabled = !startObj || !endObj || hasFormatError || isChronologyError

  const resolvedRange = useMemo(() => {
    if (timeRange === 'CUSTOM' && startObj && endObj) {
      return {
        from: formatIsoDate(startObj),
        to: formatIsoDate(endObj),
      }
    }

    return getPresetRange(timeRange === 'CUSTOM' ? '1M' : timeRange)
  }, [timeRange, startObj, endObj])

  const totalAmount = useMemo(
    () => categorySummary.reduce((sum, item) => sum + item.totalAmount, 0),
    [categorySummary],
  )

  useEffect(() => {
    const controller = new AbortController()
    let cancelled = false

    const run = async () => {
      setIsLoading(true)
      setLoadError(null)
      try {
        const [categoryData, memberData] = await Promise.all([
          fetchCategorySummary({ from: resolvedRange.from, to: resolvedRange.to }, controller.signal),
          fetchMemberSummary({ from: resolvedRange.from, to: resolvedRange.to, category: activeCategory || undefined }, controller.signal),
        ])

        if (!cancelled) {
          setCategorySummary(categoryData)
          setMemberSummary(memberData)
        }
      } catch {
        if (!cancelled) {
          setCategorySummary([])
          setMemberSummary([])
          setLoadError('Nu am putut încărca sumarul cheltuielilor din API.')
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
  }, [resolvedRange.from, resolvedRange.to, activeCategory])

  const handleApplyCustomDate = () => {
    if (!isApplyDisabled) {
      setTimeRange('CUSTOM')
      setShowDatePicker(false)
    }
  }

  const handleCloseCustomDate = () => {
    setShowDatePicker(false)
    setStartDate('')
    setEndDate('')
  }

  const handleSliceClick = (entry: CategorySummaryDTO) => {
    const nextCategory = activeCategory === entry.category ? '' : entry.category
    setActiveCategory(nextCategory)

    navigate('/expenses', {
      state: {
        initialFilters: {
          selectedCategory: nextCategory,
          startDate: resolvedRange.from,
          endDate: resolvedRange.to,
        },
      },
    })
  }

  return (
    <div style={{ maxWidth: 960, margin: '0 auto', width: '100%' }}>
      <div className="fade-up" style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 8 }}>
        <button className="btn btn-ghost btn-icon" onClick={() => navigate('/dashboard')}>
          <ArrowLeft size={16} />
        </button>
        <div className="chip chip-live">RAPOARTE · DISTRIBUȚIE</div>
      </div>
      <h1 className="h1 fade-up" style={{ marginBottom: 8 }}>Defalcare cheltuieli</h1>
      <div className="fade-up" style={{ color: 'var(--color-muted)', fontSize: 14, marginBottom: 16, lineHeight: 1.6 }}>
        Vezi distribuția pe categorii și comparația per membru în perioada selectată.
      </div>

      <div className="card fade-up" style={{ padding: '12px 16px', marginBottom: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <div className="tabs">
            {['1W', '1M', '3M', '1Y'].map((range) => (
              <button
                key={range}
                onClick={() => {
                  setTimeRange(range as Exclude<TimeRange, 'CUSTOM'>)
                  handleCloseCustomDate()
                }}
                className={`tab ${timeRange === range && !showDatePicker ? 'active' : ''}`}
              >
                {range}
              </button>
            ))}
          </div>

          <button
            onClick={() => setShowDatePicker(!showDatePicker)}
            className="btn btn-ghost"
            style={{
              fontSize: 13,
              ...(showDatePicker || timeRange === 'CUSTOM'
                ? { background: 'var(--color-primary-tint)', color: 'var(--color-primary)', borderColor: 'var(--color-primary-edge)' }
                : {}),
            }}
          >
            <Calendar size={14} /> Interval Custom
          </button>

          <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ width: 1, height: 28, background: 'var(--color-border)' }} />
            <div>
              <div className="label" style={{ marginBottom: 2 }}>TOTAL PERIOADĂ</div>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 5 }}>
                <span className="shimmer-num" style={{ fontSize: 22, fontWeight: 500, letterSpacing: '-0.8px' }}>
                  {totalAmount.toFixed(2)}
                </span>
                <span style={{ fontSize: 12, color: 'var(--color-muted-2)', fontWeight: 500 }}>RON</span>
              </div>
            </div>
          </div>
        </div>

        {showDatePicker && (
          <div className="fade-up" style={{ marginTop: 16, padding: 16, background: 'var(--color-bg)', borderRadius: 12, border: '1px solid var(--color-border)' }}>
            <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-end', gap: 10 }}>
              <div>
                <div className="label" style={{ marginBottom: 6 }}>De la (dd/mm/yyyy)</div>
                <input
                  type="text"
                  placeholder="ex: 12/04/2026"
                  maxLength={10}
                  className="input"
                  style={{
                    width: 150,
                    fontSize: 13,
                    ...(isStartFormatError || isChronologyError ? { borderColor: '#FCA5A5', background: '#FEF2F2' } : {}),
                  }}
                  value={startDate}
                  onChange={(event) => setStartDate(event.target.value)}
                />
              </div>
              <div>
                <div className="label" style={{ marginBottom: 6 }}>Până la (dd/mm/yyyy)</div>
                <input
                  type="text"
                  placeholder="ex: 20/04/2026"
                  maxLength={10}
                  className="input"
                  style={{
                    width: 150,
                    fontSize: 13,
                    ...(isEndFormatError || isChronologyError ? { borderColor: '#FCA5A5', background: '#FEF2F2' } : {}),
                  }}
                  value={endDate}
                  onChange={(event) => setEndDate(event.target.value)}
                />
              </div>
              <div style={{ display: 'flex', gap: 6 }}>
                <button
                  onClick={handleApplyCustomDate}
                  disabled={isApplyDisabled}
                  className="btn btn-primary"
                  style={{ fontSize: 13, padding: '9px 16px', opacity: isApplyDisabled ? 0.35 : 1, cursor: isApplyDisabled ? 'not-allowed' : 'pointer' }}
                >
                  Aplică
                </button>
                <button onClick={handleCloseCustomDate} className="btn btn-ghost btn-icon">
                  <X size={15} />
                </button>
              </div>
            </div>

            {hasFormatError && (
              <div className="fade-up" style={{ marginTop: 10, display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#DC2626', fontWeight: 500 }}>
                <AlertCircle size={13} /> Te rugăm să introduci datele conform formatului (dd/mm/yyyy).
              </div>
            )}
            {isChronologyError && !hasFormatError && (
              <div className="fade-up" style={{ marginTop: 10, display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#DC2626', fontWeight: 500 }}>
                <AlertCircle size={13} /> Data de început trebuie să fie înainte de data de sfârșit.
              </div>
            )}
          </div>
        )}
      </div>

      {loadError && (
        <div className="card fade-up" style={{ padding: '14px 18px', marginBottom: 16, fontSize: 13, color: '#DC2626', background: '#FEF2F2', borderColor: '#FECACA' }}>
          {loadError}
        </div>
      )}

      <div className="card fade-up" style={{ padding: '16px 20px', minHeight: 340, position: 'relative', marginBottom: 12 }}>
        {isLoading && (
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(255,255,255,0.75)', zIndex: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(3px)', borderRadius: 20 }}>
            <RefreshCw size={28} style={{ color: 'var(--color-primary)', animation: 'ring-rotate 0.9s linear infinite' }} />
          </div>
        )}

        <div className="label" style={{ marginBottom: 14 }}>DISTRIBUȚIE PE CATEGORII</div>
        <div style={{ width: '100%', height: 280 }}>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Tooltip formatter={(value: number) => `${Number(value).toFixed(2)} RON`} />
              <Pie
                data={categorySummary}
                dataKey="totalAmount"
                nameKey="category"
                innerRadius={65}
                outerRadius={100}
                paddingAngle={2}
                onClick={(entry) => handleSliceClick(entry as CategorySummaryDTO)}
              >
                {categorySummary.map((entry, index) => (
                  <Cell
                    key={entry.category}
                    fill={PIE_COLORS[index % PIE_COLORS.length]}
                    stroke={activeCategory === entry.category ? '#2D2926' : '#FFFFFF'}
                    strokeWidth={activeCategory === entry.category ? 2 : 1}
                  />
                ))}
              </Pie>
            </PieChart>
          </ResponsiveContainer>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 10 }}>
          {categorySummary.map((entry) => (
            <span
              key={entry.category}
              className="chip"
              style={activeCategory === entry.category ? { borderColor: 'var(--color-primary-edge)', background: 'var(--color-primary-tint)' } : {}}
            >
              {entry.category} ({entry.percentage.toFixed(1)}%)
            </span>
          ))}
        </div>
      </div>

      <div className="card fade-up" style={{ padding: '16px 20px', minHeight: 340, position: 'relative' }}>
        {isLoading && (
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(255,255,255,0.75)', zIndex: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(3px)', borderRadius: 20 }}>
            <RefreshCw size={28} style={{ color: 'var(--color-primary)', animation: 'ring-rotate 0.9s linear infinite' }} />
          </div>
        )}

        <div className="label" style={{ marginBottom: 14 }}>COMPARAȚIE PE MEMBRI</div>
        <div style={{ width: '100%', height: 300 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={memberSummary} margin={{ top: 10, right: 10, left: -10, bottom: 10 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#EDE9E3" />
              <XAxis dataKey="memberName" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#B8A99A' }} />
              <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#B8A99A' }} />
              <Tooltip />
              <Legend />
              <Bar dataKey="totalAmount" name="Total cheltuit (RON)" fill="#C97B4B" radius={[6, 6, 0, 0]} />
              <Bar dataKey="transactionCount" name="Nr. tranzacții" fill="#9A8A7C" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  )
}