import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
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

describe('Sidebar Component - 100% Coverage Hunt', () => {
    const mockLogout = vi.fn()

    beforeEach(() => {
        vi.clearAllMocks()
        // Configurare implicită pentru auth store
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: null, logout: mockLogout }
            return selector(state)
        })
    })

    const createMockToken = (role?: string, email?: string) => {
        const payload = { role, sub: email }
        return `header.${btoa(JSON.stringify(payload))}.signature`
    }

    it('1. Randează meniul complet pentru Părinte (Initiale si Label)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: createMockToken('Parent', 'eduard@test.com'), logout: mockLogout }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Rapoarte')).toBeInTheDocument()
        expect(screen.getByText('Părinte · Activ')).toBeInTheDocument()
        expect(screen.getByText('ED')).toBeInTheDocument()
    })

    it('2. Randează meniul restrâns pentru Copil (Gradient Avatar)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: createMockToken('Child', 'andrei@test.com'), logout: mockLogout }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Cheltuielile mele')).toBeInTheDocument()
        expect(screen.queryByText('Rapoarte')).not.toBeInTheDocument()
        expect(screen.getByText('Copil · Activ')).toBeInTheDocument()

        const avatar = screen.getByText('AN')
        expect(avatar).toHaveStyle('background: linear-gradient(135deg, #B5956A, #D4B896)')
    })

    it('3. Gestionează corect un token malformat (Ramura Catch)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            // Token invalid pentru a forța eroarea în btoa/JSON.parse
            const state = { token: 'header.invalid-json.signature', logout: mockLogout }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Eduard P.')).toBeInTheDocument()
    })

    it('4. Gestionează token fără email/sub (Ramura missing sub)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: createMockToken('Parent', undefined), logout: mockLogout }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('ED')).toBeInTheDocument()
    })

    it('5. Marchează ruta activă folosind startsWith (Sub-rute)', () => {
        vi.mocked(useLocation).mockReturnValue({ pathname: '/expenses/123', state: null, key: '', search: '', hash: '' }as any)

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const expensesBtn = screen.getByRole('button', { name: /cheltuieli/i })
        expect(expensesBtn).toHaveClass('active')
    })

    it('6. Revine la dashboard dacă ruta nu este găsită (Default activeId)', () => {
        vi.mocked(useLocation).mockReturnValue({ pathname: '/ruta-inexistenta', state: null, key: '', search: '', hash: '' }as any)

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const dashboardBtn = screen.getByRole('button', { name: /dashboard/i })
        expect(dashboardBtn).toHaveClass('active')
    })

    it('7. Execută delogarea completă', () => {
        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const logoutBtn = screen.getByTitle('Logout')
        fireEvent.click(logoutBtn)

        expect(mockLogout).toHaveBeenCalled()
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
    })
})