import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { vi } from 'vitest'
import ExpenseForm from './ExpenseForm'
import { processReceiptOCR } from '../services/expenses'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../services/expenses', () => ({
    processReceiptOCR: vi.fn(),
    createExpense: vi.fn().mockResolvedValue({ id: 1 })
}))

vi.mock('./ImageUploader', () => ({
    ImageUploader: ({ onImageSelect }: any) => (
        <>
            <button onClick={() => onImageSelect(new File([''], 'test.jpg'))}>Simulare Încărcare OCR</button>
            <button onClick={() => onImageSelect(null)}>Simulare Reset OCR</button>
        </>
    ),
}))

vi.mock('../store/expenseStore', () => ({
    useExpenseStore: () => ({ notifyExpenseAdded: vi.fn() }),
}))

vi.mock('../services/lookups', () => ({
    fetchCategoryNames: vi.fn().mockResolvedValue(['supermarket', 'transport', 'facturi']),
}))

const renderComponent = () => render(<ExpenseForm />)

describe('ExpenseForm', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('procesează OCR cu tranzacții multiple', async () => {
        vi.mocked(processReceiptOCR).mockResolvedValueOnce({
            transactions: [
                { amount: 250, description: 'Kaufland', date: '2026-04-30', currency: 'RON', type: 'EXPENSE' },
                { amount: 45, description: 'Benzină', date: '2026-04-30', currency: 'RON', type: 'EXPENSE' }
            ],
            count: 2
        })
        renderComponent()
        fireEvent.click(screen.getByText('Simulare Încărcare OCR'))
        
        await waitFor(() => {
            expect(screen.getByText('Următoarea →')).toBeDefined()
        })
        
        const amountInput = screen.getByLabelText(/Sumă \(RON\)/i) as HTMLInputElement
        expect(amountInput.value).toBe('250')
    })

    it('navighează la următoarea tranzacție', async () => {
        vi.mocked(processReceiptOCR).mockResolvedValueOnce({
            transactions: [
                { amount: 100, description: 'Test1', date: '2026-04-30', currency: 'RON', type: 'EXPENSE' },
                { amount: 200, description: 'Test2', date: '2026-04-30', currency: 'RON', type: 'EXPENSE' }
            ],
            count: 2
        })
        renderComponent()
        fireEvent.click(screen.getByText('Simulare Încărcare OCR'))
        
        await waitFor(() => {
            expect(screen.getByText('Următoarea →')).toBeDefined()
        })
        
        fireEvent.click(screen.getByText('Următoarea →'))
        
        await waitFor(() => {
            const amountInput = screen.getByLabelText(/Sumă \(RON\)/i) as HTMLInputElement
            expect(amountInput.value).toBe('200')
        })
    })

    it('afișează eroare când OCR nu găsește tranzacții', async () => {
        vi.mocked(processReceiptOCR).mockResolvedValueOnce({
            transactions: [],
            count: 0
        })
        renderComponent()
        fireEvent.click(screen.getByText('Simulare Încărcare OCR'))
        
        await waitFor(() => {
            expect(screen.getByText(/Nu am putut extrage tranzacții/)).toBeDefined()
        })
    })

    it('afișează eroare la eșec OCR', async () => {
        vi.mocked(processReceiptOCR).mockRejectedValueOnce(new Error('OCR Failed'))
        renderComponent()
        fireEvent.click(screen.getByText('Simulare Încărcare OCR'))

        await waitFor(() => {
            expect(screen.getByText(/Nu am putut citi automat datele/)).toBeDefined()
        })
    })

    it('resetează tranzacțiile la deselectare imagine', async () => {
        vi.mocked(processReceiptOCR).mockResolvedValueOnce({
            transactions: [{ amount: 99, description: 'Test', date: '2026-05-01', currency: 'RON', type: 'EXPENSE' }],
            count: 1
        })
        renderComponent()
        fireEvent.click(screen.getByText('Simulare Încărcare OCR'))
        
        await waitFor(() => {
            expect(screen.getByText('Salvează cheltuiala')).toBeDefined()
        })
        
        fireEvent.click(screen.getByText('Simulare Reset OCR'))
        
        await waitFor(() => {
            expect(screen.queryByText('Următoarea →')).toBeNull()
        })
    })
})
