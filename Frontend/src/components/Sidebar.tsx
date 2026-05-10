import { useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { getProfileAvatarUrl, getProfileDisplayName, getProfileInitials, getProfileRole, getProfileRoleLabel } from '../utils/profile'

// ── Nav items Părinte ──────────────────────────────────────────────────────
const parentNavItems = [
    { id: 'dashboard',    label: 'Dashboard',   icon: (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M3 11.5 12 4l9 7.5"/><path d="M5 10v10h14V10"/><path d="M10 20v-6h4v6"/>
            </svg>
        )},
    { id: 'expenses',     label: 'Cheltuieli',  icon: (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M5 3h14v18l-3-2-3 2-3-2-3 2-2-2V3z"/><path d="M9 8h6"/><path d="M9 12h6"/><path d="M9 16h4"/>
            </svg>
        )},
    { id: 'reports',      label: 'Rapoarte',    icon: (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M4 20V10"/><path d="M10 20V4"/><path d="M16 20v-7"/><path d="M22 20v-4"/>
            </svg>
        )},
    { id: 'family',       label: 'Familie',     icon: (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="9" cy="8" r="3.2"/><path d="M2 21c0-3.3 3-6 7-6s7 2.7 7 6"/>
                <circle cx="17" cy="9" r="2.6"/><path d="M22 20c0-2.6-2.2-4.7-5-4.7"/>
            </svg>
        )},
    { id: 'profile-settings', label: 'Setări profil', icon: (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 0 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 0 1-4 0v-.2a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 0 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 0 1 0-4h.2a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8l-.1-.1A2 2 0 1 1 7.1 4.6l.1.1a1.7 1.7 0 0 0 1.8.3h.1a1.7 1.7 0 0 0 1-1.5V3a2 2 0 0 1 4 0v.2a1.7 1.7 0 0 0 1 1.5h.1a1.7 1.7 0 0 0 1.8-.3l.1-.1A2 2 0 1 1 19.4 7l-.1.1a1.7 1.7 0 0 0-.3 1.8v.1a1.7 1.7 0 0 0 1.5 1H21a2 2 0 0 1 0 4h-.2a1.7 1.7 0 0 0-1.4 1Z"/>
            </svg>
        )},
    { id: 'expenses/map', label: 'Hartă Live',  icon: (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2z"/><path d="M9 4v14"/><path d="M15 6v14"/>
            </svg>
        )},
]

// ── Nav items Copil — acces limitat ───────────────────────────────────────
const childNavItems = [
    { id: 'dashboard',    label: 'Dashboard',   icon: parentNavItems[0].icon },
    { id: 'expenses',     label: 'Cheltuielile mele', icon: parentNavItems[1].icon },
    { id: 'family',       label: 'Familie',     icon: parentNavItems[3].icon },
    { id: 'profile-settings', label: 'Setări profil', icon: parentNavItems[4].icon },
]

export default function Sidebar() {
    const navigate  = useNavigate()
    const location  = useLocation()
    const token     = useAuthStore((s) => s.token)
    const profile   = useAuthStore((s) => s.profile)
    const logout    = useAuthStore((s) => s.logout)

    // ── Detectare rol din JWT (același mecanism ca în Dashboard) ──────────
    const userRole = getProfileRole(profile, token)
    const userName = getProfileDisplayName(profile, token, userRole === 'Child' ? 'Andrei P.' : 'Eduard P.')
    const userRoleLabel = getProfileRoleLabel(userRole)
    const userInitials = getProfileInitials(profile, token, userName)
    const userAvatar = getProfileAvatarUrl(profile, token)
    const isChild  = userRole === 'Child'
    const navItems = isChild ? childNavItems : parentNavItems

    const activeId = navItems.find(item =>
        location.pathname === '/' + item.id ||
        location.pathname.startsWith('/' + item.id)
    )?.id ?? 'dashboard'

    const handleLogout = () => {
        logout()
        navigate('/login', { replace: true })
    }

    return (
        <aside className="fa-sidebar">
            {/* Logo */}
            <div className="fa-sidebar-logo">
                <div className="fa-sidebar-logo-icon">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M3 7h15a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
                        <circle cx="17" cy="14" r="1.4"/>
                    </svg>
                </div>
                <div>
                    <div className="fa-sidebar-logo-name">Family Agent</div>
                    <div className="fa-sidebar-logo-sub">Familia Popescu</div>
                </div>
            </div>

            {/* Nav — filtrat după rol */}
            <nav className="fa-sidebar-nav">
                {navItems.map(({ id, label, icon }) => {
                    const isActive = activeId === id
                    return (
                        <button
                            key={id}
                            onClick={() => navigate('/' + id)}
                            className={`fa-sidebar-link ${isActive ? 'active' : ''}`}
                        >
              <span className="fa-sidebar-icon-bg">
                <span className="fa-sidebar-icon">{icon}</span>
              </span>
                            {label}
                            {isActive && <span className="fa-sidebar-active-dot" />}
                        </button>
                    )
                })}
            </nav>

            <div style={{ flex: 1 }} />

            {/* User footer */}
            <div className="fa-sidebar-footer">
                <div
                    className="fa-sidebar-avatar"
                    role="button"
                    tabIndex={0}
                    onClick={() => navigate('/profile-settings')}
                    onKeyDown={(event) => {
                        if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault()
                            navigate('/profile-settings')
                        }
                    }}
                    style={isChild
                        ? { background: 'linear-gradient(135deg, #B5956A, #D4B896)' }
                        : undefined
                    }
                >
                    {userAvatar ? <img src={userAvatar} alt="Avatar profil" className="w-full h-full object-cover rounded-full" /> : userInitials}
                </div>
                <div className="fa-sidebar-user-info">
                    <div className="fa-sidebar-user-name">{userName}</div>
                    <div className="fa-sidebar-user-role">{userRoleLabel}</div>
                </div>
                <button
                    onClick={handleLogout}
                    className="fa-sidebar-logout"
                    title="Logout"
                >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                        <polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
                    </svg>
                </button>
            </div>
        </aside>
    )
}