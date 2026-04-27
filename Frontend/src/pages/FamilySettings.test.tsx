import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import Reports from './Reports' // <--- VERIFICĂ ACEST IMPORT!

type MockAuthState = {
    logout: () => void
}

type ResponsiveContainerMockProps = {
    children?: ReactNode
}

// Mock pentru navigare
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate, MemoryRouter: actual.MemoryRouter }
})

const mockLogout = vi.fn()
vi.mock('../store/authStore', () => ({
    useAuthStore: <T,>(selector: (state: MockAuthState) => T) => selector({ logout: mockLogout }),
}))

vi.mock('recharts', async () => {
    const actual = await vi.importActual('recharts')
    return { ...actual, ResponsiveContainer: ({ children }: ResponsiveContainerMockProps) => <div>{children}</div> }
})

describe('Reports Component - Final Corrected Version', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.useFakeTimers()
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    const renderComponent = () => render(
        <MemoryRouter>
            <Reports />
        </MemoryRouter>
    )

    it('1. Ar trebui să afișeze titlul corect și butoanele de timp', () => {
        renderComponent()
        expect(screen.getByText('Evoluție Cheltuieli')).toBeInTheDocument()
        expect(screen.getByText('1M')).toBeInTheDocument()
    })

    it('2. Ar trebui să deschidă selectorul custom', () => {
        renderComponent()
        const customBtn = screen.getByText('Interval Custom')
        fireEvent.click(customBtn)
        expect(screen.getByPlaceholderText('ex: 12/04/2026')).toBeInTheDocument()
    })

    it('3. Ar trebui să valideze cronologia datelor (Start > End)', () => {
        renderComponent()
        fireEvent.click(screen.getByText('Interval Custom'))
        const inputs = screen.getAllByPlaceholderText(/2026/)

        fireEvent.change(inputs[0], { target: { value: '20/04/2026' } })
        fireEvent.change(inputs[1], { target: { value: '10/04/2026' } })

        expect(screen.getByText(/Eroare: Data de început trebuie să fie înainte/i)).toBeInTheDocument()
    })

    it('4. Ar trebui să aplice intervalul și să arate loading (Fără Timeout)', () => {
        const { container } = renderComponent()
        fireEvent.click(screen.getByText('Interval Custom'))

        const inputs = screen.getAllByPlaceholderText(/2026/)
        fireEvent.change(inputs[0], { target: { value: '01/04/2026' } })
        fireEvent.change(inputs[1], { target: { value: '10/04/2026' } })

        fireEvent.click(screen.getByText('Aplică'))

        // Verificăm spinner-ul prin clasă (lucide-react generează SVG-uri)
        expect(container.querySelector('.animate-spin')).toBeInTheDocument()

        act(() => {
            vi.advanceTimersByTime(800)
        })

        expect(container.querySelector('.animate-spin')).not.toBeInTheDocument()
    })

    it('5. Navigare înapoi la Dashboard', () => {
        renderComponent()
        const backButton = screen.getByRole('button', { name: '' })
        fireEvent.click(backButton)
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
    })
})