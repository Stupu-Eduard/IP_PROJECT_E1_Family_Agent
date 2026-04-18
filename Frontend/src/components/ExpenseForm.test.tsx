import '@testing-library/jest-dom'
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import ExpenseForm from './ExpenseForm'

vi.mock('../store/authStore', () => ({
    useAuthStore: () => ({
        logout: vi.fn(),
    }),
}))

describe('ExpenseForm Component (Task 2.2)', () => {
    it('ar trebui să randeze toate elementele formularului', () => {
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

    it('ar trebui să arate o eroare dacă se încearcă submit cu sumă 0 sau mai mică', () => {
        render(
            <BrowserRouter>
                <ExpenseForm />
            </BrowserRouter>
        )

        // 1. Selectăm o categorie pentru a trece de atributul 'required'
        fireEvent.change(screen.getByRole('combobox'), { target: { value: 'mancare' } })

        // 2. Setăm suma la 0
        const amountInput = screen.getByPlaceholderText('Ex: 50.50')
        fireEvent.change(amountInput, { target: { value: '0' } })

        // 3. Trimitem formularul
        const submitButton = screen.getByText('Salvează Cheltuiala')
        fireEvent.click(submitButton)

        // Acum eroarea va apărea pe ecran
        expect(screen.getByText('Suma trebuie să fie strict mai mare ca 0!')).toBeInTheDocument()
    })

    it('ar trebui să simuleze adăugarea cu succes a unei cheltuieli', async () => {
        render(
            <BrowserRouter>
                <ExpenseForm />
            </BrowserRouter>
        )

        // Completăm formularul
        fireEvent.change(screen.getByPlaceholderText('Ex: 50.50'), { target: { value: '150' } })
        fireEvent.change(screen.getByRole('combobox'), { target: { value: 'mancare' } })

        // Trimitem formularul
        fireEvent.click(screen.getByText('Salvează Cheltuiala'))

        // Așteptăm să apară starea de încărcare, apoi mesajul de succes
        expect(screen.getByText('Se salvează...')).toBeInTheDocument()

        await waitFor(() => {
            expect(screen.getByText('Cheltuială adăugată cu succes!')).toBeInTheDocument()
        }, { timeout: 1500 }) // Așteptăm să treacă setTimeout-ul de 1 secundă din componentă
    })
})