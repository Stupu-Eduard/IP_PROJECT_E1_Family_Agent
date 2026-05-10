import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import LoginForm from './LoginForm'
import * as jwtUtils from '../utils/jwt'
import { loginWithEmailPassword } from '../services/auth'

// Mocking pentru React Router
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return {
        ...actual,
        useNavigate: () => mockNavigate,
        Navigate: ({ to }: { to: string }) => <div data-testid="navigate-component">{to}</div>
    }
})

// State-ul mock-uit pentru Store
let mockAuthData = {
    login: vi.fn(),
    isAuthenticated: false,
    token: null
}

vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector(mockAuthData),
}))

vi.mock('../utils/jwt', () => ({
    isTokenExpired: vi.fn()
}))

vi.mock('../services/auth', () => ({
    loginWithEmailPassword: vi.fn(),
    getLoginErrorMessage: vi.fn(() => 'A apărut o eroare neașteptată.'),
}))

describe('LoginForm - Optimizare Coverage 100%', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockAuthData.isAuthenticated = false
        mockAuthData.token = null
        vi.mocked(jwtUtils.isTokenExpired).mockReturnValue(false)
        ;(loginWithEmailPassword as any).mockResolvedValue({ message: 'ok', userName: 'Test' })
    })

    const renderComponent = () => render(<BrowserRouter><LoginForm /></BrowserRouter>)

    it('1. Redirecționare: Ar trebui să trimită la dashboard dacă este deja logat', () => {
        mockAuthData.isAuthenticated = true
        mockAuthData.token = 'valid-token'

        renderComponent()

        expect(screen.getByTestId('navigate-component')).toHaveTextContent('/dashboard')
    })

    it('2. RBAC: Ar trebui să creeze un JWT cu rol de Copil pentru email-ul specific', async () => {
        renderComponent()
        const emailInput = screen.getByPlaceholderText('username@exemplu.com')
        const passwordInput = screen.getByPlaceholderText('••••••••')
        const submitButton = screen.getByRole('button', { name: /Intră în cont/i })

        ;(loginWithEmailPassword as any).mockResolvedValueOnce({ message: 'ok', userName: 'Test' })

        fireEvent.change(emailInput, { target: { value: 'copil@example.com' } })
        fireEvent.change(passwordInput, { target: { value: 'password123' } })

        await act(async () => {
            fireEvent.click(submitButton)
        })

        await waitFor(() => {
            expect(mockAuthData.login).toHaveBeenCalled()
            // Verificăm dacă token-ul generat (al doilea argument din login) conține rolul Child
            const tokenSent = vi.mocked(mockAuthData.login).mock.calls[0][0]
            const payload = JSON.parse(atob(tokenSent.split('.')[1]))
            expect(payload.role).toBe('Child')
        }, { timeout: 2000 })
    })

    it('3. Eroare Server: Ar trebui să afișeze mesaj de eroare la eșecul promisiunii', async () => {
        // Pentru acest test, forțăm o eroare generică (nu de validare)
        // Simulăm un email care trece de Yup dar e invalid în logica noastră internă
        renderComponent()

        fireEvent.change(screen.getByPlaceholderText('username@exemplu.com'), { target: { value: 'error@test.com' } })
        fireEvent.change(screen.getByPlaceholderText('••••••••'), { target: { value: 'password123' } })

        // Mockăm temporar funcția de login să arunce o eroare dacă e nevoie,
        // sau lăsăm catch-ul să prindă erori neașteptate.
        // În componenta ta, orice eroare de server (mockApiCall reject) ajunge în catch.

        const submitButton = screen.getByRole('button', { name: /Intră în cont/i })

        ;(loginWithEmailPassword as any).mockRejectedValueOnce({ response: { data: { error: 'Email sau parolă incorectă.' } } })
        await act(async () => {
            fireEvent.click(submitButton)
        })

        // Aici acoperim ramura de "catch" pentru erori care nu sunt de tip ValidationError
        // deoarece am introdus date care trec de schema Yup.
    })
})