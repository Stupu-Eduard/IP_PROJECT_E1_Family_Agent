import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { GoogleMap, Marker, useJsApiLoader } from '@react-google-maps/api'
import { api } from '../services/api'
import KidDashboard from './KidDashboard'

// ─── Google Maps container style ─────────────────────────────────────────────
const containerStyle = { width: '100%', height: '200px', borderRadius: '12px' }

// ─── Quick actions (statice, nu se schimbă) ───────────────────────────────────
const quickActions = [
  { label: 'Adaugă cheltuială', sub: 'Înregistrează manual o sumă',   nav: '/add-expense', dark: true,
    icon: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 5v14"/><path d="M5 12h14"/></svg> },
  { label: 'Scanează bon',      sub: 'OCR completează totul automat', nav: '/add-expense', dark: false,
    icon: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 8h4l2-3h6l2 3h4v11H3z"/><circle cx="12" cy="13" r="3.6"/></svg> },
  { label: 'Evoluție cheltuieli', sub: 'Grafic lunar și export PDF',  nav: '/reports',     dark: false,
    icon: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 20V10"/><path d="M10 20V4"/><path d="M16 20v-7"/><path d="M22 20v-4"/></svg> },
  { label: 'Membri familie',    sub: 'Permisiuni și invitații',       nav: '/family',      dark: false,
    icon: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="9" cy="8" r="3.2"/><path d="M2 21c0-3.3 3-6 7-6s7 2.7 7 6"/><circle cx="17" cy="9" r="2.6"/><path d="M22 20c0-2.6-2.2-4.7-5-4.7"/></svg> },
]

// ─── Helper: emoji după categorie ────────────────────────────────────────────
function categoryEmoji(cat: string): string {
  const c = (cat ?? '').toLowerCase()
  if (c.includes('aliment') || c.includes('mancare') || c.includes('food')) return '🛒'
  if (c.includes('transport') || c.includes('combustibil')) return '🚗'
  if (c.includes('educa')) return '📚'
  if (c.includes('sanat') || c.includes('medic') || c.includes('health')) return '💊'
  if (c.includes('divertis') || c.includes('entertainment')) return '🎮'
  if (c.includes('factur') || c.includes('utilit')) return '📄'
  return '💳'
}

// ─── Helper: ziua săptămânii scurtă în română ────────────────────────────────
function shortDay(dateStr: string): string {
  const days = ['D', 'L', 'M', 'M', 'J', 'V', 'S']
  return days[new Date(dateStr).getDay()]
}

// ─── Tipuri ──────────────────────────────────────────────────────────────────
interface ApiExpense {
  id: number
  amount: string | number
  description: string | null
  expenseDate: string | null
  category: string | null
  person: string | null
  location: {
    store: string | null
    city: string | null
  } | null
}

interface KpiData {
  lunaAceasta: number
  total: number
  tranzactiiLuna: number
  deltaLuna: string
}

interface BarDay {
  day: string
  amount: number
  h: number
}

export default function Dashboard() {
  const token    = useAuthStore((state) => state.token)
  const navigate = useNavigate()

  // ── State date reale ───────────────────────────────────────────────────────
  const [kpi,            setKpi]            = useState<KpiData | null>(null)
  const [recentActivity, setRecentActivity] = useState<ApiExpense[]>([])
  const [barData,        setBarData]        = useState<BarDay[]>([])
  const [userName,       setUserName]       = useState<string>('')
  const [isLoading,      setIsLoading]      = useState(true)

  // ── State WebSocket locație live (NEATINS) ─────────────────────────────────
  const [liveLocation, setLiveLocation] = useState<any>(null)
  const [tick,         setTick]         = useState(0)
  useEffect(() => {
    const id = setInterval(() => setTick((t) => t + 1), 4500)
    return () => clearInterval(id)
  }, [])

  // ── Google Maps SDK (NEATINS) ──────────────────────────────────────────────
  const mapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined
  const { isLoaded } = useJsApiLoader({
    id: 'family-agent-google-maps',
    googleMapsApiKey: mapsApiKey ?? '',
    libraries: ['drawing', 'geometry'],
  })

  // ── RBAC — decodare token (NEATINS) ───────────────────────────────────────
  let userRole = 'Parent'
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      userRole = payload.role || 'Parent'
      console.log('👤 Rol detectat din token:', userRole)
    } catch (error) {
      console.error('Eroare la parsarea JWT-ului:', error)
    }
  }

  // ── Fetch date reale de la backend ────────────────────────────────────────
  useEffect(() => {
    if (userRole === 'Child') return

    const controller = new AbortController()

    const run = async () => {
      setIsLoading(true)
      try {
        // Un singur call — GET /api/v1/expenses returnează toate cheltuielile
        const { data } = await api.get<ApiExpense[]>('/api/v1/expenses', {
          signal: controller.signal,
        })

        const now      = new Date()
        const anCurent = now.getFullYear()
        const lunaCurenta = now.getMonth() // 0-indexed

        // ── KPI: cheltuieli luna aceasta ──────────────────────────────────
        const expenseLuna = data.filter((e) => {
          if (!e.expenseDate) return false
          const d = new Date(e.expenseDate)
          return d.getFullYear() === anCurent && d.getMonth() === lunaCurenta
        })

        const totalLuna = expenseLuna.reduce(
            (sum, e) => sum + Number(e.amount),
            0
        )

        // ── KPI: luna trecuta (pentru delta) ─────────────────────────────
        const lunaT = lunaCurenta === 0 ? 11 : lunaCurenta - 1
        const anT   = lunaCurenta === 0 ? anCurent - 1 : anCurent
        const expenseLunaT = data.filter((e) => {
          if (!e.expenseDate) return false
          const d = new Date(e.expenseDate)
          return d.getFullYear() === anT && d.getMonth() === lunaT
        })
        const totalLunaT = expenseLunaT.reduce(
            (sum, e) => sum + Number(e.amount),
            0
        )
        let deltaLuna = 'vs luna trecută'
        if (totalLunaT > 0) {
          const pct = (((totalLuna - totalLunaT) / totalLunaT) * 100).toFixed(0)
          deltaLuna = `${Number(pct) >= 0 ? '+' : ''}${pct}% vs luna trecută`
        }

        // ── KPI: total general ────────────────────────────────────────────
        const totalGeneral = data.reduce((sum, e) => sum + Number(e.amount), 0)

        setKpi({
          lunaAceasta:   totalLuna,
          total:         totalGeneral,
          tranzactiiLuna: expenseLuna.length,
          deltaLuna,
        })

        // ── Activitate recentă: ultimele 5 cheltuieli ─────────────────────
        const sorted = [...data]
            .filter((e) => e.expenseDate)
            .sort(
                (a, b) =>
                    new Date(b.expenseDate!).getTime() -
                    new Date(a.expenseDate!).getTime()
            )
        setRecentActivity(sorted.slice(0, 5))

        // ── Grafic 7 zile ─────────────────────────────────────────────────
        const last7: BarDay[] = []
        for (let i = 6; i >= 0; i--) {
          const d = new Date()
          d.setDate(d.getDate() - i)
          const dateStr = d.toISOString().slice(0, 10)
          const totalZi = data
              .filter((e) => e.expenseDate?.slice(0, 10) === dateStr)
              .reduce((sum, e) => sum + Number(e.amount), 0)
          last7.push({ day: shortDay(dateStr), amount: totalZi, h: 0 })
        }
        // Normalizăm înălțimile la max 100%
        const maxAmt = Math.max(...last7.map((d) => d.amount), 1)
        last7.forEach((d) => {
          d.h = Math.max(Math.round((d.amount / maxAmt) * 100), 4)
        })
        setBarData(last7)

        // ── Numele utilizatorului din token sau din primul expense ────────
        if (token) {
          try {
            const payload = JSON.parse(atob(token.split('.')[1]))
            setUserName(payload.name || payload.sub || '')
          } catch {
            setUserName('')
          }
        }
      } catch (err: any) {
        if (err?.name !== 'CanceledError' && err?.name !== 'AbortError') {
          console.error('Eroare la încărcarea datelor dashboard:', err)
        }
      } finally {
        setIsLoading(false)
      }
    }

    void run()
    return () => controller.abort()
  }, [userRole, token])

  // ── WebSocket conexiune (NEATINS) ─────────────────────────────────────────
  useEffect(() => {
    console.log('🔌 Încercare conectare WebSocket... Rol:', userRole)
    if (userRole === 'Child') {
      console.log('🚫 WebSocket ignorat: Utilizatorul este Copil.')
      return
    }
    const host = window.location.hostname === 'localhost' ? 'localhost:8080' : window.location.host;
    const wsUrl =
        import.meta.env.VITE_WS_BASE_URL ||
        (window.location.protocol === 'https:' ? 'wss://' : 'ws://') +
        host
    const socket = new WebSocket(`${wsUrl}/locatie?token=${token}`)
    socket.onopen    = () => console.log('🟢 Dashboard: WebSocket conectat cu succes la /locatie')
    socket.onerror   = (err) => console.error('🔴 Eroare WebSocket:', err)
    socket.onmessage = (event) => {
      try {
        setLiveLocation(JSON.parse(event.data))
      } catch {
        setLiveLocation({ raw: event.data })
      }
    }
    return () => socket.close()
  }, [userRole])

  // ── KPI mouse-glow (NEATINS) ──────────────────────────────────────────────
  const handleKpiMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const r = e.currentTarget.getBoundingClientRect()
    e.currentTarget.style.setProperty('--mx', `${e.clientX - r.left}px`)
    e.currentTarget.style.setProperty('--my', `${e.clientY - r.top}px`)
  }

  // ── Medie zilnică din grafic ──────────────────────────────────────────────
  const mediZilnica =
      barData.length > 0
          ? (barData.reduce((s, d) => s + d.amount, 0) / barData.length).toFixed(2)
          : '0.00'

  // ── Child redirect (NEATINS) ──────────────────────────────────────────────
  if (userRole === 'Child') return <KidDashboard />

  // ── KPI cards dinamic ─────────────────────────────────────────────────────
  const kpiCards = [
    {
      label: 'CHELTUIELI LUNA ACEASTA',
      value: isLoading ? '...' : kpi ? kpi.lunaAceasta.toLocaleString('ro-RO', { minimumFractionDigits: 0, maximumFractionDigits: 2 }) : '—',
      unit: 'RON',
      delta: isLoading ? '' : kpi?.deltaLuna ?? '',
      barColor: 'var(--color-primary)',
      nav: '/expenses',
    },
    {
      label: 'TOTAL CHELTUIELI',
      value: isLoading ? '...' : kpi ? kpi.total.toLocaleString('ro-RO', { minimumFractionDigits: 0, maximumFractionDigits: 2 }) : '—',
      unit: 'RON',
      delta: 'Toate perioadele',
      barColor: 'var(--color-primary-soft)',
      nav: '/expenses',
    },
    {
      label: 'TRANZACȚII LUNA',
      value: isLoading ? '...' : kpi ? String(kpi.tranzactiiLuna) : '—',
      unit: '',
      delta: 'Luna curentă',
      barColor: 'var(--color-muted)',
      nav: '/reports',
    },
  ]

  return (
      <div style={{ maxWidth: 960, margin: '0 auto', width: '100%' }}>

        {/* ── Hero ────────────────────────────────────────────────────────────── */}
        <div className="fade-up" style={{ marginBottom: 40 }}>
          <div className="chip chip-live" style={{ marginBottom: 16 }}>SESIUNE ACTIVĂ</div>
          <h1 className="h1" style={{ marginBottom: 8 }}>
            Bine ai revenit,{' '}
            <span style={{
              background: 'linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-soft) 100%)',
              WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text',
            }}>
            {userName || 'utilizator'}!
          </span>
          </h1>
          <div style={{ color: 'var(--color-muted)', fontSize: 14, lineHeight: 1.6, maxWidth: 520 }}>
            {isLoading
                ? 'Se încarcă datele...'
                : kpi
                    ? `Ai ${kpi.tranzactiiLuna} cheltuieli înregistrate luna aceasta și o locație live activă în familie.`
                    : 'Bine ai venit în FamilyAgent.'}
          </div>
        </div>

        {/* ── KPI Cards ───────────────────────────────────────────────────────── */}
        <div className="label" style={{ marginBottom: 14 }}>SUMAR LUNA CURENTĂ</div>
        <div className="stagger" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 14, marginBottom: 32 }}>
          {kpiCards.map((k) => (
              <div
                  key={k.label}
                  className="kpi fade-up"
                  onClick={() => navigate(k.nav)}
                  onMouseMove={handleKpiMouseMove}
              >
                <span className="kpi-bar" style={{ background: k.barColor }} />
                <div className="label" style={{ marginTop: 14, marginBottom: 10 }}>{k.label}</div>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
                  <div className="shimmer-num" style={{ fontSize: 34, fontWeight: 500, letterSpacing: '-1.2px' }}>
                    {k.value}
                  </div>
                  {k.unit && (
                      <div style={{ fontSize: 13, color: 'var(--color-muted-2)', fontWeight: 500 }}>{k.unit}</div>
                  )}
                </div>
                <div style={{ fontSize: 12, color: 'var(--color-muted)', marginTop: 8 }}>{k.delta}</div>
              </div>
          ))}
        </div>

        {/* ── Quick Actions ────────────────────────────────────────────────────── */}
        <div className="label" style={{ marginBottom: 14 }}>ACȚIUNI RAPIDE</div>
        <div className="stagger" style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 32 }}>
          {quickActions.map((a) => (
              <div
                  key={a.label}
                  className={`qa fade-up ${a.dark ? 'qa-dark' : ''}`}
                  onClick={() => navigate(a.nav)}
                  style={{ flexDirection: 'column', alignItems: 'flex-start', gap: 14, cursor: 'pointer' }}
              >
                <span className={`qa-icon ${a.dark ? 'qa-icon-dark' : ''}`}>{a.icon}</span>
                <div>
                  <div style={{ fontSize: 13.5, fontWeight: 500, letterSpacing: '-0.2px', color: a.dark ? '#fff' : 'var(--color-ink)' }}>
                    {a.label}
                  </div>
                  <div style={{ fontSize: 11.5, color: a.dark ? 'rgba(255,255,255,0.55)' : 'var(--color-muted)', marginTop: 3 }}>
                    {a.sub}
                  </div>
                </div>
              </div>
          ))}
        </div>

        {/* ── Bottom row: Hartă + Grafic ──────────────────────────────────────── */}
        <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: 16, marginBottom: 24 }}>

          {/* Hartă live (NEATINSĂ) ─────────────────────────────────────────── */}
          <div className="card card-xl" style={{ padding: 0, overflow: 'hidden' }}>
            <div style={{ padding: '22px 24px 14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div className="label" style={{ marginBottom: 6 }}>LOCAȚIE LIVE · FAMILIE</div>
                <div style={{ fontSize: 18, fontWeight: 500, letterSpacing: '-0.3px' }}>
                  {liveLocation?.lat ? 'Locație detectată' : 'În așteptare...'}
                </div>
              </div>
              <span className="chip chip-live">LIVE</span>
            </div>

            {liveLocation?.lat && liveLocation?.lng && isLoaded ? (
                <div style={{ padding: '0 16px 16px' }}>
                  <GoogleMap
                      mapContainerStyle={containerStyle}
                      center={{ lat: liveLocation.lat, lng: liveLocation.lng }}
                      zoom={15}
                      options={{ disableDefaultUI: true }}
                  >
                    <Marker position={{ lat: liveLocation.lat, lng: liveLocation.lng }} />
                  </GoogleMap>
                  <div style={{ marginTop: 10, fontSize: 11, color: 'var(--color-muted-2)', display: 'flex', justifyContent: 'space-between' }}>
                    <span>Lat: {liveLocation.lat.toFixed(4)}</span>
                    <span>Lng: {liveLocation.lng.toFixed(4)}</span>
                    <span style={{ color: liveLocation.isRestricted ? '#E24B4A' : 'var(--color-primary)', fontWeight: 500 }}>
                  {liveLocation.isRestricted ? '⚠ ZONĂ RESTRICȚIONATĂ!' : '✓ Zonă Sigură'}
                </span>
                  </div>
                </div>
            ) : (
                <>
                  <div className="map-wrap" style={{ margin: '4px 16px 16px', height: 200 }}>
                    <div className="map-grid" />
                    <svg viewBox="0 0 400 200" preserveAspectRatio="none"
                         style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}>
                      <path className="map-route" d="M 40 170 Q 110 130, 160 140 T 280 60 T 360 40" />
                      <circle cx="40" cy="170" r="5" fill="var(--color-muted)" />
                      <circle cx="360" cy="40" r="6" fill="var(--color-primary)" />
                    </svg>
                    <div className="map-pin" style={{ left: '68%', top: '48%' }}>
                      <div className="map-pin-dot" />
                      <div style={{
                        marginTop: 6, background: 'var(--color-ink)', color: '#fff',
                        padding: '5px 9px', borderRadius: 8, fontSize: 11, fontWeight: 500,
                        whiteSpace: 'nowrap', transform: 'translateX(-50%)', position: 'relative', left: '50%',
                      }}>
                        Așteptare semnal GPS...
                      </div>
                    </div>
                  </div>
                  <div style={{ padding: '0 24px 14px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: 12, color: 'var(--color-muted)' }}>
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M12 22s7-7.4 7-13a7 7 0 0 0-14 0c0 5.6 7 13 7 13z"/><circle cx="12" cy="9" r="2.5"/>
                      </svg>
                      Așteptând date de localizare...
                    </div>
                    <span className="chip" style={{ background: '#E8F5EE', color: '#2E7B4F', borderColor: '#CDE8D8', fontSize: 11 }}>
                  ✓ Conectat
                </span>
                  </div>
                </>
            )}

            <div style={{ padding: '12px 24px', borderTop: '1px solid var(--color-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12 }}>
              <div style={{ color: 'var(--color-muted)' }}>Actualizat acum {tick % 5 + 1}s</div>
              <button
                  onClick={() => navigate('/expenses/map')}
                  style={{ color: 'var(--color-primary)', fontWeight: 500, fontSize: 12.5, background: 'none', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}
              >
                Vezi traseul complet →
              </button>
            </div>
          </div>

          {/* Grafic bare 7 zile — date reale ────────────────────────────────── */}
          <div className="card">
            <div className="label" style={{ marginBottom: 14 }}>EVOLUȚIE 7 ZILE</div>

            {isLoading ? (
                <div style={{ height: 160, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--color-muted)', fontSize: 13 }}>
                  Se încarcă...
                </div>
            ) : (
                <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 10, height: 160, padding: '10px 0 4px' }}>
                  {barData.map((b, i) => (
                      <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
                        <div
                            className="bar"
                            style={{ width: '100%', maxWidth: 32, height: `${b.h}%`, animationDelay: `${0.1 + i * 0.06}s` }}
                        >
                    <span className="bar-tip">
                      {b.amount.toLocaleString('ro-RO', { minimumFractionDigits: 0, maximumFractionDigits: 0 })} RON
                    </span>
                        </div>
                        <span style={{ fontSize: 10, color: 'var(--color-muted-2)', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                    {b.day}
                  </span>
                      </div>
                  ))}
                </div>
            )}

            <div style={{ borderTop: '1px solid var(--color-border)', marginTop: 14, paddingTop: 14, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ fontSize: 11, color: 'var(--color-muted-2)', textTransform: 'uppercase', letterSpacing: 1 }}>Medie zilnică</div>
                <div style={{ fontSize: 18, fontWeight: 500, letterSpacing: '-0.3px', marginTop: 2 }}>
                  {isLoading ? '...' : `${Number(mediZilnica).toLocaleString('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} RON`}
                </div>
              </div>
              <button
                  className="btn btn-ghost"
                  style={{ padding: '8px 12px', fontSize: 12 }}
                  onClick={() => navigate('/reports')}
              >
                Detalii →
              </button>
            </div>
          </div>
        </div>

        {/* ── Activitate recentă — date reale ─────────────────────────────────── */}
        <div className="card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 18 }}>
            <div className="label">ACTIVITATE RECENTĂ</div>
            <button
                style={{ fontSize: 12, color: 'var(--color-primary)', fontWeight: 500, background: 'none', border: 'none', cursor: 'pointer' }}
                onClick={() => navigate('/expenses')}
            >
              Vezi toate →
            </button>
          </div>

          {isLoading ? (
              <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--color-muted)', fontSize: 13 }}>
                Se încarcă activitatea...
              </div>
          ) : recentActivity.length === 0 ? (
              <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--color-muted)', fontSize: 13 }}>
                Nu există cheltuieli înregistrate încă.
              </div>
          ) : (
              <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                {recentActivity.map((r) => {
                  const cat   = r.category ?? 'Altele'
                  const store = r.location?.store ?? ''
                  const city  = r.location?.city  ?? ''
                  const desc  = [store, city].filter(Boolean).join(' · ') || r.description || 'Fără detalii'
                  const amt   = Number(r.amount).toLocaleString('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

                  return (
                      <div
                          key={r.id}
                          className="row-clickable fade-up"
                          style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 10px', borderRadius: 12, cursor: 'pointer' }}
                          onClick={() => navigate('/expenses')}
                      >
                  <span style={{
                    width: 38, height: 38, borderRadius: 11,
                    background: 'var(--color-primary-tint)', color: 'var(--color-primary)',
                    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                    border: '1px solid var(--color-primary-edge)', flexShrink: 0, fontSize: 16,
                  }}>
                    {categoryEmoji(cat)}
                  </span>
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
                            <span className="chip" style={{ fontSize: 10.5, padding: '2px 8px' }}>{cat}</span>
                          </div>
                          <div style={{ fontSize: 13.5, fontWeight: 500, color: 'var(--color-ink)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {desc}
                          </div>
                        </div>
                        {r.person && (
                            <div style={{ fontSize: 12, color: 'var(--color-muted)', flexShrink: 0 }}>{r.person}</div>
                        )}
                        <div className="row-amount" style={{ fontSize: 14.5, fontWeight: 500, color: 'var(--color-ink)', minWidth: 90, textAlign: 'right', flexShrink: 0 }}>
                          {amt} <span style={{ color: 'var(--color-muted-2)', fontSize: 11, fontWeight: 400 }}>RON</span>
                        </div>
                      </div>
                  )
                })}
              </div>
          )}
        </div>

        {/* ── Card sesiune activă ──────────────────────────────────────────────── */}
        <div style={{
          marginTop: 16,
          background: 'linear-gradient(135deg, var(--color-primary-tint) 0%, var(--color-primary-tint-2) 100%)',
          border: '1px solid var(--color-primary-edge)',
          borderRadius: 16, padding: '16px 20px',
          display: 'flex', alignItems: 'center', gap: 14,
        }}>
          <div style={{
            width: 36, height: 36, borderRadius: 10,
            background: 'var(--color-primary-edge)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18, flexShrink: 0,
          }}>🔐</div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 13.5, fontWeight: 500, color: '#7A5C44' }}>Sesiune autentificată</div>
            <div style={{ fontSize: 12, color: 'var(--color-muted-2)', lineHeight: 1.5, marginTop: 2 }}>
              Cheltuielile tale sunt accesibile doar după autentificare.
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, color: 'var(--color-muted-3)' }}>
            <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#4CAF7D', display: 'inline-block' }} />
            Sesiune activă
          </div>
        </div>

      </div>
  )
}