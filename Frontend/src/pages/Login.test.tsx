import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import Login from './LoginForm'
import { useAuthStore } from '../store/authStore'

vi.mock('../store/authStore', () => ({
    useAuthStore: vi.fn()
}))

vi.mock('../utils/jwt', () => ({
    isTokenExpired: vi.fn(() => false)
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return {
        ...actual,
        useNavigate: () => mockNavigate
    }
})

describe('Login Component - 100% Coverage', () => {
    const mockLogin = vi.fn()

    const setupUnauthenticated = () => {
        ;(useAuthStore as any).mockImplementation((selector: any) =>
            selector({ token: null, isAuthenticated: false, login: mockLogin })
        )
    }

    beforeEach(() => {
        vi.clearAllMocks()
        setupUnauthenticated()
    })

    const renderComponent = () => render(
        <MemoryRouter initialEntries={['/login']}>
            <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/dashboard" element={<div>Dashboard Page</div>} />
            </Routes>
        </MemoryRouter>
    )

    // Helper: găsește div-ul de eroare roșu din container
    const getErrorDiv = (container: HTMLElement) =>
        container.querySelector('[style*="254, 242, 242"]')

    // ── Test 1: Redirect dacă deja autentificat ──────────────────────────────
    it('1. Redirecționează către /dashboard dacă userul e deja autentificat', () => {
        ;(useAuthStore as any).mockImplementation((selector: any) =>
            selector({
                token: 'valid.token.here',
                isAuthenticated: true,
                login: mockLogin,
            })
        )

        render(
            <MemoryRouter initialEntries={['/login']}>
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path="/dashboard" element={<div>Dashboard Page</div>} />
                </Routes>
            </MemoryRouter>
        )

        expect(screen.getByText('Dashboard Page')).toBeInTheDocument()
    })

    // ── Test 3: Eroare email invalid ─────────────────────────────────────────
    it('3. Arată eroare pentru format email invalid', async () => {
        const { container } = renderComponent()
        const emailInput = screen.getByPlaceholderText(/username@exemplu.com/i)
        const passInput  = screen.getByPlaceholderText(/••••••••/i)
        const form = emailInput.closest('form')!

        // Schimbăm type-ul ca JSDOM să nu valideze formatul emailului nativ
        emailInput.setAttribute('type', 'text')
        fireEvent.change(emailInput, { target: { value: 'email-incorect' } })
        fireEvent.change(passInput,  { target: { value: 'password123' } })
        fireEvent.submit(form)

        await waitFor(() => {
            const errorDiv = getErrorDiv(container)
            expect(errorDiv).toBeInTheDocument()
            expect(errorDiv?.textContent).toContain('Te rugăm să introduci o adresă de email validă.')
        })
    })

    // ── Test 4: Eroare parolă prea scurtă ────────────────────────────────────
    it('4. Arată eroare pentru parolă prea scurtă', async () => {
        const { container } = renderComponent()
        const emailInput = screen.getByPlaceholderText(/username@exemplu.com/i)
        const passInput  = screen.getByPlaceholderText(/••••••••/i)
        const form = emailInput.closest('form')!

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } })
        fireEvent.change(passInput,  { target: { value: '123' } })
        fireEvent.submit(form)

        await waitFor(() => {
            const errorDiv = getErrorDiv(container)
            expect(errorDiv).toBeInTheDocument()
            expect(errorDiv?.textContent).toContain('Parola trebuie să aibă minimum 6 caractere.')
        })
    })

    // ── Test 5: Login cu succes - cont Părinte (ramura 'Parent') ─────────────
    it('5. Login cu succes pentru cont Părinte (Loading -> Dashboard)', async () => {
        renderComponent()
        const emailInput = screen.getByPlaceholderText(/username@exemplu.com/i)
        const passInput  = screen.getByPlaceholderText(/••••••••/i)
        const form = emailInput.closest('form')!

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } })
        fireEvent.change(passInput,  { target: { value: 'password123' } })
        fireEvent.submit(form)

        expect(await screen.findByText(/Se procesează/i)).toBeInTheDocument()
        expect(emailInput).toBeDisabled()

        await waitFor(() => {
            expect(mockLogin).toHaveBeenCalled()
            expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true })
        }, { timeout: 3000 })
    })

    // ── Test 6: Login cu cont Copil (ramura assignedRole = 'Child') ──────────
    it('6. Login cu succes pentru cont Copil (acoperă ramura Child)', async () => {
        renderComponent()
        const emailInput = screen.getByPlaceholderText(/username@exemplu.com/i)
        const passInput  = screen.getByPlaceholderText(/••••••••/i)
        const form = emailInput.closest('form')!

        fireEvent.change(emailInput, { target: { value: 'copil@example.com' } })
        fireEvent.change(passInput,  { target: { value: 'password123' } })
        fireEvent.submit(form)

        await waitFor(() => {
            expect(mockLogin).toHaveBeenCalled()
            expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true })
        }, { timeout: 3000 })
    })

    // ── Test 7: Eroare neașteptată (ramura else din catch) ───────────────────
    it('7. Afișează eroare neașteptată când se aruncă o valoare non-Error', async () => {
        const { container } = renderComponent()
        const emailInput = screen.getByPlaceholderText(/username@exemplu.com/i)
        const passInput  = screen.getByPlaceholderText(/••••••••/i)
        const form = emailInput.closest('form')!

        // mockLogin aruncă un string — nu e instanceof Error, nici ValidationError
        mockLogin.mockImplementationOnce(() => { throw 'eroare_string' })

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } })
        fireEvent.change(passInput,  { target: { value: 'password123' } })
        fireEvent.submit(form)

        await waitFor(() => {
            const errorDiv = getErrorDiv(container)
            expect(errorDiv).toBeInTheDocument()
            expect(errorDiv?.textContent).toContain('A apărut o eroare neașteptată.')
        }, { timeout: 3000 })
    })

    // ── Test 8: Navigare links ────────────────────────────────────────────────
    it('8. Navighează către Register și Reset', () => {
        renderComponent()
        expect(screen.getByText(/Înregistrează-te/i).closest('a')).toHaveAttribute('href', '/register')
        expect(screen.getByText(/AI UITAT\?/i).closest('a')).toHaveAttribute('href', '/forgot-password')
    })
})