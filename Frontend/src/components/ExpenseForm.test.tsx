import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ExpenseForm from './ExpenseForm'
import { processReceiptOCR } from '../services/expenses'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../services/expenses', () => ({
    processReceiptOCR: vi.fn()
}))

vi.mock('./ImageUploader', () => ({
    ImageUploader: ({ onImageSelect }: any) => (
        <button onClick={() => onImageSelect(new File([''], 'test.jpg'))}>
            Simulare Încărcare OCR
        </button>
    )
}))

describe('ExpenseForm Component', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    const renderComponent = () => render(<MemoryRouter><ExpenseForm /></MemoryRouter>)

    it('1. Randează formularul și câmpurile obligatorii', () => {
        renderComponent()
        expect(screen.getByText('Adaugă o cheltuială nouă')).toBeInTheDocument()
        expect(screen.getByLabelText(/Sumă \(RON\)/i)).toBeInTheDocument()
        expect(screen.getByLabelText(/Categorie/i)).toBeInTheDocument()
    })

    it('2. Navighează la dashboard la click pe butonul de înapoi', () => {
        renderComponent()
        const backBtn = screen.getByLabelText('Înapoi')
        fireEvent.click(backBtn)
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
    })

    it('3. Arată eroare dacă se încearcă trimiterea unei sume <= 0', async () => {
        renderComponent()
        const amountInput = screen.getByLabelText(/Sumă \(RON\)/i)
        const categorySelect = screen.getByLabelText(/Categorie/i)
        const submitBtn = screen.getByRole('button', { name: /Salvează cheltuiala/i })

        fireEvent.change(categorySelect, { target: { value: 'mancare' } })
        fireEvent.change(amountInput, { target: { value: '0' } })
        fireEvent.click(submitBtn)

        expect(await screen.findByText('Suma trebuie să fie strict mai mare ca 0!')).toBeInTheDocument()
    })

    it('4. Salvează o cheltuială cu succes și resetează formularul', () => {
        vi.useFakeTimers()
        renderComponent()
        const amountInput = screen.getByLabelText(/Sumă \(RON\)/i)
        const categorySelect = screen.getByLabelText(/Categorie/i)
        const form = amountInput.closest('form')!

        fireEvent.change(amountInput, { target: { value: '150.5' } })
        fireEvent.change(categorySelect, { target: { value: 'mancare' } })
        fireEvent.submit(form)

        expect(screen.getByText('Se salvează...')).toBeInTheDocument()

        act(() => {
            vi.advanceTimersByTime(1000)
        })

        expect(screen.getByText('Cheltuială adăugată cu succes!')).toBeInTheDocument()
        expect(amountInput).toHaveValue(null)

        act(() => {
            vi.advanceTimersByTime(3000)
        })

        vi.runOnlyPendingTimers()
        vi.useRealTimers()
    })

    it('5. Completează datele prin OCR la selectarea unei imagini', async () => {
        vi.mocked(processReceiptOCR).mockResolvedValueOnce({
            amount: 250,
            category: 'facturi',
            date: '2026-04-30T12:00:00'
        })
        renderComponent()
        fireEvent.click(screen.getByText('Simulare Încărcare OCR'))
        expect(screen.getByText('Procesăm bonul tău…')).toBeInTheDocument()

        await waitFor(() => {
            const amountInput = screen.getByLabelText(/Sumă \(RON\)/i) as HTMLInputElement
            expect(amountInput.value).toBe('250')
            const categorySelect = screen.getByLabelText(/Categorie/i) as HTMLSelectElement
            expect(categorySelect.value).toBe('facturi')
        })
    })

    it('6. Afișează eroare OCR dacă procesarea eșuează', async () => {
        vi.mocked(processReceiptOCR).mockRejectedValueOnce(new Error('OCR Failed'))
        renderComponent()
        fireEvent.click(screen.getByText('Simulare Încărcare OCR'))

        await waitFor(() => {
            expect(screen.getByText(/Nu am putut citi automat toate datele/i)).toBeInTheDocument()
        })
    })
})