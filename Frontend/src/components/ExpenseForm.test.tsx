import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import ExpenseForm from './ExpenseForm'
import * as expenseService from '../services/expenses'

vi.mock('../store/authStore', () => ({
    useAuthStore: () => ({
        logout: vi.fn(),
    }),
}))

vi.mock('../services/expenses', () => ({
    processReceiptOCR: vi.fn(),
}))

vi.mock('./ImageUploader', () => ({
    ImageUploader: ({ onImageSelect }: { onImageSelect: (file: File | null) => void }) => (
        <div data-testid="mock-uploader">
            <input
                type="file"
                onChange={(e) => onImageSelect(e.target.files ? e.target.files[0] : null)}
            />
        </div>
    ),
}))

describe('ExpenseForm Component - OCR & Validation (US 2.3)', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        // AM ELIMINAT FAKE TIMERS DE AICI PENTRU A NU BLOCA FUNCȚIA waitFor()
    })

    afterEach(() => {
        // Ne asigurăm că timpul revine la normal după fiecare test
        vi.useRealTimers()
    })

    const renderComponent = () => render(
        <BrowserRouter>
            <ExpenseForm />
        </BrowserRouter>
    )

    it('ar trebui sa randeze elementele de baza si uploader-ul', () => {
        renderComponent()
        expect(screen.getByText('Adaugă Cheltuială')).toBeInTheDocument()
        expect(screen.getByTestId('mock-uploader')).toBeInTheDocument()
    })

    it('ar trebui sa declanseze procesul OCR si sa populeze campurile automat la succes', async () => {
        const mockOcrData = {
            amount: 125.50,
            category: 'mancare',
            date: '2026-04-25T10:00:00',
            confidence: 0.98
        };
        vi.mocked(expenseService.processReceiptOCR).mockResolvedValue(mockOcrData);

        const { container } = renderComponent()

        const file = new File(['receipt'], 'bon.png', { type: 'image/png' });
        const uploader = screen.getByTestId('mock-uploader').querySelector('input')!;
        fireEvent.change(uploader, { target: { files: [file] } });

        expect(screen.getByText('Analizăm bonul...')).toBeInTheDocument()

        await waitFor(() => {
            const amountInput = screen.getByPlaceholderText('Ex: 50.50') as HTMLInputElement;
            const categorySelect = screen.getByRole('combobox') as HTMLSelectElement;
            const dateInput = container.querySelector('input[type="date"]') as HTMLInputElement;

            expect(amountInput.value).toBe('125.5')
            expect(categorySelect.value).toBe('mancare')
            expect(dateInput.value).toBe('2026-04-25')
        })

        await waitFor(() => {
            expect(screen.queryByText('Analizăm bonul...')).not.toBeInTheDocument()
        })
    })

    it('ar trebui sa afiseze un mesaj de eroare daca serviciul OCR esueaza', async () => {
        vi.mocked(expenseService.processReceiptOCR).mockRejectedValue(new Error('Network Error'));

        renderComponent()

        const file = new File(['receipt'], 'bon.png', { type: 'image/png' });
        const uploader = screen.getByTestId('mock-uploader').querySelector('input')!;
        fireEvent.change(uploader, { target: { files: [file] } });

        await waitFor(() => {
            expect(screen.getByText(/Nu am putut citi automat toate datele/i)).toBeInTheDocument()
        })

        const amountInput = screen.getByPlaceholderText('Ex: 50.50') as HTMLInputElement;
        expect(amountInput.disabled).toBe(false)
    })

    it('ar trebui sa blocheze interactiunea cu formularul in timpul analizei OCR', async () => {
        vi.mocked(expenseService.processReceiptOCR).mockReturnValue(new Promise(() => {}));

        renderComponent()

        const file = new File(['receipt'], 'bon.png', { type: 'image/png' });
        const uploader = screen.getByTestId('mock-uploader').querySelector('input')!;
        fireEvent.change(uploader, { target: { files: [file] } });

        const amountInput = screen.getByPlaceholderText('Ex: 50.50') as HTMLInputElement;
        const submitButton = screen.getByRole('button', { name: /Salvează Cheltuiala/i });

        expect(amountInput.disabled).toBe(true)
        expect(submitButton).toBeDisabled()
    })

    it('ar trebui sa blocheze submit-ul si sa afiseze eroare daca suma este 0 sau negativa', () => {
        renderComponent()
        const amountInput = screen.getByPlaceholderText('Ex: 50.50') as HTMLInputElement;
        const categorySelect = screen.getByRole('combobox') as HTMLSelectElement;
        const submitButton = screen.getByRole('button', { name: /Salvează Cheltuiala/i });

        fireEvent.change(categorySelect, { target: { value: 'mancare' } });
        fireEvent.change(amountInput, { target: { value: '0' } });
        fireEvent.click(submitButton);

        expect(screen.getByText('Suma trebuie să fie strict mai mare ca 0!')).toBeInTheDocument();
    })

    it('ar trebui sa arate starea de loading, sa efectueze salvarea si sa reseteze formularul', () => {
        vi.useFakeTimers();

        renderComponent();

        const amountInput = screen.getByPlaceholderText('Ex: 50.50') as HTMLInputElement;
        const categorySelect = screen.getByRole('combobox') as HTMLSelectElement;
        const submitButton = screen.getByRole('button', { name: /Salvează Cheltuiala/i });

        fireEvent.change(amountInput, { target: { value: '150' } });
        fireEvent.change(categorySelect, { target: { value: 'mancare' } });

        fireEvent.click(submitButton);

        expect(screen.getByText(/Se salvează.../i)).toBeInTheDocument();

        act(() => {
            vi.advanceTimersByTime(1000)
        })

        expect(screen.getByText(/Cheltuială adăugată cu succes!/i)).toBeInTheDocument()
        expect(amountInput.value).toBe('');
        expect(categorySelect.value).toBe('');

        act(() => {
            vi.advanceTimersByTime(3000)
        })

        vi.useRealTimers();
    })
})