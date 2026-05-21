import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, cleanup } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Sidebar from './Sidebar.tsx'
import { useAuthStore } from '../store/authStore'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return {
        ...actual,
        useNavigate: () => mockNavigate,
        useLocation: vi.fn(() => ({ pathname: '/dashboard' }))
    }
})

vi.mock('../store/authStore', () => ({
    useAuthStore: vi.fn()
}))

// ── Helper: generare token cu name și/sau sub ─────────────────────────────
const createMockToken = (payload: Record<string, unknown>) => {
    return `header.${btoa(JSON.stringify(payload))}.signature`
}

describe('Sidebar Component', () => {
    const mockLogout = vi.fn()

    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: null, logout: mockLogout, avatarUrl: null }
            return selector(state)
        })
    })

    afterEach(() => {
        cleanup()
    })

    // ─── 1. MENIU PĂRINTE CU NUME DIN CÂMPUL NAME ─────────────────────────

    it('1. Randează meniul complet pentru Părinte cu nume din câmpul name', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Parent', name: 'Ana Popescu' }),
                logout: mockLogout,
                avatarUrl: null,
            }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Rapoarte')).toBeInTheDocument()
        expect(screen.getByText('Părinte · Activ')).toBeInTheDocument()
        // Ana Popescu → "Ana P." și inițiale "AP"
        expect(screen.getByText('Ana P.')).toBeInTheDocument()
        expect(screen.getByText('AP')).toBeInTheDocument()
    })

    // ─── 2. MENIU PĂRINTE CU NUME DIN CÂMPUL SUB (EMAIL) ─────────────────

    it('2. Extrage inițialele din email (câmpul sub) când name lipsește', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Parent', sub: 'eduard@test.com' }),
                logout: mockLogout,
                avatarUrl: null,
            }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Părinte · Activ')).toBeInTheDocument()
        // "eduard@test.com" → firstName = "eduard@test.com", inițiale = "ED"
        expect(screen.getByText('ED')).toBeInTheDocument()
    })

    // ─── 3. MENIU COPIL ───────────────────────────────────────────────────

    it('3. Randează meniul restrâns pentru Copil cu gradient avatar', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Child', name: 'Andrei Popescu' }),
                logout: mockLogout,
                avatarUrl: null,
            }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Cheltuielile mele')).toBeInTheDocument()
        expect(screen.queryByText('Rapoarte')).not.toBeInTheDocument()
        expect(screen.getByText('Copil · Activ')).toBeInTheDocument()
        expect(screen.getByText('Andrei P.')).toBeInTheDocument()
        expect(screen.getByText('AP')).toBeInTheDocument()

        const avatar = screen.getByText('AP')
        expect(avatar).toHaveStyle('background: linear-gradient(135deg, #B5956A, #D4B896)')
    })

    // ─── 4. TOKEN MALFORMAT (CATCH) ───────────────────────────────────────

    it('4. Gestionează corect un token malformat (ramura catch)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: 'header.invalid-json.signature', logout: mockLogout, avatarUrl: null }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        // Când token e invalid, afișează valorile default
        expect(screen.getByText('Utilizator')).toBeInTheDocument()
        expect(screen.getByText('Părinte · Activ')).toBeInTheDocument()
        expect(screen.getByText('U')).toBeInTheDocument()
    })

    // ─── 5. TOKEN NULL ────────────────────────────────────────────────────

    it('5. Randează valorile default când token-ul este null', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: null, logout: mockLogout, avatarUrl: null }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Utilizator')).toBeInTheDocument()
        expect(screen.getByText('Părinte · Activ')).toBeInTheDocument()
        expect(screen.getByText('U')).toBeInTheDocument()
    })

    // ─── 6. LOGOUT ────────────────────────────────────────────────────────

    it('6. Apelează logout și navighează spre /login la click pe butonul de ieșire', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Parent', name: 'Ana Popescu' }),
                logout: mockLogout,
                avatarUrl: null,
            }
            return selector(state)
        })

        const { container } = render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const logoutBtn = container.querySelector('.fa-sidebar-logout')
        if (logoutBtn) fireEvent.click(logoutBtn)

        expect(mockLogout).toHaveBeenCalledTimes(1)
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
    })

    // ─── 7. AVATAR PREDEFINIT (PersonSvg) ────────────────────────────────

    it('7. Randează PersonSvg când avatarUrl este un ID predefinit (av_f_pink)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Parent', name: 'Ana Popescu' }),
                logout: mockLogout,
                avatarUrl: 'av_f_pink',
            }
            return selector(state)
        })

        const { container } = render(<MemoryRouter><Sidebar /></MemoryRouter>)

        // PersonSvg randează un SVG — nu inițialele
        const svg = container.querySelector('svg')
        expect(svg).toBeInTheDocument()
        // Inițialele NU sunt afișate când există avatar predefinit
        expect(screen.queryByText('AP')).not.toBeInTheDocument()
    })

    // ─── 8. AVATAR REMOTE URL (img tag) ──────────────────────────────────

    it('8. Randează un img când avatarUrl este un URL remote (https)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Parent', name: 'Ana Popescu' }),
                logout: mockLogout,
                avatarUrl: 'https://res.cloudinary.com/demo/avatar.jpg',
            }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const img = screen.getByAltText('Avatar')
        expect(img).toBeInTheDocument()
        expect(img).toHaveAttribute('src', 'https://res.cloudinary.com/demo/avatar.jpg')
        // Inițialele NU sunt afișate când există imagine
        expect(screen.queryByText('AP')).not.toBeInTheDocument()
    })

    // ─── 9. NAVIGARE LA PROFIL (FOOTER CLICK) ────────────────────────────

    it('9. Click pe footer navighează spre /profile', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Parent', name: 'Ana Popescu' }),
                logout: mockLogout,
                avatarUrl: null,
            }
            return selector(state)
        })

        const { container } = render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const footerBtn = container.querySelector('.fa-sidebar-user')
        if (footerBtn) fireEvent.click(footerBtn)

        expect(mockNavigate).toHaveBeenCalledWith('/profile')
    })

    // ─── 10. FAMILIE LABEL DIN TOKEN ─────────────────────────────────────

    it('10. Afișează numele familiei din token (familyName)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Parent', name: 'Ana', familyName: 'Popescu' }),
                logout: mockLogout,
                avatarUrl: null,
            }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Familia Popescu')).toBeInTheDocument()
    })
})
