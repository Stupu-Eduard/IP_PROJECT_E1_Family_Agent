import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import KidDashboard from './KidDashboard'

export default function Dashboard() {
  const logout = useAuthStore((state) => state.logout)
  const token = useAuthStore((state) => state.token) // 2. Extragerea token-ului din starea globală
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  // 3. Decodarea Token-ului și evaluarea permisiunilor (RBAC)
  let userRole = 'Parent';
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      userRole = payload.role || 'Parent';
    } catch (error) {
      console.error("Eroare la parsarea JWT-ului:", error);
    }
  }

  // 4. Interceptarea fluxului pentru minori
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
          <div className="text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-4 fade-in-up" style={{ animationDelay: '0.1s' }}>ACȚIUNI RAPIDE</div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-10 fade-in-up" style={{ animationDelay: '0.1s' }}>

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

            <div onClick={() => navigate('/reports')} className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 flex flex-col gap-3.5 cursor-pointer transition-all hover:border-[#C4B9AC] hover:-translate-y-[2px] group">
              <div className="w-[38px] h-[38px] rounded-[10px] bg-[#F4F0EB] flex items-center justify-center text-[16px]">📊</div>
              <div>
                <div className="text-[15px] font-medium text-[#2D2926] tracking-[-0.3px]">Evoluție cheltuieli</div>
                <div className="text-[12px] text-[#9A8A7C] leading-[1.5] mt-1">Grafice și statistici lunare</div>
              </div>
              <div className="text-[18px] text-[#D4C9BC] mt-auto self-end group-hover:text-[#2D2926] transition-colors">→</div>
            </div>

            <div onClick={() => navigate('/family')} className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 flex flex-col gap-3.5 cursor-pointer transition-all hover:border-[#C4B9AC] hover:-translate-y-[2px] group">
              <div className="w-[38px] h-[38px] rounded-[10px] bg-[#F4F0EB] flex items-center justify-center text-[16px]">👥</div>
              <div>
                <div className="text-[15px] font-medium text-[#2D2926] tracking-[-0.3px]">Membri familie</div>
                <div className="text-[12px] text-[#9A8A7C] leading-[1.5] mt-1">Conturi și permisiuni</div>
              </div>
              <div className="text-[18px] text-[#D4C9BC] mt-auto self-end group-hover:text-[#2D2926] transition-colors">→</div>
            </div>

          </div>

          {/* Bottom Section */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 fade-in-up" style={{ animationDelay: '0.2s' }}>
            <div className="md:col-span-2 bg-white border border-[#EDE9E3] rounded-[14px] p-6">
              <div className="text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-5">ACTIVITATE RECENTĂ</div>
              <div className="flex flex-col items-center justify-center min-h-[100px] gap-2">
                <div className="w-9 h-9 rounded-full border-[1.5px] border-dashed border-[#D4C9BC] flex items-center justify-center text-[16px] text-[#D4C9BC]">○</div>
                <div className="text-[12px] text-[#C4B9AC]">Nicio cheltuială înregistrată încă</div>
              </div>
            </div>

            <div className="col-span-1 bg-[#FFF8F2] border border-[#F0DFD0] rounded-[14px] p-5 flex flex-col gap-2.5">
              <div className="w-[32px] h-[32px] rounded-[8px] bg-[#F0DFD0] flex items-center justify-center text-[15px]">🔐</div>
              <div className="text-[13px] font-medium text-[#7A5C44]">Securizat cu JWT</div>
              <div className="text-[12px] text-[#B8A99A] leading-[1.6]">Cheltuielile tale sunt accesibile doar după autentificare.</div>
            </div>
          </div>
        </div>
      </div>
  )
}