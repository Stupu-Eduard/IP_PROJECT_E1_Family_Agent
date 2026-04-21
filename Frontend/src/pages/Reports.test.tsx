import '@testing-library/jest-dom'
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Reports from './Reports'

vi.mock('../store/authStore', () => ({
    useAuthStore: () => ({
        logout: vi.fn(),
    }),
}))

// Mocking ResponsiveContainer to prevent Recharts rendering issues in JSDOM
vi.mock('recharts', async () => {
    const ActualRecharts = await vi.importActual('recharts')
    return {
        ...ActualRecharts,
        ResponsiveContainer: ({ children }: any) => <div>{children}</div>,
    }
})

describe('Reports Component (Task 2.6)', () => {
    it('ar trebui să afișeze butoanele de selecție rapidă a timpului', () => {
        render(
            <BrowserRouter>
                <Reports />
            </BrowserRouter>
        )

        expect(screen.getByText('1W')).toBeInTheDocument()
        expect(screen.getByText('1M')).toBeInTheDocument()
        expect(screen.getByText('3M')).toBeInTheDocument()
        expect(screen.getByText('1Y')).toBeInTheDocument()
    })

    it('ar trebui să deschidă selectorul custom la click pe "Interval Custom"', () => {
        render(
            <BrowserRouter>
                <Reports />
            </BrowserRouter>
        )

        const customButton = screen.getByText('Interval Custom')
        fireEvent.click(customButton)

        // După click, input-urile pentru dată ar trebui să apară
        expect(screen.getByText('De la (dd/mm/yyyy)')).toBeInTheDocument()
        expect(screen.getByText('Până la (dd/mm/yyyy)')).toBeInTheDocument()
    })

    it('ar trebui să afișeze mesaj de eroare dacă formatul datei este greșit', () => {
        render(
            <BrowserRouter>
                <Reports />
            </BrowserRouter>
        )

        // Deschidem selectorul
        fireEvent.click(screen.getByText('Interval Custom'))

        // Găsim input-ul de Start (are placeholder ex: 12/04/2026)
        const startDateInput = screen.getByPlaceholderText('ex: 12/04/2026')

        // Introducem o dată invalidă
        fireEvent.change(startDateInput, { target: { value: 'invalid-date' } })

        // Verificăm dacă mesajul de eroare apare pe ecran
        expect(screen.getByText(/Te rugăm să introduci datele conform formatului/i)).toBeInTheDocument()
    })

    it('ar trebui să dezactiveze butonul Aplică dacă datele sunt greșite', () => {
        render(
            <BrowserRouter>
                <Reports />
            </BrowserRouter>
        )

        fireEvent.click(screen.getByText('Interval Custom'))
        const startDateInput = screen.getByPlaceholderText('ex: 12/04/2026')

        fireEvent.change(startDateInput, { target: { value: 'wrong-date' } })

        const applyBtn = screen.getByText('Aplică')
        expect(applyBtn).toBeDisabled()
    })
})