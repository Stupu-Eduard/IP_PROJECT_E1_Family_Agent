import { Outlet } from 'react-router-dom'
import LogoutTopBar from './LogoutTopBar'

export default function ProtectedLayout() {
  return (
	<div className="min-h-screen bg-[#FAF8F5] font-sans text-[#2D2926] flex flex-col">
	  <LogoutTopBar />
	  <main className="flex-1 flex flex-col">
		<Outlet />
	  </main>
	</div>
  )
}

