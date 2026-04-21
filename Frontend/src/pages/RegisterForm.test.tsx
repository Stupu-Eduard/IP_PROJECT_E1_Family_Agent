import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import RegisterForm from './RegisterForm'

// Mocking pentru React Router
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return {
        ...actual,
        useNavigate: () => mockNavigate,
        Link: ({ children, to }: { children: React.ReactNode; to: string }) => <a href={to}>{children}</a>
    }
})

// Mocking pentru Zustand Auth Store
const mockLogin = vi.fn()
vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector({
        login: mockLogin,
        isAuthenticated: false,
        token: null
    }),
}))

// Mocking pentru utilitarul JWT
vi.mock('../utils/jwt', () => ({
    isTokenExpired: vi.fn(() => false)
}))

describe('RegisterForm Component', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderComponent = () => render(<BrowserRouter><RegisterForm /></BrowserRouter>)

    it('ar trebui să randeze toate câmpurile formularului', () => {
        renderComponent()

        expect(screen.getByPlaceholderText('Nume Complet')).toBeInTheDocument()
        expect(screen.getByPlaceholderText('Email')).toBeInTheDocument()
        expect(screen.getByPlaceholderText('Parolă (Min. 8 caractere)')).toBeInTheDocument()
        expect(screen.getByPlaceholderText('Confirmare Parolă')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: /Creează Contul/i })).toBeInTheDocument()
    })

    it('ar trebui să afișeze eroare dacă parolele nu coincid', async () => {
        renderComponent()

        fireEvent.change(screen.getByPlaceholderText('Nume Complet'), { target: { value: 'Ion Popescu' } })
        fireEvent.change(screen.getByPlaceholderText('Email'), { target: { value: 'ion@example.com' } })
        fireEvent.change(screen.getByPlaceholderText('Parolă (Min. 8 caractere)'), { target: { value: 'parola123' } })
        fireEvent.change(screen.getByPlaceholderText('Confirmare Parolă'), { target: { value: 'parolaGRESITA' } })

        await act(async () => {
            // Forțăm submit-ul pe formular pentru a declanșa validarea Yup în mod sigur
            fireEvent.submit(screen.getByRole('button', { name: /Creează Contul/i }).closest('form')!)
        })

        await waitFor(() => {
            expect(screen.getByText('Parolele nu coincid.')).toBeInTheDocument()
        })
    })

    it('ar trebui să afișeze eroare (HTTP 409) la utilizarea unui email duplicat', async () => {
        renderComponent()

        fireEvent.change(screen.getByPlaceholderText('Nume Complet'), { target: { value: 'Test User' } })
        fireEvent.change(screen.getByPlaceholderText('Email'), { target: { value: 'test@example.com' } })
        fireEvent.change(screen.getByPlaceholderText('Parolă (Min. 8 caractere)'), { target: { value: 'parola123' } })
        fireEvent.change(screen.getByPlaceholderText('Confirmare Parolă'), { target: { value: 'parola123' } })

        await act(async () => {
            fireEvent.submit(screen.getByRole('button', { name: /Creează Contul/i }).closest('form')!)
        })

        await waitFor(() => {
            expect(screen.getByText(/Acest email este deja asociat unui cont/i)).toBeInTheDocument()
        }, { timeout: 2500 })
    })

    it('ar trebui să proceseze înregistrarea cu succes și să navigheze la dashboard', async () => {
        renderComponent()

        fireEvent.change(screen.getByPlaceholderText('Nume Complet'), { target: { value: 'User Nou' } })
        fireEvent.change(screen.getByPlaceholderText('Email'), { target: { value: 'nou@example.com' } })
        fireEvent.change(screen.getByPlaceholderText('Parolă (Min. 8 caractere)'), { target: { value: 'parola123' } })
        fireEvent.change(screen.getByPlaceholderText('Confirmare Parolă'), { target: { value: 'parola123' } })

        await act(async () => {
            fireEvent.submit(screen.getByRole('button', { name: /Creează Contul/i }).closest('form')!)
        })

        await waitFor(() => {
            expect(mockLogin).toHaveBeenCalledTimes(1)
            expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true })
        }, { timeout: 2500 })
    })
})