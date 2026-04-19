import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import LoginForm from './LoginForm'

// 1. Mocking pentru React Router
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return {
        ...actual,
        useNavigate: () => mockNavigate,
        Navigate: ({ to }: { to: string }) => <div data-testid="navigate-component">{to}</div>
    }
})

// 2. Mocking pentru Zustand Auth Store
const mockLogin = vi.fn()
vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector({
        login: mockLogin,
        isAuthenticated: false,
        token: null
    }),
}))

// 3. Mocking pentru utilitarul JWT
vi.mock('../utils/jwt', () => ({
    isTokenExpired: vi.fn(() => false)
}))

describe('LoginForm Component (Securitate & Autentificare)', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderComponent = () => render(<BrowserRouter><LoginForm /></BrowserRouter>)

    it('ar trebui să randeze formularul cu toate câmpurile necesare', () => {
        renderComponent()

        expect(screen.getByPlaceholderText('username@exemplu.com')).toBeInTheDocument()
        expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: /Intră în cont/i })).toBeInTheDocument()
    })

    it('ar trebui să afișeze eroare de validare (Yup) dacă se apasă submit cu câmpuri goale', async () => {
        renderComponent()

        const submitButton = screen.getByRole('button', { name: /Intră în cont/i })

        // Învelim click-ul în act() pentru a permite rularea validării asincrone din Yup
        await act(async () => {
            fireEvent.click(submitButton)
        })

        // Validăm că apare div-ul de eroare (căutăm o parte din textul returnat de Yup)
        await waitFor(() => {
            expect(screen.getByText(/este obligatorie/i)).toBeInTheDocument()
        })
    })

    it('ar trebui să proceseze login-ul și să navigheze la dashboard pe date valide', async () => {
        renderComponent()

        const emailInput = screen.getByPlaceholderText('username@exemplu.com')
        const passwordInput = screen.getByPlaceholderText('••••••••')
        const submitButton = screen.getByRole('button', { name: /Intră în cont/i })

        // 1. Introducem date valide
        fireEvent.change(emailInput, { target: { value: 'test@example.com' } })
        fireEvent.change(passwordInput, { target: { value: 'password123' } })

        // 2. Apăsăm submit în interiorul act()
        await act(async () => {
            fireEvent.click(submitButton)
        })

        // 3. Așteptăm rezolvarea timeout-ului de 1.5s din codul tău real
        // Setăm timeout-ul de test la 2.5s pentru a oferi timp suficient promisiunii
        await waitFor(() => {
            expect(mockLogin).toHaveBeenCalledTimes(1)
            expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true })
        }, { timeout: 2500 })
    })
})