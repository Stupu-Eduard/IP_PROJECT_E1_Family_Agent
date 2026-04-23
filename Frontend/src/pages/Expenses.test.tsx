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

const mockLogout = vi.fn()
vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector({ logout: mockLogout }),
}))

vi.mock('../services/expenses', () => ({
    fetchExpenses: vi.fn(),
}))

vi.mock('../services/lookups', () => ({
    fetchCategoryNames: vi.fn(),
    fetchUserNames: vi.fn(),
}))

describe('Expenses Component - 100% Coverage Final', () => {
    const mockAllExpenses = [
        {
            id: 1,
            amount: 120,
            currency: 'RON',
            description: 'Cumpărături Kaufland',
            expenseDate: '2026-04-19T10:00:00',
            category: '🍕 Mâncare & Alimente',
            person: 'Maria',
            location: { id: 99, store: 'Kaufland', city: 'Bucuresti', lat: 44.4, lng: 26.1 },
        },
        {
            id: 2,
            amount: 55,
            currency: 'RON',
            description: 'Abonament Netflix',
            expenseDate: '2026-04-18T10:00:00',
            category: '🎮 Divertisment',
            person: 'Ion',
            location: null,
        },
    ]

    const fetchExpensesMock = fetchExpenses as unknown as ReturnType<typeof vi.fn>
    const fetchCategoryNamesMock = fetchCategoryNames as unknown as ReturnType<typeof vi.fn>
    const fetchUserNamesMock = fetchUserNames as unknown as ReturnType<typeof vi.fn>

    beforeEach(() => {
        vi.clearAllMocks()

        fetchCategoryNamesMock.mockResolvedValue(['🍕 Mâncare & Alimente', '🎮 Divertisment'])
        fetchUserNamesMock.mockResolvedValue(['Ion', 'Maria'])

        fetchExpensesMock.mockImplementation(async (filters?: any) => {
            let data = [...mockAllExpenses]
            if (filters?.date) data = data.filter((e) => String(e.expenseDate ?? '').slice(0, 10) === filters.date)
            if (filters?.category) data = data.filter((e) => e.category === filters.category)
            if (filters?.person) data = data.filter((e) => e.person === filters.person)
            return data
        })
    })

    const renderComponent = () => {
        return render(
            <MemoryRouter>
                <Expenses />
            </MemoryRouter>
        )
    }

    it('ar trebui să afișeze starea de încărcare și apoi datele mock', async () => {
        renderComponent()
        expect(screen.getByText('Se încarcă cheltuielile…')).toBeInTheDocument()

        await screen.findAllByText('Cumpărături Kaufland')
        expect(screen.queryByText('Se încarcă cheltuielile…')).not.toBeInTheDocument()
        expect(screen.getByText('Istoric Cheltuieli')).toBeInTheDocument()
    })

    it('ar trebui să filtreze lista și să apeleze backend-ul cu parametrii corecți', async () => {
        renderComponent()
        await screen.findAllByText('Cumpărături Kaufland')

        const selects = screen.getAllByRole('combobox')
        const categorySelect = selects[0]
        const personSelect = selects[1]
        const dateInput = screen.getByTitle('Perioadă')

        fireEvent.change(personSelect, { target: { value: 'Ion' } })
        await waitFor(() => {
            expect(fetchExpensesMock).toHaveBeenCalledWith(
                expect.objectContaining({ person: 'Ion' }),
                expect.any(AbortSignal)
            )
        })

        fireEvent.change(categorySelect, { target: { value: '🎮 Divertisment' } })
        await waitFor(() => {
            expect(fetchExpensesMock).toHaveBeenCalledWith(
                expect.objectContaining({ category: '🎮 Divertisment', person: 'Ion' }),
                expect.any(AbortSignal)
            )
        })

        fireEvent.change(dateInput, { target: { value: '2026-04-18' } })
        await waitFor(() => {
            expect(fetchExpensesMock).toHaveBeenCalledWith(
                expect.objectContaining({ date: '2026-04-18' }),
                expect.any(AbortSignal)
            )
        })

        fireEvent.click(screen.getByText('Resetează Filtre'))
        await waitFor(() => {
            expect(fetchExpensesMock).toHaveBeenCalledWith(
                { date: undefined, category: undefined, person: undefined },
                expect.any(AbortSignal)
            )
        })
    })

    it('ar trebui să gestioneze corect butoanele de paginare', async () => {
        renderComponent()
        await screen.findAllByText('Cumpărături Kaufland')

        const unnamedButtons = screen.getAllByRole('button', { name: '' })
        const prevBtn = unnamedButtons[1]
        const nextBtn = unnamedButtons[2]

        fireEvent.click(screen.getByRole('button', { name: '2' }))
        expect(screen.getByText((_, el) => el?.textContent === 'Pagina 2 din 2')).toBeInTheDocument()

        fireEvent.click(screen.getByRole('button', { name: '1' }))
        expect(screen.getByText((_, el) => el?.textContent === 'Pagina 1 din 2')).toBeInTheDocument()

        fireEvent.click(nextBtn)
        fireEvent.click(prevBtn)
        expect(screen.getByText((_, el) => el?.textContent === 'Pagina 1 din 2')).toBeInTheDocument()
    })

    it('ar trebui să execute navigarea și funcția de logout', async () => {
        renderComponent()
        await screen.findAllByText('Cumpărături Kaufland')

        const backBtn = screen.getAllByRole('button', { name: '' })[0]
        fireEvent.click(backBtn)
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')

        fireEvent.click(screen.getByText('FamilyAgent'))
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')

        fireEvent.click(screen.getByText('Adaugă'))
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense')

        fireEvent.click(screen.getByText('Logout'))
        expect(mockLogout).toHaveBeenCalled()
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
    })

    it('ar trebui să navigheze la hartă la apăsarea locației (openMap)', async () => {
        renderComponent()
        await screen.findAllByText('Cumpărături Kaufland')

        const locationBtn = screen.getAllByText('Kaufland, Bucuresti')[0]
        fireEvent.click(locationBtn)

        expect(mockNavigate).toHaveBeenCalledWith('/expenses/map', {
            state: expect.objectContaining({
                lat: 44.4,
                lng: 26.1,
                locationId: 99,
            }),
        })
    })

    it('ar trebui să afișeze Empty State atunci când serverul returnează o listă goală', async () => {
        fetchExpensesMock.mockResolvedValueOnce([])
        renderComponent()

        expect(await screen.findByText('Nu s-au găsit cheltuieli')).toBeInTheDocument()
    })

    it('ar trebui să afișeze mesaj de eroare dacă fetch-ul eșuează', async () => {
        fetchExpensesMock.mockRejectedValueOnce(new Error('Network Error'))
        renderComponent()

        expect(await screen.findByText(/Nu am putut încărca cheltuielile din backend/)).toBeInTheDocument()
    })
})