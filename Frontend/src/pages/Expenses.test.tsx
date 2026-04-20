import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Expenses from './Expenses'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate, MemoryRouter: actual.MemoryRouter }
})

const mockLogout = vi.fn()
vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector({ logout: mockLogout }),
}))

describe('Expenses Component - 100% Coverage Final', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderComponent = () => render(
        <MemoryRouter>
            <Expenses />
        </MemoryRouter>
    )

    it('1. Filtrare complexă: Dată, Categorie și Persoană', () => {
        renderComponent()
        const selects = screen.getAllByRole('combobox')
        const dateInput = screen.getByTitle('Perioadă')

        // Testare ramură conversie dată (split/reverse/join)
        fireEvent.change(dateInput, { target: { value: '2026-04-12' } })
        expect(screen.getAllByText('Cumpărături Kaufland').length).toBeGreaterThan(0)

        // Testare ramură Categorie și Persoană simultan
        fireEvent.change(selects[0], { target: { value: '🚗 Transport' } })
        fireEvent.change(selects[1], { target: { value: 'Maria' } })

        fireEvent.click(screen.getByText('Resetează Filtre'))
        expect(screen.getAllByText('Factură Energie').length).toBeGreaterThan(0)
    })

    it('2. Paginare: Toate butoanele numerice și direcționale', () => {
        renderComponent()
        const unnamedButtons = screen.getAllByRole('button', { name: '' })
        const prevBtn = unnamedButtons[1]
        const nextBtn = unnamedButtons[2]

        // Testare directă buton numeric 2 (setCurrentPage(2))
        fireEvent.click(screen.getByRole('button', { name: '2' }))
        expect(screen.getByText((_, el) => el?.textContent === 'Pagina 2 din 2')).toBeInTheDocument()

        // Testare directă buton numeric 1 (setCurrentPage(1))
        fireEvent.click(screen.getByRole('button', { name: '1' }))
        expect(screen.getByText((_, el) => el?.textContent === 'Pagina 1 din 2')).toBeInTheDocument()

        // Testare Chevron Next și Prev
        fireEvent.click(nextBtn)
        fireEvent.click(prevBtn)
        expect(screen.getByText((_, el) => el?.textContent === 'Pagina 1 din 2')).toBeInTheDocument()
    })

    it('3. Navigare și Logout: Execuție și redirecționare replace', () => {
        renderComponent()

        // Back Navigation
        const backBtn = screen.getAllByRole('button', { name: '' })[0]
        fireEvent.click(backBtn)
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')

        // Logo Navigation
        fireEvent.click(screen.getByText('FamilyAgent'))
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')

        // Add Expense Navigation
        fireEvent.click(screen.getByText('Adaugă'))
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense')

        // Logout Flow
        fireEvent.click(screen.getByText('Logout'))
        expect(mockLogout).toHaveBeenCalled()
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
    })

    it('4. Empty State: Validarea randării fallback-ului', () => {
        renderComponent()
        const dateInput = screen.getByTitle('Perioadă')

        // Aplicăm o dată viitoare pentru a forța lista goală
        fireEvent.change(dateInput, { target: { value: '2026-12-31' } })

        expect(screen.getByText('Nu s-au găsit cheltuieli')).toBeInTheDocument()
        expect(screen.getByText('Nu există nicio înregistrare care să corespundă filtrelor selectate.')).toBeInTheDocument()
    })
})