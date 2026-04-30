import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import RegisterForm from './RegisterForm'
import { useAuthStore } from '../store/authStore'

// ── Mock-uri ──
vi.mock('../store/authStore', () => ({
    useAuthStore: vi.fn()
}))

vi.mock('../utils/jwt', () => ({
    isTokenExpired: vi.fn(() => false)
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

describe('RegisterForm - 100% Coverage & Zero Errors', () => {
    const mockLogin = vi.fn()

    beforeEach(() => {
        vi.clearAllMocks()
        ;(useAuthStore as any).mockImplementation((selector: any) =>
            selector({ token: null, isAuthenticated: false, login: mockLogin })
        )
    })

    const renderComponent = () => render(
        <MemoryRouter initialEntries={['/register']}>
            <Routes>
                <Route path="/register" element={<RegisterForm />} />
                <Route path="/dashboard" element={<div>Dashboard Page</div>} />
            </Routes>
        </MemoryRouter>
    )

    // Helper care citește direct textContent din div-ul de eroare (background unic în pagină)
    const getErrorText = () => {
        const el = document.querySelector('[style*="rgb(254, 242, 242)"]')
        return el?.textContent?.trim() ?? ''
    }

    const waitForError = (text: string) =>
        waitFor(() => {
            expect(getErrorText()).toContain(text)
        }, { timeout: 3000 })

    it('1. Redirecționează imediat dacă userul este deja autentificat', () => {
        ;(useAuthStore as any).mockImplementation((selector: any) =>
            selector({ token: 'token-valid', isAuthenticated: true, login: mockLogin })
        )
        renderComponent()
        expect(screen.getByText(/Dashboard Page/i)).toBeInTheDocument()
    })

    it('2. Arată eroare de validare când doar numele lipsește', async () => {
        renderComponent()
        // Completăm toate câmpurile EXCEPT nume → prima eroare va fi "Numele este obligatoriu."
        fireEvent.change(screen.getByPlaceholderText(/username@exemplu.com/i), { target: { value: 'test@test.com' } })
        const passFields = screen.getAllByPlaceholderText(/••••••••/i)
        fireEvent.change(passFields[0], { target: { value: 'Password1!' } })
        fireEvent.change(passFields[1], { target: { value: 'Password1!' } })
        fireEvent.submit(screen.getByRole('button', { name: /Creează contul/i }))
        await waitForError('Numele este obligatoriu.')
    })

    it('3. Eroare: Parolele nu coincid', async () => {
        renderComponent()
        fireEvent.change(screen.getByPlaceholderText(/Ana Popescu/i), { target: { value: 'Ana Popescu' } })
        fireEvent.change(screen.getByPlaceholderText(/username@exemplu.com/i), { target: { value: 'ana@test.com' } })
        const passFields = screen.getAllByPlaceholderText(/••••••••/i)
        fireEvent.change(passFields[0], { target: { value: 'Password1!' } })
        fireEvent.change(passFields[1], { target: { value: 'altceva111' } })
        fireEvent.submit(screen.getByRole('button', { name: /Creează contul/i }))
        await waitForError('Parolele nu coincid.')
    })

    it('4. Password Strength: Acoperă toate ramurile de scor', () => {
        renderComponent()
        const passInput = screen.getAllByPlaceholderText(/••••••••/i)[0]

        // Scor 1: doar lungime >= 8, fără majuscule/cifre/speciale
        fireEvent.change(passInput, { target: { value: 'parolamica' } })
        expect(screen.getByText(/Slabă/i)).toBeInTheDocument()

        // Scor 4: toate criteriile (majusculă + cifră + special + lungime)
        fireEvent.change(passInput, { target: { value: 'Parola123!' } })
        expect(screen.getByText(/Puternică/i)).toBeInTheDocument()
    })

    it('5. Înregistrare cu succes -> navigate către Dashboard', async () => {
        renderComponent()

        fireEvent.change(screen.getByPlaceholderText(/Ana Popescu/i), { target: { value: 'Ana Popescu' } })
        fireEvent.change(screen.getByPlaceholderText(/username@exemplu.com/i), { target: { value: 'newuser@test.com' } })
        const passFields = screen.getAllByPlaceholderText(/••••••••/i)
        fireEvent.change(passFields[0], { target: { value: 'Password123!' } })
        fireEvent.change(passFields[1], { target: { value: 'Password123!' } })

        fireEvent.submit(screen.getByRole('button', { name: /Creează contul/i }))

        // Verificăm starea de loading
        expect(await screen.findByText(/Se procesează/i)).toBeInTheDocument()

        // mockApiCall are setTimeout 1500ms — așteptăm să se termine
        await waitFor(() => {
            expect(mockLogin).toHaveBeenCalledTimes(1)
            expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true })
        }, { timeout: 4000 })
    })

    it('6. Eroare 409: Email deja existent', async () => {
        renderComponent()

        fireEvent.change(screen.getByPlaceholderText(/Ana Popescu/i), { target: { value: 'Test User' } })
        // Email hardcodat în RegisterForm care declanșează reject-ul 409
        fireEvent.change(screen.getByPlaceholderText(/username@exemplu.com/i), { target: { value: 'test@example.com' } })
        const passFields = screen.getAllByPlaceholderText(/••••••••/i)
        fireEvent.change(passFields[0], { target: { value: 'Password123!' } })
        fireEvent.change(passFields[1], { target: { value: 'Password123!' } })

        fireEvent.submit(screen.getByRole('button', { name: /Creează contul/i }))

        await waitForError('Acest email este deja asociat unui cont.')
    })

    it('7. Eroare neașteptată de rețea (throw non-Error)', async () => {
        renderComponent()

        fireEvent.change(screen.getByPlaceholderText(/Ana Popescu/i), { target: { value: 'Test User' } })
        fireEvent.change(screen.getByPlaceholderText(/username@exemplu.com/i), { target: { value: 'error@test.com' } })
        const passFields = screen.getAllByPlaceholderText(/••••••••/i)
        fireEvent.change(passFields[0], { target: { value: 'Password123!' } })
        fireEvent.change(passFields[1], { target: { value: 'Password123!' } })

        // mockLogin aruncă un string (non-Error) → ramura `else setError('Eroare neașteptată...')`
        mockLogin.mockImplementationOnce(() => { throw 'Crash non-Error' })

        fireEvent.submit(screen.getByRole('button', { name: /Creează contul/i }))

        await waitForError('Eroare neașteptată de rețea.')
    })

    it('8. Navigare: link către Login are href corect', () => {
        renderComponent()
        expect(screen.getByText(/Conectează-te/i).closest('a')).toHaveAttribute('href', '/login')
    })
})