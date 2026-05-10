import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export default function Dashboard() {
  const logout = useAuthStore((state) => state.logout)
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  // ── WebSocket conexiune (LOGICĂ OPTIMIZATĂ JWT) ─────────────────────────────────
  useEffect(() => {
    if (!token) return;
    console.log('🔌 Încercare conectare WebSocket... Rol:', userRole);
    if (userRole === 'Child') {
      console.log('🚫 WebSocket ignorat: Utilizatorul este Copil.');
      return;
    }

    // IMPORTANT: Backend-ul rulează de obicei pe 8080, nu pe portul de Vite (5173)
    const host = window.location.hostname === 'localhost' ? 'localhost:8080' : window.location.host;
    const wsUrl = import.meta.env.VITE_WS_BASE_URL || (window.location.protocol === 'https:' ? 'wss://' : 'ws://') + host;
    
    console.log('📡 Încercare conexiune la:', `${wsUrl}/locatie?token=...`);
    const socket = new WebSocket(`${wsUrl}/locatie?token=${token}`);

    socket.onopen = () => console.log('🟢 WebSocket conectat cu succes la /locatie');
    socket.onerror = (err) => console.error('🔴 Eroare WebSocket:', err);

    socket.onmessage = (event) => {
      console.log('📡 THE PIPE (Dashboard): Mesaj brut primit:', event.data);
      try {
        const data = JSON.parse(event.data);
        console.log('📍 THE PIPE (Dashboard): Date parsate:', data);
        setLiveLocation(data);
      } catch (e) {
        console.error('❌ THE PIPE (Dashboard): Eroare parsare JSON:', e);
        setLiveLocation({ raw: event.data });
      }
    };

    return () => socket.close();
  }, [userRole, token]);

  // ── KPI mouse-glow ref ─────────────────────────────────────────────────────
  const handleKpiMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const r = e.currentTarget.getBoundingClientRect()
    e.currentTarget.style.setProperty('--mx', `${e.clientX - r.left}px`)
    e.currentTarget.style.setProperty('--my', `${e.clientY - r.top}px`)
  }

  // ── Child redirect (NEATINS) ───────────────────────────────────────────────
  if (userRole === 'Child') return <KidDashboard />

  return (
    <div className="page-shell page-shell--dashboard">
      {/* Header Navigation */}
      <nav className="dashboard-topbar">
        <div className="content-wrap surface-card surface-card--compact dashboard-topbar__panel">
          <div className="dashboard-brand">
            <div className="brand-mark brand-mark--sage">
              <svg
                viewBox="0 0 24 24"
                className="brand-mark__icon brand-mark__icon--sage"
                fill="currentColor"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-13c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5z" />
              </svg>
            </div>
            <h1 className="dashboard-brand__title">FamilyAgent</h1>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="dashboard-logout"
          >
            Logout
          </button>
        </div>
      </nav>

      {/* Main Content */}
      <div className="content-wrap">
        {/* Welcome Section */}
        <div className="surface-card surface-card--large dashboard-hero fade-in-up">
          <h2 className="dashboard-heading">Bine ai revenit! 👋</h2>
          <p className="dashboard-lead">Sesiunea ta este activă. Poți gestiona cheltuielile familiei tale ușor și eficient.</p>
          
          {/* Quick Stats */}
          <div className="dashboard-stats">
            <div className="dashboard-stat dashboard-stat--family">
              <p className="dashboard-stat__label">Cheltuieli Luna Aceasta</p>
              <p className="dashboard-stat__value dashboard-stat__value--sage">0 RON</p>
            </div>
            <div className="dashboard-stat dashboard-stat--total">
              <p className="dashboard-stat__label">Total Cheltuieli</p>
              <p className="dashboard-stat__value dashboard-stat__value--blue">0 RON</p>
            </div>
          </div>
        </div>

        {/* Actions Grid */}
        <div className="dashboard-actions">
          {/* Add Expense Card */}
          <Link 
            to="/expenses"
            className="dashboard-action"
          >
            <div className="dashboard-action__icon dashboard-action__icon--sage">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <path d="M12 5v14M5 12h14" />
              </svg>
            </div>
            <h3 className="dashboard-action__title">Adaugă Cheltuiala</h3>
            <p className="dashboard-action__text">Înregistrează o nouă cheltuiala din familia ta</p>
          </Link>

          {/* View Expenses Card */}
          <Link 
            to="/expenses"
            className="dashboard-action"
          >
            <div className="dashboard-action__icon dashboard-action__icon--blue">
              <svg
                viewBox="0 0 24 24"
                className="dashboard-action__icon-svg"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
              </svg>
            </div>
            <h3 className="dashboard-action__title">Vezi Cheltuielile</h3>
            <p className="dashboard-action__text">Vizualizează și gestionează cheltuielile înregistrate</p>
          </Link>
        </div>

        {/* Info Section */}
        <div className="dashboard-tip">
          <p className="dashboard-tip__text">
            <span className="font-semibold">💡 Sfat:</span> Cheltuielile tale sunt securizate cu JWT authentication și pot fi accesate doar după autentificare.
          </p>
        </div>
      </div>
    </div>
  )
}

