import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import LoginForm from './LoginForm'
import { useAuthStore } from '../store/authStore'
import { loginWithEmailPassword } from '../services/auth'
import * as jwtUtils from '../utils/jwt'

// ── Mock-uri identice cu cele din fișierele existente ──────────────────────
vi.mock('../store/authStore', () => ({
    useAuthStore: vi.fn(),
}))

vi.mock('../utils/jwt', () => ({
    isTokenExpired: vi.fn(() => false),
}))

vi.mock('../services/auth', () => ({
    loginWithEmailPassword: vi.fn(),
    getLoginErrorMessage: vi.fn(() => 'A apărut o eroare neașteptată.'),
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

// ── Helper-e ────────────────────────────────────────────────────────────────
const mockLogin = vi.fn()

const setupUnauthenticated = () => {
    ;(useAuthStore as any).mockImplementation((selector: any) =>
        selector({ token: null, isAuthenticated: false, login: mockLogin }),
    )
}

const renderComponent = () =>
    render(
        <MemoryRouter initialEntries={['/login']}>
            <Routes>
                <Route path="/login" element={<LoginForm />} />
                <Route path="/dashboard" element={<div>Dashboard Page</div>} />
            </Routes>
        </MemoryRouter>,
    )

describe('LoginForm – ramuri lipsă (coverage 100%)', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        setupUnauthenticated()
        vi.mocked(jwtUtils.isTokenExpired).mockReturnValue(false)
        ;(loginWithEmailPassword as any).mockResolvedValue({
            message: 'ok',
            userName: 'Test',
            token: 'fake.jwt.token',
        })
    })

    // ── A. Token prezent DAR expirat → NU redirecționează ───────────────────
    // Ramura: `if (isAuthenticated && token && !isTokenExpired(token))` → false
    // deoarece isTokenExpired returnează true.
    it('A. Nu redirecționează dacă tokenul existent este expirat', () => {
        ;(useAuthStore as any).mockImplementation((selector: any) =>
            selector({ token: 'expired.token', isAuthenticated: true, login: mockLogin }),
        )
        vi.mocked(jwtUtils.isTokenExpired).mockReturnValue(true)  // ← token expirat

        renderComponent()

        // Formularul de login trebuie să fie afișat, nu dashboard-ul
        expect(screen.getByPlaceholderText(/username@exemplu.com/i)).toBeInTheDocument()
        expect(screen.queryByText('Dashboard Page')).not.toBeInTheDocument()
    })

    // ── B. Răspuns fără token → ramura `else` → "Token lipsă." ──────────────
    // Ramura: `if (response.token) { ... } else { setError('...Token lipsă.') }`
    it('B. Afișează "Token lipsă" când serverul returnează răspuns fără câmpul token', async () => {
        ;(loginWithEmailPassword as any).mockResolvedValueOnce({
            message: 'ok',
            userName: 'Test',
            // token lipsă intenționat
        })

        const { container } = renderComponent()
        const emailInput = screen.getByPlaceholderText(/username@exemplu.com/i)
        const passInput  = screen.getByPlaceholderText(/••••••••/i)
        const form = emailInput.closest('form')!

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } })
        fireEvent.change(passInput,  { target: { value: 'password123' } })

        await act(async () => {
            fireEvent.submit(form)
        })

        await waitFor(() => {
            // Div-ul de eroare conține mesajul despre token lipsă
            const errorDiv = container.querySelector('[style*="254, 242, 242"]') ??
                container.querySelector('[style*="FEF2F2"]') ??
                container.querySelector('[style*="#FEF2F2"]')
            // Căutăm textul direct în document dacă selector-ul de culoare nu funcționează
            expect(screen.getByText(/Token lipsă/i)).toBeInTheDocument()
        }, { timeout: 3000 })
    })

    // ── C. Câmpuri goale → eroare validare Yup (email obligatoriu) ───────────
    // Acoperă ramura `catch (err)` → `err instanceof yup.ValidationError`
    it('C. Afișează eroare validare când email-ul lipsește', async () => {
        const { container } = renderComponent()
        const form = screen.getByPlaceholderText(/username@exemplu.com/i).closest('form')!

        await act(async () => {
            fireEvent.submit(form)
        })

        await waitFor(() => {
            // Yup aruncă ValidationError → setError(err.message)
            // Mesajul poate fi "obligatorie" sau "validă" în funcție de ordinea validării
            const errorText = container.textContent ?? ''
            expect(
                errorText.includes('obligatorie') || errorText.includes('validă') || errorText.includes('minimum'),
            ).toBe(true)
        }, { timeout: 2000 })
    })

    // ── D. isLoading dezactivează câmpurile în timp ce cererea e în curs ─────
    // Acoperă branch-ul `disabled={isLoading}` pe ambele input-uri
    it('D. Câmpurile sunt dezactivate în timp ce loginul se procesează', async () => {
        let resolveLogin: ((value: any) => void) | null = null
        ;(loginWithEmailPassword as any).mockReturnValueOnce(
            new Promise((resolve) => { resolveLogin = resolve }),
        )

        renderComponent()
        const emailInput = screen.getByPlaceholderText(/username@exemplu.com/i)
        const passInput  = screen.getByPlaceholderText(/••••••••/i)
        const form = emailInput.closest('form')!

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } })
        fireEvent.change(passInput,  { target: { value: 'password123' } })
        fireEvent.submit(form)

        // Starea de loading
        expect(await screen.findByText(/Se procesează/i)).toBeInTheDocument()
        expect(emailInput).toBeDisabled()
        expect(passInput).toBeDisabled()

        // Rezolvăm promise-ul ca să nu lăsăm state updates nesupravegheate
        await act(async () => {
            resolveLogin?.({ message: 'ok', userName: 'Test', token: 'fake.jwt.token' })
        })
    })

    // ── E. Eroare non-ValidationError din loginStore (getLoginErrorMessage) ──
    // Acoperă ramura `else setError(getLoginErrorMessage(err))` în catch
    it('E. Afișează mesaj generic când loginStore aruncă o excepție non-ValidationError', async () => {
        mockLogin.mockImplementationOnce(() => {
            throw new Error('server_error')   // instanceof Error, nu ValidationError
        })

        const { container } = renderComponent()
        const emailInput = screen.getByPlaceholderText(/username@exemplu.com/i)
        const passInput  = screen.getByPlaceholderText(/••••••••/i)
        const form = emailInput.closest('form')!

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } })
        fireEvent.change(passInput,  { target: { value: 'password123' } })

        await act(async () => {
            fireEvent.submit(form)
        })

        await waitFor(() => {
            expect(screen.getByText(/A apărut o eroare neașteptată/i)).toBeInTheDocument()
        }, { timeout: 3000 })
    })
})