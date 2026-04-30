import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import Sidebar from './Sidebar'
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

describe('Sidebar Component', () => {
    const mockLogout = vi.fn()

    beforeEach(() => {
        vi.clearAllMocks()
        // Setup default auth store behavior
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: null, logout: mockLogout }
            return selector(state)
        })
    })

    const createMockToken = (role: string, email: string) => {
        const payload = { role, sub: email }
        return `header.${btoa(JSON.stringify(payload))}.signature`
    }

    it('1. Randează meniul complet pentru Părinte', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: createMockToken('Parent', 'eduard@test.com'), logout: mockLogout }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Rapoarte')).toBeInTheDocument()
        expect(screen.getByText('Hartă Live')).toBeInTheDocument()
        expect(screen.getByText('Părinte · Activ')).toBeInTheDocument()
        expect(screen.getByText('ED')).toBeInTheDocument() // Inițiale corecte
    })

    it('2. Randează meniul restrâns pentru Copil', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = { token: createMockToken('Child', 'andrei@test.com'), logout: mockLogout }
            return selector(state)
        })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        expect(screen.getByText('Cheltuielile mele')).toBeInTheDocument()
        expect(screen.queryByText('Rapoarte')).not.toBeInTheDocument() // Nu există pentru copil
        expect(screen.getByText('Copil · Activ')).toBeInTheDocument()
        expect(screen.getByText('AN')).toBeInTheDocument()
    })

    it('3. Marchează ruta activă corect', () => {
        vi.mocked(useLocation).mockReturnValueOnce({ pathname: '/expenses', state: null, key: '', search: '', hash: '' })

        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const expensesLink = screen.getByRole('button', { name: /cheltuieli/i })
        expect(expensesLink).toHaveClass('active')
    })

    it('4. Navighează la ruta specifică la click pe item', () => {
        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const familyLink = screen.getByRole('button', { name: /familie/i })
        fireEvent.click(familyLink)

        expect(mockNavigate).toHaveBeenCalledWith('/family')
    })

    it('5. Execută delogarea și redirectează la login', () => {
        render(<MemoryRouter><Sidebar /></MemoryRouter>)

        const logoutBtn = screen.getByTitle('Logout')
        fireEvent.click(logoutBtn)

        expect(mockLogout).toHaveBeenCalled()
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
    })
})