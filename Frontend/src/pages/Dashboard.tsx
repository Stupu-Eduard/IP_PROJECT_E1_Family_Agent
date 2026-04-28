import { useEffect } from 'react' // Importăm useEffect pentru lifecycle
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { useMapStore } from '../store/mapStore' // Importăm store-ul pentru locație
import KidDashboard from './KidDashboard'

export default function Dashboard() {
  const logout = useAuthStore((state) => state.logout)
  const token = useAuthStore((state) => state.token)
  const navigate = useNavigate()

  // --- LOGICA PENTRU DATE LIVE ȘI GEOFENCING ---
  const {
    startLiveTracking,
    stopLiveTracking,
    currentChildLocation,
    isOptimisticallyDanger
  } = useMapStore()

  useEffect(() => {
    // Deschidem "țeava" de date (SSE) imediat ce părintele intră pe Dashboard
    if (token) {
      // Notă: ID-ul 1 este generic; în mod ideal îl poți extrage din payload-ul JWT
      startLiveTracking(1, token);
    }

    // Funcție de cleanup: Închidem conexiunea când utilizatorul pleacă de pe Dashboard
    return () => stopLiveTracking();
  }, [token, startLiveTracking, stopLiveTracking]);
  // ---------------------------------------------

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  // Decodarea Token-ului și evaluarea permisiunilor (RBAC)
  let userRole = 'Parent';
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      userRole = payload.role || 'Parent';
    } catch (error) {
      console.error("Eroare la parsarea JWT-ului:", error);
    }
  }

  // Interceptarea fluxului pentru minori
  if (userRole === 'Child') {
    return <KidDashboard />;
  }

  return (
      <div className="min-h-screen bg-[#FAF8F5] font-sans flex flex-col">
        {/* Topbar */}
        <nav className="sticky top-0 z-10 bg-[#FAF8F5] border-b border-[#EDE9E3] px-6 lg:px-10 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2.5 cursor-pointer" onClick={() => navigate('/dashboard')}>
            <div className="w-8 h-8 rounded-[8px] bg-[#2D2926] flex items-center justify-center text-[13px] font-medium text-[#FAF8F5] tracking-tight">FA</div>
            <span className="text-[15px] font-medium text-[#2D2926] tracking-tight">FamilyAgent</span>
          </div>
          <div className="flex items-center gap-2.5">
            <div className="text-[12px] text-[#8C7E6E] px-3.5 py-1.5 border border-[#E2DDD7] rounded-[20px] bg-white hover:border-[#C4B9AC] transition-colors cursor-pointer hidden sm:block">
              Aprilie 2026
            </div>
            <button
                onClick={handleLogout}
                title="Logout"
                className="w-8 h-8 rounded-full bg-[#E8D5C4] flex items-center justify-center text-[12px] font-medium text-[#7A5C44] cursor-pointer hover:opacity-80 transition-opacity"
            >
              MC
            </button>
          </div>
        </nav>

        {/* --- BANNER DE ALERTĂ LIVE --- */}
        {(isOptimisticallyDanger || currentChildLocation?.isRestricted) && (
            <div className="bg-red-600 text-white px-6 py-4 flex items-center justify-between shadow-lg animate-pulse z-20">
              <div className="flex items-center gap-3">
                <span className="text-xl">⚠️</span>
                <div>
                  <p className="font-bold text-[14px]">ALERTĂ DE SIGURANȚĂ</p>
                  <p className="text-[12px] opacity-90">Copilul a părăsit perimetrul de siguranță sau este într-o zonă interzisă!</p>
                </div>
              </div>
              <button
                  onClick={() => navigate('/expenses/map')}
                  className="bg-white text-red-600 px-4 py-1.5 rounded-full text-[12px] font-bold hover:bg-gray-100 transition-colors"
              >
                VEZI PE HARTĂ
              </button>
            </div>
        )}

        {/* Body Content */}
        <div className="px-6 lg:px-10 pt-12 pb-20 max-w-[960px] mx-auto w-full flex-1">
          {/* Hero */}
          <div className="mb-12 fade-in-up">
            <div className="text-[11px] tracking-[1.2px] text-[#B8A99A] font-medium mb-2.5">DASHBOARD · SESIUNE ACTIVĂ</div>
            <div className="text-[36px] font-medium text-[#2D2926] tracking-[-1.2px] leading-[1.15]">
              Bine ai revenit,<br/><span className="text-[#C97B4B]">Edi!</span>
            </div>
            <div className="text-[14px] text-[#9A8A7C] mt-2.5 leading-[1.6]">
              Poți gestiona cheltuielile familiei tale ușor și eficient.
            </div>
          </div>

          {/* KPIs */}
          <div className="text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-4 fade-in-up">SUMAR LUNA CURENTĂ</div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-10 fade-in-up">
            <div onClick={() => navigate('/expenses')} className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 cursor-pointer transition-all hover:border-[#C4B9AC] hover:-translate-y-[2px]">
              <div className="h-[3px] rounded-[2px] mb-[18px] w-7 bg-[#C97B4B]"></div>
              <div className="text-[11px] tracking-[0.6px] text-[#B8A99A] mb-2">CHELTUIELI LUNA ACEASTA</div>
              <div className="text-[30px] font-medium text-[#2D2926] tracking-[-1px]">
                100 <sup className="text-[14px] font-normal text-[#B8A99A] tracking-normal align-super">RON</sup>
              </div>
              <div className="text-[11px] text-[#C4B9AC] mt-1.5">Nicio înregistrare încă</div>
            </div>

            <div onClick={() => navigate('/expenses')} className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 cursor-pointer transition-all hover:border-[#C4B9AC] hover:-translate-y-[2px]">
              <div className="h-[3px] rounded-[2px] mb-[18px] w-7 bg-[#B5956A]"></div>
              <div className="text-[11px] tracking-[0.6px] text-[#B8A99A] mb-2">TOTAL CHELTUIELI</div>
              <div className="text-[30px] font-medium text-[#2D2926] tracking-[-1px]">
                500 <sup className="text-[14px] font-normal text-[#B8A99A] tracking-normal align-super">RON</sup>
              </div>
              <div className="text-[11px] text-[#C4B9AC] mt-1.5">Toate perioadele</div>
            </div>

            <div onClick={() => navigate('/reports')} className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 cursor-pointer transition-all hover:border-[#C4B9AC] hover:-translate-y-[2px]">
              <div className="h-[3px] rounded-[2px] mb-[18px] w-7 bg-[#D4C9BC]"></div>
              <div className="text-[11px] tracking-[0.6px] text-[#B8A99A] mb-2">TRANZACȚII</div>
              <div className="text-[30px] font-medium text-[#2D2926] tracking-[-1px]">0</div>
              <div className="text-[11px] text-[#C4B9AC] mt-1.5">Luna curentă</div>
            </div>
          </div>

          {/* Actions */}
          <div className="text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-4 fade-in-up">ACȚIUNI RAPIDE</div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-10 fade-in-up">
            <div onClick={() => navigate('/add-expense')} className="bg-[#2D2926] border border-[#2D2926] rounded-[14px] p-6 flex flex-col gap-3.5 cursor-pointer transition-all hover:border-[#C4B9AC] hover:-translate-y-[2px] group">
              <div className="w-[38px] h-[38px] rounded-[10px] bg-white/10 flex items-center justify-center text-[16px] text-white">＋</div>
              <div>
                <div className="text-[15px] font-medium text-[#FAF8F5] tracking-[-0.3px]">Adaugă cheltuială</div>
                <div className="text-[12px] text-[#FAF8F5]/50 leading-[1.5] mt-1">Înregistrează o nouă cheltuială</div>
              </div>
              <div className="text-[18px] text-[#FAF8F5]/30 mt-auto self-end group-hover:text-white/80 transition-colors">→</div>
            </div>

            <div onClick={() => navigate('/expenses')} className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 flex flex-col gap-3.5 cursor-pointer transition-all hover:border-[#C4B9AC] hover:-translate-y-[2px] group">
              <div className="w-[38px] h-[38px] rounded-[10px] bg-[#F4F0EB] flex items-center justify-center text-[16px]">🔍</div>
              <div>
                <div className="text-[15px] font-medium text-[#2D2926] tracking-[-0.3px]">Vezi cheltuielile</div>
                <div className="text-[12px] text-[#9A8A7C] leading-[1.5] mt-1">Vizualizează și gestionează înregistrările</div>
              </div>
              <div className="text-[18px] text-[#D4C9BC] mt-auto self-end group-hover:text-[#2D2926] transition-colors">→</div>
            </div>
          </div>
        </div>
      </div>
  )
}