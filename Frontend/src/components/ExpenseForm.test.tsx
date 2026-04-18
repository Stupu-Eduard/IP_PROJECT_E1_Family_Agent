import '@testing-library/jest-dom'
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import ExpenseForm from './ExpenseForm'

// Mock pentru store-ul de autentificare
vi.mock('../store/authStore', () => ({
    useAuthStore: () => ({
        logout: vi.fn(),
    }),
}))

describe('ExpenseForm - Manual Entry Validation', () => {

    it('ar trebui să randeze toate elementele formularului pentru introducere manuală', () => {
        render(
            <BrowserRouter>
                <ExpenseForm />
            </BrowserRouter>
        )

        expect(screen.getByText('Sumă (RON)')).toBeInTheDocument()
        expect(screen.getByText('Categorie')).toBeInTheDocument()
        expect(screen.getByText('Dată')).toBeInTheDocument()
        expect(screen.getByText('Salvează Cheltuiala')).toBeInTheDocument()
    })

    it('ar trebui să valideze suma minimă și să afișeze eroarea asincron', async () => {
        render(
            <BrowserRouter>
                <ExpenseForm />
            </BrowserRouter>
        )

        // 1. Selectăm o categorie pentru a trece de validarea 'required' a browserului
        const categorySelect = screen.getByRole('combobox')
        fireEvent.change(categorySelect, { target: { value: 'mancare' } })

        // 2. Setăm suma la 0 (valoare invalidă)
        const amountInput = screen.getByPlaceholderText('Ex: 50.50')
        fireEvent.change(amountInput, { target: { value: '0' } })

        // 3. Trimitem formularul
        const submitButton = screen.getByText('Salvează Cheltuiala')
        fireEvent.click(submitButton)

        // 4. Folosim findByText pentru a aștepta apariția mesajului în DOM
        // findByText = getByText + waitFor
        const errorMessage = await screen.findByText('Suma trebuie să fie strict mai mare ca 0!')
        expect(errorMessage).toBeInTheDocument()
    })

    it('ar trebui să simuleze adăugarea cu succes a unei cheltuieli', async () => {
        render(
            <BrowserRouter>
                <ExpenseForm />
            </BrowserRouter>
        )

        // Completăm datele corecte
        fireEvent.change(screen.getByPlaceholderText('Ex: 50.50'), { target: { value: '150' } })
        fireEvent.change(screen.getByRole('combobox'), { target: { value: 'mancare' } })

        // Declanșăm salvarea
        fireEvent.click(screen.getByText('Salvează Cheltuiala'))

        // Verificăm starea intermediară (Loading)
        expect(screen.getByText('Se salvează...')).toBeInTheDocument()

        // Așteptăm mesajul de succes (setTimeout de 1s în componentă)
        await waitFor(() => {
            expect(screen.getByText('Cheltuială adăugată cu succes!')).toBeInTheDocument()
        }, { timeout: 2000 })
    })
})