import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar.tsx'
import ChatAI from './ChatAi'
import { useAuthStore } from '../store/authStore'
import { decodeJwtPayload } from '../utils/jwt'

export default function ProtectedLayout() {
	const token = useAuthStore((s) => s.token)

	let isChild = false
	if (token) {
		try {
			const payload = decodeJwtPayload(token)
			isChild = payload?.role === 'Child'
		} catch { /* empty */ }
	}

	return (
		<div className="fa-layout">
			<Sidebar />
			<main className="fa-layout-content">
				<Outlet />
			</main>
			{/* Chat AI — doar pentru părinți, fixed bottom-right */}
			{!isChild && <ChatAI />}
		</div>
	)
}