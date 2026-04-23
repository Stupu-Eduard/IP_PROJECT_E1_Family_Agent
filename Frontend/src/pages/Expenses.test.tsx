import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Expenses from './Expenses'

import { fetchExpenses } from '../services/expenses'
import { fetchCategoryNames, fetchUserNames } from '../services/lookups'

vi.mock('../services/expenses', () => ({
    fetchExpenses: vi.fn(),
}))

vi.mock('../services/lookups', () => ({
    fetchCategoryNames: vi.fn(),
    fetchUserNames: vi.fn(),
}))

// Mocking the auth store
vi.mock('../store/authStore', () => ({
    useAuthStore: () => ({
        logout: vi.fn(),
    }),
}))

describe('Expenses Component - Funcționalitate și Randare', () => {
    // Resetăm mock-urile înainte de fiecare test pentru a evita interferențele
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const mockAllExpenses = [
        {
            id: 1,
            amount: 120,
            currency: 'RON',
            description: 'Cumpărături Kaufland',
            expenseDate: '2026-04-19T10:00:00',
            category: '🍕 Mâncare & Alimente',
            person: 'Maria',
            location: null,
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
        {
            id: 3,
            amount: 200,
            currency: 'RON',
            description: 'Factură Energie',
            expenseDate: '2026-04-17T10:00:00',
            category: '📄 Facturi & Utilități',
            person: 'Ion',
            location: null,
        },
        {
            id: 4,
            amount: 300,
            currency: 'RON',
            description: 'Plin Benzină',
            expenseDate: '2026-04-16T10:00:00',
            category: '🚗 Transport',
            person: 'Ion',
            location: null,
        },
    ]

    const fetchExpensesMock = fetchExpenses as unknown as ReturnType<typeof vi.fn>
    const fetchCategoryNamesMock = fetchCategoryNames as unknown as ReturnType<typeof vi.fn>
    const fetchUserNamesMock = fetchUserNames as unknown as ReturnType<typeof vi.fn>

    beforeEach(() => {
        fetchCategoryNamesMock.mockResolvedValue([
            '🍕 Mâncare & Alimente',
            '📄 Facturi & Utilități',
            '🚗 Transport',
            '🎮 Divertisment',
        ])

        fetchUserNamesMock.mockResolvedValue(['Ion', 'Maria'])

        fetchExpensesMock.mockImplementation(async (filters?: any) => {
            let data = [...mockAllExpenses]

            if (filters?.date) {
                data = data.filter((e) => String(e.expenseDate ?? '').slice(0, 10) === filters.date)
            }
            if (filters?.category) {
                data = data.filter((e) => e.category === filters.category)
            }
            if (filters?.person) {
                data = data.filter((e) => e.person === filters.person)
            }

            return data
        })
    })

    const renderComponent = () => {
        return render(
            <BrowserRouter>
                <Expenses />
            </BrowserRouter>
        )
    }

    // ==========================================
    // 1. TESTE DE RANDARE (Din baza ta inițială)
    // ==========================================

    it('ar trebui să afișeze titlul paginii și butonul de adăugare', async () => {
        renderComponent()

        await screen.findAllByText('Cumpărături Kaufland')

        expect(screen.getByText('Istoric Cheltuieli')).toBeInTheDocument()
        expect(screen.getByText('Adaugă')).toBeInTheDocument()
    })

    it('ar trebui să randeze elementele de filtrare (Select-uri)', async () => {
        renderComponent()

        await screen.findAllByText('Cumpărături Kaufland')

        expect(screen.getByText('Toate Categoriile')).toBeInTheDocument()
        expect(screen.getByText('Orice Persoană')).toBeInTheDocument()
        // Text actualizat conform noii logici de "stare derivată"
        expect(screen.getByText('Resetează Filtre')).toBeInTheDocument()
    })

    it('ar trebui să afișeze cheltuielile din backend (mock)', async () => {
        renderComponent()

        // Textul apare de două ori (Mobile & Desktop view)
        expect(await screen.findAllByText('Cumpărături Kaufland')).toHaveLength(2)
        expect(await screen.findAllByText('Abonament Netflix')).toHaveLength(2)
    })

    it('ar trebui să afișeze elementele de paginare', async () => {
        renderComponent()

        await screen.findAllByText('Cumpărături Kaufland')

        // Verificăm textul de bază al paginării
        const pageTexts = screen.getAllByText(/Pagina/i)
        expect(pageTexts.length).toBeGreaterThan(0)

        // Căutăm specific butoanele de paginare după rolul lor în DOM
        expect(screen.getByRole('button', { name: '1' })).toBeInTheDocument()
        expect(screen.getByRole('button', { name: '2' })).toBeInTheDocument()
    })

    // ==========================================
    // 2. TESTE DE LOGICĂ & FILTRARE (Funcționalitățile noi)
    // ==========================================

    it('ar trebui să filtreze corect lista după Persoană', async () => {
        renderComponent()

        await screen.findAllByText('Cumpărături Kaufland')

        // Obținem toate elementele de tip <select>. Cel de persoană este al doilea (index 1).
        const selects = screen.getAllByRole('combobox')
        const personSelect = selects[1]

        // Aplicăm filtrul pentru "Ion"
        fireEvent.change(personSelect, { target: { value: 'Ion' } })

        await waitFor(() => {
            expect(fetchExpensesMock).toHaveBeenLastCalledWith(
                { date: undefined, category: undefined, person: 'Ion' },
                expect.any(AbortSignal),
            )
        })

        // Validăm că tranzacțiile lui Ion sunt vizibile
        expect(await screen.findAllByText('Factură Energie')).toHaveLength(2)
        expect(await screen.findAllByText('Abonament Netflix')).toHaveLength(2)

        // Validăm că tranzacția Mariei a dispărut din DOM
        await waitFor(() => {
            expect(screen.queryByText('Cumpărături Kaufland')).not.toBeInTheDocument()
        })
    })

    it('ar trebui să filtreze corect lista după Categorie', async () => {
        renderComponent()

        await screen.findAllByText('Cumpărături Kaufland')

        // Obținem select-ul de categorie (index 0)
        const selects = screen.getAllByRole('combobox')
        const categorySelect = selects[0]

        // Aplicăm filtrul
        fireEvent.change(categorySelect, { target: { value: '🚗 Transport' } })

        await waitFor(() => {
            expect(fetchExpensesMock).toHaveBeenLastCalledWith(
                { date: undefined, category: '🚗 Transport', person: undefined },
                expect.any(AbortSignal),
            )
        })

        // Doar transportul trebuie să fie vizibil
        expect(await screen.findAllByText('Plin Benzină')).toHaveLength(2)
        await waitFor(() => {
            expect(screen.queryByText('Abonament Netflix')).not.toBeInTheDocument()
        })
    })

    it('ar trebui să afișeze Empty State atunci când niciun element nu corespunde filtrelor', async () => {
        renderComponent()

        await screen.findAllByText('Cumpărături Kaufland')

        const selects = screen.getAllByRole('combobox')
        const categorySelect = selects[0]
        const personSelect = selects[1]

        // Setăm filtre contradictorii (Maria nu a cumpărat Divertisment în mock data)
        fireEvent.change(categorySelect, { target: { value: '🎮 Divertisment' } })
        fireEvent.change(personSelect, { target: { value: 'Maria' } })

        // Validăm afișarea mesajului de Empty State
        expect(await screen.findByText('Nu s-au găsit cheltuieli')).toBeInTheDocument()
        expect(await screen.findByText('Nu există nicio înregistrare care să corespundă filtrelor selectate.')).toBeInTheDocument()
    })

    it('ar trebui să restabilească lista la apăsarea butonului de Resetare Filtre', async () => {
        renderComponent()

        await screen.findAllByText('Cumpărături Kaufland')

        const selects = screen.getAllByRole('combobox')
        const personSelect = selects[1]

        // 1. Filtrăm lista (ascundem elemente)
        fireEvent.change(personSelect, { target: { value: 'Ion' } })

        await waitFor(() => {
            expect(screen.queryByText('Cumpărături Kaufland')).not.toBeInTheDocument()
        })

        // 2. Apăsăm butonul de reset
        const resetButton = screen.getByText('Resetează Filtre')
        fireEvent.click(resetButton)

        // 3. Validăm că filtrele au fost golite și lista a revenit la normal
        expect(await screen.findAllByText('Cumpărături Kaufland')).toHaveLength(2)
    })
})