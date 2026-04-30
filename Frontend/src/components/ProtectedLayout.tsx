import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import ChatAI from './ChatAi'
import { useAuthStore } from '../store/authStore'

export default function ProtectedLayout() {
	const token = useAuthStore((s) => s.token)

	let isChild = false
	if (token) {
		try {
			const payload = JSON.parse(atob(token.split('.')[1]))
			isChild = payload.role === 'Child'
		} catch {}
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