import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Dashboard from './Dashboard'

// Mock pentru navigare
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

// Variabile pentru controlul stării globale în teste
let currentToken: string | null = null;
const mockLogout = vi.fn()

vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector({
        logout: mockLogout,
        token: currentToken
    }),
}))

// Mock pentru KidDashboard (ca să nu încărcăm logica lui aici)
vi.mock('./KidDashboard', () => ({
    default: () => <div data-testid="kid-dashboard">Kid Dashboard View</div>
}))

describe('Dashboard Component - Coverage Optimization', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        currentToken = null // Resetăm token-ul la Parent default
    })

    const renderComponent = () => render(<BrowserRouter><Dashboard /></BrowserRouter>)

    it('1. Randează Parent View când rolul este Parent (sau token-ul lipsește)', () => {
        renderComponent()
        expect(screen.getByText(/Bine ai revenit,/i)).toBeInTheDocument()
    })

    it('2. Randează KidDashboard când rolul din JWT este "Child"', () => {
        // Payload base64 pentru {"role": "Child"}
        currentToken = "header.eyJyb2xlIjogIkNoaWxkIn0=.signature";
        renderComponent()
        expect(screen.getByTestId('kid-dashboard')).toBeInTheDocument()
    })

    it('3. Tratează eroarea de parsare a token-ului malformat (Catch Block)', () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
        currentToken = "invalid.token.string"; // Va crăpa la atob sau JSON.parse

        renderComponent()

        expect(consoleSpy).toHaveBeenCalledWith("Eroare la parsarea JWT-ului:", expect.any(Error))
        // Ar trebui să facă fallback la "Parent" și să randeze dashboard-ul normal
        expect(screen.getByText(/Bine ai revenit,/i)).toBeInTheDocument()
        consoleSpy.mockRestore()
    })

    it('4. Execută logout corect', () => {
        renderComponent()
        const logoutButton = screen.getByTitle('Logout')
        fireEvent.click(logoutButton)
        expect(mockLogout).toHaveBeenCalled()
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
    })

    describe('5. Validare Navigare Completă', () => {
        it('navighează la Home când se apasă pe Logo', () => {
            renderComponent()
            fireEvent.click(screen.getByText('FamilyAgent'))
            expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
        })

        it('navighează la Expenses din cardul de Sumar', () => {
            renderComponent()
            fireEvent.click(screen.getByText('CHELTUIELI LUNA ACEASTA'))
            expect(mockNavigate).toHaveBeenCalledWith('/expenses')
        })

        it('navighează la Reports din cardul de Tranzacții', () => {
            renderComponent()
            fireEvent.click(screen.getByText('TRANZACȚII'))
            expect(mockNavigate).toHaveBeenCalledWith('/reports')
        })

        it('navighează la Family din Acțiuni Rapide', () => {
            renderComponent()
            fireEvent.click(screen.getByText('Membri familie'))
            expect(mockNavigate).toHaveBeenCalledWith('/family')
        })

        it('navighează la Add Expense', () => {
            renderComponent()
            fireEvent.click(screen.getByText('Adaugă cheltuială'))
            expect(mockNavigate).toHaveBeenCalledWith('/add-expense')
        })
        it('6. Testează restul de click-uri pentru acoperire 100%', () => {
            renderComponent()

            // Cardul "TOTAL CHELTUIELI" (duce tot la /expenses)
            fireEvent.click(screen.getByText('TOTAL CHELTUIELI'))
            expect(mockNavigate).toHaveBeenCalledWith('/expenses')

            // Cardul "Vezi cheltuielile"
            fireEvent.click(screen.getByText('Vezi cheltuielile'))
            expect(mockNavigate).toHaveBeenCalledWith('/expenses')

            // Cardul "Evoluție cheltuieli" (duce la /reports)
            fireEvent.click(screen.getByText('Evoluție cheltuieli'))
            expect(mockNavigate).toHaveBeenCalledWith('/reports')
        })

        it('7. Testează fallback-ul de rol (când payload-ul nu are cheia "role")', () => {
            // Payload valid, dar fără "role": {"name": "Edi"}
            currentToken = "header.eyJuYW1lIjogIkVkaSJ9.signature"
            renderComponent()

            // Ar trebui să pună 'Parent' implicit și să randeze dashboard-ul mare
            expect(screen.getByText(/Bine ai revenit,/i)).toBeInTheDocument()
        })
    })
})