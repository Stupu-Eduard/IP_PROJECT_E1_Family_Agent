import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import KidDashboard from './KidDashboard'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

const MOCK_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.' +
    btoa(JSON.stringify({ name: 'Sofia Popescu', userId: 5, role: 'Child' })) +
    '.signature'

vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: (s: { token: string }) => unknown) =>
        selector({ token: MOCK_TOKEN }),
}))

const MOCK_EXPENSES = [
    { id: 1, amount: '8.50',  currency: 'RON', description: 'Gustare la chioșc',    expenseDate: '2026-05-14T13:20:00', category: 'Food',      person: 'Sofia', location: null },
    { id: 2, amount: '24.00', currency: 'RON', description: 'Caiete pentru școală', expenseDate: '2026-05-13T17:05:00', category: 'Education', person: 'Sofia', location: null },
    { id: 3, amount: '12.50', currency: 'RON', description: 'Suc + apă plată',       expenseDate: '2026-05-12T09:40:00', category: 'Food',      person: 'Sofia', location: null },
]
const MOCK_BUDGET = { totalBudget: 100, totalSpent: 55, balance: 45 }

vi.mock('../services/expenses', () => ({
    fetchExpenses: vi.fn().mockResolvedValue(MOCK_EXPENSES),
}))

vi.mock('../services/api', () => ({
    api: {
        get: vi.fn().mockResolvedValue({ data: MOCK_BUDGET }),
        interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    },
}))

describe('KidDashboard', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        Object.defineProperty(navigator, 'geolocation', {
            value: { watchPosition: vi.fn(), clearWatch: vi.fn() },
            configurable: true,
        })
    })

    const renderComponent = () => render(
        <MemoryRouter><KidDashboard /></MemoryRouter>
    )

    it('afișează identitatea din token — prenume, avatar, badge', () => {
        renderComponent()
        expect(screen.getByText(/SESIUNE COPIL/i)).toBeInTheDocument()
        expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/Salut, Sofia/i)
        expect(screen.getByText('S')).toBeInTheDocument()
        expect(screen.getByText('Sofia P.')).toBeInTheDocument()
        expect(screen.getByText(/Cont copil/i)).toBeInTheDocument()
    })

    it('afișează luna curentă în header-ul cardului de sold', () => {
        renderComponent()
        const monthLabel = new Date().toLocaleString('ro-RO', { month: 'long' }).toUpperCase()
        expect(screen.getByText(new RegExp(`SOLD DISPONIBIL · ${monthLabel}`))).toBeInTheDocument()
    })

    it('afișează datele de buget după încărcare', async () => {
        renderComponent()
        await waitFor(() => {
            expect(screen.getByText('45.00')).toBeInTheDocument()
        })
        expect(screen.getByText(/din 100.00 RON alocați de părinți/i)).toBeInTheDocument()
        expect(screen.getByText(/Cheltuit/i)).toBeInTheDocument()
        expect(screen.getByText('55% din buget')).toBeInTheDocument()
    })

    it('afișează cheltuielile reale după încărcare', async () => {
        renderComponent()
        await waitFor(() => {
            expect(screen.getByText('Gustare la chioșc')).toBeInTheDocument()
        })
        expect(screen.getByText('Caiete pentru școală')).toBeInTheDocument()
        expect(screen.getByText('Suc + apă plată')).toBeInTheDocument()
        expect(screen.getByText('8.50')).toBeInTheDocument()
        expect(screen.getByText('24.00')).toBeInTheDocument()
        expect(screen.getByText('12.50')).toBeInTheDocument()
    })

    it('navighează corect din acțiunile rapide', () => {
        renderComponent()
        fireEvent.click(screen.getByText('Scanează un bon'))
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense')
        fireEvent.click(screen.getByText('Cheltuielile mele'))
        expect(mockNavigate).toHaveBeenCalledWith('/expenses')
        fireEvent.click(screen.getByText('Familia mea'))
        expect(mockNavigate).toHaveBeenCalledWith('/family')
    })

    it('navighează la /expenses la click pe rândul de cheltuială', async () => {
        renderComponent()
        await waitFor(() => screen.getByText('Gustare la chioșc'))
        const row = screen.getByText('Gustare la chioșc').closest('.row-clickable')
        if (row) fireEvent.click(row)
        expect(mockNavigate).toHaveBeenCalledWith('/expenses')
    })

    it('afișează butonul "Vezi tot" și navighează', () => {
        renderComponent()
        fireEvent.click(screen.getByText(/Vezi tot →/i))
        expect(mockNavigate).toHaveBeenCalledWith('/expenses')
    })

    it('afișează secțiunea obiectiv cu mesaj coming soon', () => {
        renderComponent()
        expect(screen.getByText(/OBIECTIVUL MEU/i)).toBeInTheDocument()
        expect(screen.getByText(/Disponibil în curând/i)).toBeInTheDocument()
    })

    it('afișează mesaj gol când nu există cheltuieli', async () => {
        const { fetchExpenses } = await import('../services/expenses')
        vi.mocked(fetchExpenses).mockResolvedValueOnce([])
        renderComponent()
        await waitFor(() => {
            expect(screen.getByText(/Nicio cheltuială înregistrată/i)).toBeInTheDocument()
        })
    })
})
