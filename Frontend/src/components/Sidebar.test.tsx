import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, cleanup } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
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
            const state = { token: null, logout: mockLogout }
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
                logout: mockLogout
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
                logout: mockLogout
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
                logout: mockLogout
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
            const state = { token: 'header.invalid-json.signature', logout: mockLogout }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        // Când token e invalid, afișează valorile default
        expect(screen.getByText('Utilizator')).toBeInTheDocument()
        expect(screen.getByText('Părinte · Activ')).toBeInTheDocument()
        expect(screen.getByText('U')).toBeInTheDocument()
    })

    // ─── 5. TOKEN FĂRĂ NAME ȘI FĂRĂ SUB ──────────────────────────────────

    it('5. Gestionează token fără name și fără sub (fallback la Utilizator)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Parent' }),
                logout: mockLogout
            }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Utilizator')).toBeInTheDocument()
        expect(screen.getByText('U')).toBeInTheDocument()
    })

    // ─── 6. TOKEN NULL ────────────────────────────────────────────────────

    it('6. Gestionează token null (utilizator neautentificat)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: null, logout: mockLogout }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Utilizator')).toBeInTheDocument()
        expect(screen.getByText('Părinte · Activ')).toBeInTheDocument()
    })

    // ─── 7. NUME FĂRĂ PRENUME COMPUS (UN SINGUR CUVÂNT) ──────────────────

    it('7. Gestionează corect un nume cu un singur cuvânt', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Parent', name: 'Mihaela' }),
                logout: mockLogout
            }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Mihaela')).toBeInTheDocument()
        // Un singur cuvânt → inițiale = primele 2 litere
        expect(screen.getByText('MI')).toBeInTheDocument()
    })

    // ─── 8. RUTĂ ACTIVĂ CU SUB-RUTE ──────────────────────────────────────

    it('8. Marchează ruta activă folosind startsWith (sub-rute)', () => {
        vi.mocked(useLocation).mockReturnValue({
            pathname: '/expenses/123', state: null, key: '', search: '', hash: ''
        } as any)

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const expensesBtn = screen.getByRole('button', { name: /cheltuieli/i })
        expect(expensesBtn).toHaveClass('active')
    })

    // ─── 9. NAVIGARE HARTĂ LIVE ───────────────────────────────────────────

    it('9. Navighează corect la Hartă Live', () => {
        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        fireEvent.click(screen.getByRole('button', { name: /hartă live/i }))

        expect(mockNavigate).toHaveBeenCalledWith('/expenses/all-map')
    })

    // ─── 10. RUTĂ INEXISTENTĂ → DEFAULT DASHBOARD ─────────────────────────

    it('10. Revine la dashboard dacă ruta nu este găsită (default activeId)', () => {
        vi.mocked(useLocation).mockReturnValue({
            pathname: '/ruta-inexistenta', state: null, key: '', search: '', hash: ''
        } as any)

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const dashboardBtn = screen.getByRole('button', { name: /dashboard/i })
        expect(dashboardBtn).toHaveClass('active')
    })

    // ─── 11. LOGOUT ───────────────────────────────────────────────────────

    it('11. Execută delogarea completă', () => {
        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        fireEvent.click(screen.getByTitle('Logout'))

        expect(mockLogout).toHaveBeenCalled()
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
    })

    // ─── 12. NAVIGARE TOATE ITEM-URILE ────────────────────────────────────

    it('12. Navighează corect la toate item-urile din meniu Părinte', () => {
        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        fireEvent.click(screen.getByRole('button', { name: /dashboard/i }))
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')

        fireEvent.click(screen.getByRole('button', { name: /cheltuieli$/i }))
        expect(mockNavigate).toHaveBeenCalledWith('/expenses')

        fireEvent.click(screen.getByRole('button', { name: /rapoarte/i }))
        expect(mockNavigate).toHaveBeenCalledWith('/reports')

        fireEvent.click(screen.getByRole('button', { name: /familie/i }))
        expect(mockNavigate).toHaveBeenCalledWith('/family')
    })

    // ─── 13. NAVIGARE MENIU COPIL ─────────────────────────────────────────

    it('13. Navighează corect la item-urile din meniu Copil', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: createMockToken({ role: 'Child', name: 'Andrei P.' }),
                logout: mockLogout
            }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        fireEvent.click(screen.getByRole('button', { name: /cheltuielile mele/i }))
        expect(mockNavigate).toHaveBeenCalledWith('/expenses')
    })

    // ─── 14. LOGO ȘI TITLU ────────────────────────────────────────────────

    it('14. Afișează corect logo-ul și titlul aplicației', () => {
        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Family Agent')).toBeInTheDocument()
        expect(screen.getByText('Familia Popescu')).toBeInTheDocument()
    })
})