import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Expenses from './Expenses'
import { fetchExpenses } from '../services/expenses'
import { fetchCategoryNames, fetchUserNames } from '../services/lookups'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate, MemoryRouter: actual.MemoryRouter }
})

vi.mock('../services/expenses', () => ({ fetchExpenses: vi.fn() }))
vi.mock('../services/lookups', () => ({
    fetchCategoryNames: vi.fn(),
    fetchUserNames: vi.fn(),
}))

describe('Expenses Component', () => {
    const mockAllExpenses = [
        {
            id: 1, amount: 120, currency: 'RON',
            description: 'Cumpărături Kaufland',
            expenseDate: '2026-04-19T10:00:00',
            category: 'Alimentare', person: 'Maria',
            location: { id: 99, store: 'Kaufland', address: '', city: 'Bucuresti', country: '', lat: 44.4, lng: 26.1 },
        },
        {
            id: 2, amount: 55, currency: 'RON',
            description: 'Abonament Netflix',
            expenseDate: '2026-04-18T10:00:00',
            category: 'Divertisment', person: 'Ion',
            location: null,
        },
    ]

    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(fetchCategoryNames).mockResolvedValue(['Alimentare', 'Divertisment'])
        vi.mocked(fetchUserNames).mockResolvedValue(['Ion', 'Maria'])
        vi.mocked(fetchExpenses).mockImplementation(async (filters?: any) => {
            let data = [...mockAllExpenses]
            if (filters?.category) data = data.filter(e => e.category === filters.category)
            if (filters?.person) data = data.filter(e => e.person === filters.person)
            return data
        })
    })

    const renderComponent = () => render(<MemoryRouter><Expenses /></MemoryRouter>)

    it('1. Afișează datele după încărcare', async () => {
        renderComponent()
        await screen.findByText('Cumpărături Kaufland')
        expect(screen.getByText('Abonament Netflix')).toBeInTheDocument()
        expect(screen.getByText('Istoric cheltuieli')).toBeInTheDocument()
    })

    it('2. Filtrează după categorie', async () => {
        renderComponent()
        await screen.findByText('Cumpărături Kaufland')

        const selects = screen.getAllByRole('combobox')
        fireEvent.change(selects[0], { target: { value: 'Divertisment' } })

        await waitFor(() => {
            expect(vi.mocked(fetchExpenses)).toHaveBeenCalledWith(
                expect.objectContaining({ category: 'Divertisment' }),
                expect.any(AbortSignal)
            )
        })
    })

    it('3. Resetează filtrele', async () => {
        renderComponent()
        await screen.findByText('Cumpărături Kaufland')
        fireEvent.click(screen.getByText('Resetează'))

        await waitFor(() => {
            expect(vi.mocked(fetchExpenses)).toHaveBeenCalled()
        })
    })

    it('4. Navighează la dashboard la click pe înapoi', async () => {
        renderComponent()
        await screen.findByText('Cumpărături Kaufland')
        // Butonul back e primul button
        const backBtn = screen.getAllByRole('button')[0]
        fireEvent.click(backBtn)
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
    })

    it('5. Navighează la add-expense la click pe Adaugă', async () => {
        renderComponent()
        await screen.findByText('Cumpărături Kaufland')
        fireEvent.click(screen.getByText('Adaugă'))
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense')
    })

    it('6. Afișează Empty State când lista e goală', async () => {
        vi.mocked(fetchExpenses).mockResolvedValueOnce([])
        renderComponent()
        expect(await screen.findByText('Nu s-au găsit cheltuieli')).toBeInTheDocument()
    })

    it('7. Afișează eroare dacă fetch eșuează', async () => {
        vi.mocked(fetchExpenses).mockRejectedValueOnce(new Error('Network Error'))
        renderComponent()
        expect(await screen.findByText(/Nu am putut încărca cheltuielile din backend/)).toBeInTheDocument()
    })

    it('8. Navighează la hartă la click pe locație', async () => {
        renderComponent()
        await screen.findByText('Cumpărături Kaufland')
        const locationBtn = screen.getByText('Kaufland, Bucuresti')
        fireEvent.click(locationBtn)
        expect(mockNavigate).toHaveBeenCalledWith('/expenses/map', expect.objectContaining({
            state: expect.objectContaining({ lat: 44.4, lng: 26.1 })
        }))
    })
})