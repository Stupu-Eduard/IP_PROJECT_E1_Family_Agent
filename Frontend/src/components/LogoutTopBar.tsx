import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export default function LogoutTopBar() {
  const navigate = useNavigate()
  const logout = useAuthStore((state) => state.logout)

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="sticky top-0 z-20 bg-[#FAF8F5] border-b border-[#EDE9E3] px-6 lg:px-10 py-4 flex justify-end">
      <button
        type="button"
        onClick={handleLogout}
        aria-label="Logout"
        className="text-[12px] font-medium text-[#8C7E6E] px-3.5 py-1.5 border border-[#E2DDD7] rounded-[20px] bg-white hover:border-[#C4B9AC] hover:text-[#2D2926] transition-colors"
      >
        Logout
      </button>
    </header>
  )
}
