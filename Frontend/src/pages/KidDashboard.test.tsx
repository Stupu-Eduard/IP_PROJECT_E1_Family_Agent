import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import KidDashboard from './KidDashboard'

// 1. Mocking pentru navigare (React Router)
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    }
})

// 2. Mocking pentru starea de autentificare (Zustand)
const mockLogout = vi.fn()
vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector({ logout: mockLogout }),
}))

describe('KidDashboard Component (Interfață Adaptivă Copii)', () => {
    // Resetăm funcțiile mock înainte de fiecare test
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderComponent = () => {
        return render(
            <BrowserRouter>
                <KidDashboard />
            </BrowserRouter>
        )
    }

    it('ar trebui să randeze mesajul de întâmpinare și soldul disponibil', () => {
        renderComponent()

        // Validăm prezența textelor cheie
        expect(screen.getByText(/Salut, Andrei!/i)).toBeInTheDocument()
        expect(screen.getByText('Sold Disponibil')).toBeInTheDocument()
        expect(screen.getByText('45')).toBeInTheDocument() // Soldul
    })

    it('ar trebui să navigheze către modulul OCR la apăsarea butonului principal', () => {
        renderComponent()

        // Căutăm butonul de scanare
        const scanButton = screen.getByText('Scanează Bonul')
        fireEvent.click(scanButton)

        // Validăm că funcția navigate a fost apelată cu ruta corectă
        expect(mockNavigate).toHaveBeenCalledWith('/scan-receipt')
        expect(mockNavigate).toHaveBeenCalledTimes(1)
    })

    it('ar trebui să navigheze către grupul familiei la apăsarea cardului aferent', () => {
        renderComponent()

        // Căutăm butonul grupului familial
        const familyButton = screen.getByText('Grupul Familiei')
        // Pentru că textul este în interiorul butonului, dăm click pe containerul părinte care are evenimentul
        fireEvent.click(familyButton.closest('button')!)

        // Validăm rutarea
        expect(mockNavigate).toHaveBeenCalledWith('/family')
    })

    it('ar trebui să apeleze funcția de logout și să redirecționeze la login', () => {
        renderComponent()

        // Căutăm butonul de ieșire
        const logoutButton = screen.getByText(/Ieși/i)
        fireEvent.click(logoutButton)

        // Validăm logica de deconectare
        expect(mockLogout).toHaveBeenCalledTimes(1)
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
    })
})