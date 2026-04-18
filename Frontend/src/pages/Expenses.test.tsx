import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Expenses from './Expenses'

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

    it('ar trebui să afișeze titlul paginii și butonul de adăugare', () => {
        renderComponent()

        expect(screen.getByText('Istoric Cheltuieli')).toBeInTheDocument()
        expect(screen.getByText('Adaugă')).toBeInTheDocument()
    })

    it('ar trebui să randeze elementele de filtrare (Select-uri)', () => {
        renderComponent()

        expect(screen.getByText('Toate Categoriile')).toBeInTheDocument()
        expect(screen.getByText('Orice Persoană')).toBeInTheDocument()
        // Text actualizat conform noii logici de "stare derivată"
        expect(screen.getByText('Resetează Filtre')).toBeInTheDocument()
    })

    it('ar trebui să afișeze cheltuielile din lista dummy', () => {
        renderComponent()

        // Folosim getAllByText deoarece textul apare de două ori (Mobile & Desktop view)
        expect(screen.getAllByText('Cumpărături Kaufland')[0]).toBeInTheDocument()
        expect(screen.getAllByText('Abonament Netflix')[0]).toBeInTheDocument()
    })

    it('ar trebui să afișeze elementele de paginare', () => {
        renderComponent()

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

    it('ar trebui să filtreze corect lista după Persoană', () => {
        renderComponent()

        // Obținem toate elementele de tip <select>. Cel de persoană este al doilea (index 1).
        const selects = screen.getAllByRole('combobox')
        const personSelect = selects[1]

        // Aplicăm filtrul pentru "Ion"
        fireEvent.change(personSelect, { target: { value: 'Ion' } })

        // Validăm că tranzacțiile lui Ion sunt vizibile
        expect(screen.getAllByText('Factură Energie').length).toBeGreaterThan(0)
        expect(screen.getAllByText('Abonament Netflix').length).toBeGreaterThan(0)

        // Validăm că tranzacția Mariei a dispărut din DOM
        expect(screen.queryByText('Cumpărături Kaufland')).not.toBeInTheDocument()
    })

    it('ar trebui să filtreze corect lista după Categorie', () => {
        renderComponent()

        // Obținem select-ul de categorie (index 0)
        const selects = screen.getAllByRole('combobox')
        const categorySelect = selects[0]

        // Aplicăm filtrul
        fireEvent.change(categorySelect, { target: { value: '🚗 Transport' } })

        // Doar transportul trebuie să fie vizibil
        expect(screen.getAllByText('Plin Benzină').length).toBeGreaterThan(0)
        expect(screen.queryByText('Abonament Netflix')).not.toBeInTheDocument()
    })

    it('ar trebui să afișeze Empty State atunci când niciun element nu corespunde filtrelor', () => {
        renderComponent()

        const selects = screen.getAllByRole('combobox')
        const categorySelect = selects[0]
        const personSelect = selects[1]

        // Setăm filtre contradictorii (Maria nu a cumpărat Divertisment în mock data)
        fireEvent.change(categorySelect, { target: { value: '🎮 Divertisment' } })
        fireEvent.change(personSelect, { target: { value: 'Maria' } })

        // Validăm afișarea mesajului de Empty State
        expect(screen.getByText('Nu s-au găsit cheltuieli')).toBeInTheDocument()
        expect(screen.getByText('Nu există nicio înregistrare care să corespundă filtrelor selectate.')).toBeInTheDocument()
    })

    it('ar trebui să restabilească lista la apăsarea butonului de Resetare Filtre', () => {
        renderComponent()

        const selects = screen.getAllByRole('combobox')
        const personSelect = selects[1]

        // 1. Filtrăm lista (ascundem elemente)
        fireEvent.change(personSelect, { target: { value: 'Ion' } })
        expect(screen.queryByText('Cumpărături Kaufland')).not.toBeInTheDocument()

        // 2. Apăsăm butonul de reset
        const resetButton = screen.getByText('Resetează Filtre')
        fireEvent.click(resetButton)

        // 3. Validăm că filtrele au fost golite și lista a revenit la normal
        expect(screen.getAllByText('Cumpărături Kaufland').length).toBeGreaterThan(0)
    })
})