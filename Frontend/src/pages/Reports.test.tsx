import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Reports from './Reports'

// Mock pentru navigare
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

const mockLogout = vi.fn()
vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector({ logout: mockLogout }),
}))

// Mock Recharts (ResponsiveContainer face probleme în JSDOM)
vi.mock('recharts', async () => {
    const actual = await vi.importActual('recharts')
    return {
        ...actual,
        ResponsiveContainer: ({ children }: any) => <div>{children}</div>,
    }
})

describe('Reports Component - Full Exam Coverage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.useFakeTimers() // Necesar pentru a testa starea de Loading (setTimeout)
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    const renderComponent = () => render(<BrowserRouter><Reports /></BrowserRouter>)

    it('1. Navigare: Ar trebui să se întoarcă la Dashboard la click pe logo și butonul înapoi', () => {
        renderComponent()

        // Click pe Logo
        fireEvent.click(screen.getByText('FamilyAgent'))
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')

        // Click pe butonul de Back (săgeata)
        const backBtn = screen.getByRole('button', { name: '' }) // Butonul cu ArrowLeft
        fireEvent.click(backBtn)
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
    })

    it('2. Logout: Ar trebui să execute logout și să redirecționeze', () => {
        renderComponent()
        fireEvent.click(screen.getByText('Logout'))
        expect(mockLogout).toHaveBeenCalled()
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
    })

    it('3. Time Ranges: Ar trebui să schimbe intervalul și să închidă datele custom', () => {
        renderComponent()

        // Deschidem intervalul custom mai întâi
        fireEvent.click(screen.getByText('Interval Custom'))
        expect(screen.getByPlaceholderText('ex: 12/04/2026')).toBeInTheDocument()

        // Click pe 1W (ar trebui să schimbe starea și să închidă picker-ul)
        fireEvent.click(screen.getByText('1W'))
        expect(screen.queryByPlaceholderText('ex: 12/04/2026')).not.toBeInTheDocument()
    })

    it('4. Validare: Ar trebui să afișeze eroare de cronologie (Start > End)', () => {
        renderComponent()
        fireEvent.click(screen.getByText('Interval Custom'))

        const startInput = screen.getByPlaceholderText('ex: 12/04/2026')
        const endInput = screen.getByPlaceholderText('ex: 20/04/2026')

        // Introducem date valide ca format, dar greșite ca ordine
        fireEvent.change(startInput, { target: { value: '25/04/2026' } })
        fireEvent.change(endInput, { target: { value: '10/04/2026' } })

        expect(screen.getByText(/Eroare: Data de început trebuie să fie înainte de data de sfârșit/i)).toBeInTheDocument()
        expect(screen.getByText('Aplică')).toBeDisabled()
    })

    it('5. Succes: Ar trebui să aplice intervalul custom corect', () => {
        // Extragem 'container' pentru a putea folosi querySelector pe clase CSS
        const { container } = renderComponent()

        fireEvent.click(screen.getByText('Interval Custom'))

        const startInput = screen.getByPlaceholderText('ex: 12/04/2026')
        const endInput = screen.getByPlaceholderText('ex: 20/04/2026')

        fireEvent.change(startInput, { target: { value: '01/04/2026' } })
        fireEvent.change(endInput, { target: { value: '10/04/2026' } })

        const applyBtn = screen.getByText('Aplică')
        fireEvent.click(applyBtn)

        // Verificăm prezența spinner-ului prin clasa sa de animație
        const spinner = container.querySelector('.animate-spin')
        expect(spinner).toBeInTheDocument()

        // Sărim peste cele 800ms
        act(() => {
            vi.advanceTimersByTime(800)
        })

        // Verificăm că spinner-ul a dispărut
        expect(container.querySelector('.animate-spin')).not.toBeInTheDocument()
    })

    it('6. Reset: Ar trebui să curețe input-urile la click pe butonul de închidere (X)', () => {
        renderComponent()
        fireEvent.click(screen.getByText('Interval Custom'))

        const startInput = screen.getByPlaceholderText('ex: 12/04/2026')
        fireEvent.change(startInput, { target: { value: '10/04/2026' } })

        const closeBtn = screen.getByTitle('Anulează')
        fireEvent.click(closeBtn)

        expect(screen.queryByPlaceholderText('ex: 12/04/2026')).not.toBeInTheDocument()

        // Redeschidem să verificăm dacă s-au șters valorile
        fireEvent.click(screen.getByText('Interval Custom'))
        expect(screen.getByPlaceholderText('ex: 12/04/2026')).toHaveValue('')
    })

    it('7. Edge Case: Ar trebui să ignore datele inexistente (ex: 31 februarie)', () => {
        renderComponent()
        fireEvent.click(screen.getByText('Interval Custom'))
        const startInput = screen.getByPlaceholderText('ex: 12/04/2026')

        // 31/02/2026 nu există
        fireEvent.change(startInput, { target: { value: '31/02/2026' } })
        expect(screen.getByText(/Te rugăm să introduci datele conform formatului/i)).toBeInTheDocument()
    })
})