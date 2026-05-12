import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ExpenseForm from './ExpenseForm'
import { processReceiptOCR } from '../services/expenses'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../services/expenses', () => ({
    processReceiptOCR: vi.fn(),
    createExpense: vi.fn()
}))

// Mock care expune AMBELE comportamente: simulare cu fișier ȘI simulare cu null (reset)
vi.mock('./ImageUploader', () => ({
    ImageUploader: ({ onImageSelect }: any) => (
        <>
            <button onClick={() => onImageSelect(new File([''], 'test.jpg'))}>
                Simulare Încărcare OCR
            </button>
            <button onClick={() => onImageSelect(null)}>
                Simulare Reset OCR
            </button>
        </>
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

    it('4. Salvează o cheltuială cu succes și resetează formularul', async () => {
        const { createExpense } = await import('../services/expenses')
        vi.mocked(createExpense).mockResolvedValueOnce({} as any)

        renderComponent()
        const amountInput = screen.getByLabelText(/Sumă \(RON\)/i)
        const categorySelect = screen.getByLabelText(/Categorie/i)

        fireEvent.change(amountInput, { target: { value: '150.5' } })
        fireEvent.change(categorySelect, { target: { value: 'mancare' } })
        fireEvent.submit(amountInput.closest('form')!)

        await waitFor(() => {
            expect(screen.getByText('Cheltuială adăugată cu succes!')).toBeInTheDocument()
        })
        expect(amountInput).toHaveValue(null)
    })

    it('5. Completează datele prin OCR la selectarea unei imagini', async () => {
        vi.mocked(processReceiptOCR).mockResolvedValueOnce({
            amount: 250,
            category: 'facturi',
            date: '2026-04-30T12:00:00'
        } as any)
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

    // ── TESTE NOI PENTRU COVERAGE 100% ────────────────────────────────────────

    it('7. Resetează eroarea OCR când ImageUploader trimite null (deselectare fișier)', async () => {
        // Mai întâi declanșăm o eroare OCR
        vi.mocked(processReceiptOCR).mockRejectedValueOnce(new Error('OCR Failed'))
        renderComponent()
        fireEvent.click(screen.getByText('Simulare Încărcare OCR'))

        await waitFor(() => {
            expect(screen.getByText(/Nu am putut citi automat toate datele/i)).toBeInTheDocument()
        })

        // Acum apăsăm butonul care simulează deselectarea (file === null)
        // Asta acoperă ramura `else { setOcrError(null); }` din onImageSelect
        fireEvent.click(screen.getByText('Simulare Reset OCR'))

        await waitFor(() => {
            expect(screen.queryByText(/Nu am putut citi automat toate datele/i)).not.toBeInTheDocument()
        })
    })

    it('8. OCR cu dată în format YYYY-MM-DD (fără T) — acoperă ramura else din formatare', async () => {
        // Acoperă ramura `: data.date` din `data.date.includes('T') ? ... : data.date`
        vi.mocked(processReceiptOCR).mockResolvedValueOnce({
            amount: 99,
            category: 'transport',
            date: '2026-05-01'  // fără 'T'
        } as any)
        renderComponent()
        fireEvent.click(screen.getByText('Simulare Încărcare OCR'))

        await waitFor(() => {
            const dateInput = screen.getByLabelText(/Dată/i) as HTMLInputElement
            expect(dateInput.value).toBe('2026-05-01')
        })
    })

    it('9. OCR cu răspuns gol — nu modifică câmpurile (acoperă ramurile false ale if-urilor)', async () => {
        // Acoperă ramurile FALSE pentru: if (data.amount), if (data.category), if (data.date)
        vi.mocked(processReceiptOCR).mockResolvedValueOnce({
            amount: 0,        // falsy
            category: '',     // falsy
            date: ''          // falsy
        } as any)
        renderComponent()

        const amountInputBefore = screen.getByLabelText(/Sumă \(RON\)/i) as HTMLInputElement
        const dateInputBefore = screen.getByLabelText(/Dată/i) as HTMLInputElement
        const initialDateValue = dateInputBefore.value

        fireEvent.click(screen.getByText('Simulare Încărcare OCR'))

        await waitFor(() => {
            // Loader-ul trebuie să dispară (procesarea s-a terminat)
            expect(screen.queryByText('Procesăm bonul tău…')).not.toBeInTheDocument()
        })

        // Câmpurile au rămas neschimbate (ramurile if au fost false)
        expect(amountInputBefore.value).toBe('')
        const categorySelect = screen.getByLabelText(/Categorie/i) as HTMLSelectElement
        expect(categorySelect.value).toBe('')
        expect(dateInputBefore.value).toBe(initialDateValue)
    })

    it('10. Permite golirea câmpului sumă (amount = "" în onChange)', () => {
        // Acoperă ramura `e.target.value === '' ? '' : Number(...)` pentru valoarea ''
        renderComponent()
        const amountInput = screen.getByLabelText(/Sumă \(RON\)/i) as HTMLInputElement

        fireEvent.change(amountInput, { target: { value: '50' } })
        expect(amountInput).toHaveValue(50)

        fireEvent.change(amountInput, { target: { value: '' } })
        expect(amountInput).toHaveValue(null)
    })
})