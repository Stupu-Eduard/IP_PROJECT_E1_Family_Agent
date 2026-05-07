import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import Expenses from './Expenses'
import * as expensesService from '../services/expenses'
import * as lookupsService from '../services/lookups'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../services/expenses', () => ({
    fetchExpenses: vi.fn(),
}))

vi.mock('../services/lookups', () => ({
    fetchCategoryNames: vi.fn(),
    fetchUserNames: vi.fn(),
}))

const renderWithRouter = () => render(<MemoryRouter><Expenses /></MemoryRouter>)

describe('Expenses Component', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(lookupsService.fetchCategoryNames).mockResolvedValue(['Alimentare', 'Transport'])
        vi.mocked(lookupsService.fetchUserNames).mockResolvedValue(['Eduard', 'Mihaela', 'Andrei'])
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    it('1. afișează scheletul de loading la inițializare', async () => {
        // Le facem să stea pending intenționat ca să nu facă update de state și să dea eroarea de act(...)
        vi.mocked(expensesService.fetchExpenses).mockImplementation(() => new Promise(() => {}))
        vi.mocked(lookupsService.fetchCategoryNames).mockImplementation(() => new Promise(() => {}))
        vi.mocked(lookupsService.fetchUserNames).mockImplementation(() => new Promise(() => {}))

        const { container } = renderWithRouter()

        expect(screen.getByText(/Istoric Cheltuieli/i)).toBeInTheDocument()
        expect(container.querySelectorAll('.skeleton').length).toBeGreaterThan(0)
    })

    it('2. nu actualizează state-ul dacă componenta este demontată înainte de finalizarea request-ului', async () => {
        let resolveFetch: any
        vi.mocked(expensesService.fetchExpenses).mockImplementation(() => new Promise((res) => { resolveFetch = res }))
        vi.mocked(lookupsService.fetchCategoryNames).mockImplementation(() => new Promise(() => {}))
        vi.mocked(lookupsService.fetchUserNames).mockImplementation(() => new Promise(() => {}))

        const { unmount } = renderWithRouter()

        unmount() // Demontăm imediat

        await act(async () => {
            resolveFetch([])
        })

        expect(expensesService.fetchExpenses).toHaveBeenCalled()
    })

    it('3. prinde eroarea dar nu actualizează state-ul dacă componenta a fost demontată', async () => {
        let rejectFetch: any
        vi.mocked(expensesService.fetchExpenses).mockImplementation(() => new Promise((_, rej) => { rejectFetch = rej }))
        vi.mocked(lookupsService.fetchCategoryNames).mockImplementation(() => new Promise(() => {}))
        vi.mocked(lookupsService.fetchUserNames).mockImplementation(() => new Promise(() => {}))

        const { unmount } = renderWithRouter()

        unmount()

        await act(async () => {
            rejectFetch(new Error('Network error'))
        })

        expect(expensesService.fetchExpenses).toHaveBeenCalled()
    })

    it('4. afișează mesaj de eroare când apelul fetchExpenses eșuează', async () => {
        vi.mocked(expensesService.fetchExpenses).mockRejectedValue(new Error('Backend error'))
        renderWithRouter()

        expect(await screen.findByText(/Nu am putut încărca cheltuielile din backend/i)).toBeInTheDocument()
    })

    it('5. afișează "Empty State" dacă nu se primesc cheltuieli de la backend', async () => {
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue([])
        renderWithRouter()

        expect(await screen.findByText('Nu s-au găsit cheltuieli')).toBeInTheDocument()
    })

    it('6. randează corect tabelul și acoperă fallback-urile pentru date incomplete', async () => {
        const mockExpenses = [
            {
                id: 1, expenseDate: '2023-10-15T12:00:00Z', category: 'Alimentare', description: 'Mega Image',
                amount: 150.50, person: 'Eduard', location: { store: 'Mega', address: 'Strada X', city: 'Cluj', country: 'RO', id: 99, lat: 44.1, lng: 26.2 }
            },
            {
                id: 2, expenseDate: null as any, category: null as any, description: null as any,
                amount: "30", person: 'Mihaela', location: null as any
            },
            {
                id: 3, expenseDate: '', category: '', description: '',
                amount: null as any, person: 'Andrei', location: {}
            }
        ]

        vi.mocked(expensesService.fetchExpenses).mockResolvedValue(mockExpenses)
        const { container } = renderWithRouter()

        const elements = await screen.findAllByText('Mega Image')
        expect(elements.length).toBeGreaterThan(0)

        expect((await screen.findAllByText('15.10.2023')).length).toBeGreaterThan(0)
        expect((await screen.findAllByText(/Mega, Strada X, Cluj, RO/i)).length).toBeGreaterThan(0)
        expect((await screen.findAllByText('Fără categorie')).length).toBeGreaterThan(0)
        expect((await screen.findAllByText('Fără locație')).length).toBeGreaterThan(0)
        expect((await screen.findAllByText('30.00')).length).toBeGreaterThan(0)
        expect((await screen.findAllByText('0.00')).length).toBeGreaterThan(0)

        const avatars = container.querySelectorAll('.avatar')
        expect(avatars.length).toBeGreaterThan(0)
    })

    it('7. navighează spre harta detaliată când dă click pe adresă', async () => {
        const mockExpense = [{
            id: 1, expenseDate: '2023-10-15T12:00:00Z', category: 'Alimentare', description: 'Auchan', amount: 50, person: 'Mihaela',
            location: { store: 'Auchan', city: 'Cluj', id: 10, lat: 46.7, lng: 23.6 }
        }]
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue(mockExpense)

        renderWithRouter()

        const locationBtns = await screen.findAllByText(/Auchan, Cluj/i)
        fireEvent.click(locationBtns[0])

        expect(mockNavigate).toHaveBeenCalledWith('/expenses/map', {
            state: { lat: 46.7, lng: 23.6, locationId: 10, locationLabel: 'Auchan, Cluj', locationCity: 'Cluj', locationCountry: undefined, description: 'Auchan' }
        })
    })

    it('8. aplică filtrele de date, categorie și persoană', async () => {
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue([])
        const { container } = renderWithRouter()

        await screen.findAllByDisplayValue('Toate Categoriile')

        // Setăm filtrele de date (care funcționează acum doar în frontend)
        const dateInputs = container.querySelectorAll('input[type="date"]')
        if (dateInputs.length >= 2) {
            fireEvent.change(dateInputs[0], { target: { value: '2023-10-10' } }) // startDate
            fireEvent.change(dateInputs[1], { target: { value: '2023-10-20' } }) // endDate
        }

        // Setăm filtrele pentru backend
        const categorySelect = screen.getAllByRole('combobox')[0]
        fireEvent.change(categorySelect, { target: { value: 'Alimentare' } })

        const personSelect = screen.getAllByRole('combobox')[1]
        fireEvent.change(personSelect, { target: { value: 'Eduard' } })

        await waitFor(() => {
            // Verificăm că a trimis datele noi către API (atenție, date: undefined)
            expect(expensesService.fetchExpenses).toHaveBeenCalledWith(
                { date: undefined, category: 'Alimentare', person: 'Eduard' },
                expect.any(AbortSignal)
            )
        })

        fireEvent.click(screen.getByText(/Resetează/i))

        await waitFor(() => {
            expect(expensesService.fetchExpenses).toHaveBeenCalledWith(
                { date: undefined, category: undefined, person: undefined },
                expect.any(AbortSignal)
            )
        })
    })

    it('9. navighează spre Adaugă Cheltuială', async () => {
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue([])
        renderWithRouter()

        fireEvent.click(await screen.findByText(/Adaugă/i))
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense')
    })

    it('10. navighează spre Dashboard la butonul de Back', async () => {
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue([])
        const { container } = renderWithRouter()

        await screen.findByText(/Istoric Cheltuieli/i)

        // Primul buton din UI-ul nou Tailwind este mereu cel de ArrowLeft (Back)
        const backBtn = container.querySelectorAll('button')[0]
        fireEvent.click(backBtn)

        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
    })

    it('11. navighează prin pagini corect', async () => {
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue([{ id: 1, amount: 10, person: 'E' } as any])
        renderWithRouter()

        await screen.findByText(/Pagina/i)
        expect(screen.getByText('1', { selector: 'strong' })).toBeInTheDocument()

        const buttons = screen.getAllByRole('button')
        // Găsim butoanele de paginare (în noul design sunt la sfârșit)
        const nextPageBtn = buttons[buttons.length - 1]
        const prevPageBtn = buttons[buttons.length - 4]

        fireEvent.click(nextPageBtn)
        expect(screen.getByText('2', { selector: 'strong' })).toBeInTheDocument()

        fireEvent.click(nextPageBtn)
        expect(screen.getByText('2', { selector: 'strong' })).toBeInTheDocument()

        fireEvent.click(prevPageBtn)
        expect(screen.getByText('1', { selector: 'strong' })).toBeInTheDocument()
    })
})