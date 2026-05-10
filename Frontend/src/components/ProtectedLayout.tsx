import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar.tsx'
import ChatAI from './ChatAi'
import { useAuthStore } from '../store/authStore'
import { getProfileRole } from '../utils/profile'

export default function ProtectedLayout() {
	const token = useAuthStore((s) => s.token)
	const profile = useAuthStore((s) => s.profile)

	const isChild = getProfileRole(profile, token) === 'Child'

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