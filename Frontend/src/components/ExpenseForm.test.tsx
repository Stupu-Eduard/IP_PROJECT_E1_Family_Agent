import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ExpenseForm from './ExpenseForm'
import type { ApiExpenseListDto } from '../services/expenses'
import type { OcrResponseDTO } from '../types/OcrResponseDTO'
import { createExpense, processReceiptOCR, updateExpense } from '../services/expenses'

type MockLocationState = { expense?: ApiExpenseListDto } | null

const mockNavigate = vi.fn()
const mockLocation = { state: null as MockLocationState }

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate, useLocation: () => mockLocation }
})

vi.mock('../services/expenses', () => ({
  createExpense: vi.fn(),
  updateExpense: vi.fn(),
  processReceiptOCR: vi.fn(),
}))

type MockStoreState = {
  expenses: ApiExpenseListDto[]
  upsertExpense: (expense: ApiExpenseListDto) => void
  removeExpense: (id: number) => void
  setExpenses: (expenses: ApiExpenseListDto[]) => void
}

const mockStoreState: MockStoreState = {
  expenses: [],
  upsertExpense: (expense) => {
    const index = mockStoreState.expenses.findIndex((item) => item.id === expense.id)
    if (index === -1) {
      mockStoreState.expenses = [expense, ...mockStoreState.expenses]
      return
    }
    mockStoreState.expenses[index] = expense
  },
  removeExpense: (id) => {
    mockStoreState.expenses = mockStoreState.expenses.filter((item) => item.id !== id)
  },
  setExpenses: (expenses) => {
    mockStoreState.expenses = expenses
  },
}

vi.mock('../store/expensesStore', () => ({
  useExpensesStore: (selector: (state: MockStoreState) => unknown) => selector(mockStoreState),
}))

interface MockUploaderProps {
  onImageSelect: (file: File | null) => void
}

vi.mock('./ImageUploader', () => ({
  ImageUploader: ({ onImageSelect }: MockUploaderProps) => (
    <button onClick={() => onImageSelect(new File([''], 'test.jpg'))}>Simulare Încărcare OCR</button>
  ),
}))

const renderAddRoute = () => {
  mockLocation.state = null
  return render(
    <MemoryRouter initialEntries={['/add-expense']}>
      <Routes>
        <Route path="/add-expense" element={<ExpenseForm />} />
      </Routes>
    </MemoryRouter>,
  )
}

const renderEditRoute = (expense: ApiExpenseListDto) => {
  mockLocation.state = { expense }
  return render(
    <MemoryRouter initialEntries={[`/expenses/${expense.id}/edit`]}>
      <Routes>
        <Route path="/expenses/:id/edit" element={<ExpenseForm />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ExpenseForm Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStoreState.expenses = []
    mockLocation.state = null
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('afișează formularul de adăugare și butoanele de bază', () => {
    renderAddRoute()
    expect(screen.getByText('Adaugă o cheltuială nouă')).toBeInTheDocument()
    expect(screen.getByLabelText(/Sumă \(RON\)/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Categorie/i)).toBeInTheDocument()
  })

  it('navighează la dashboard la click pe butonul de înapoi în modul add', () => {
    renderAddRoute()
    fireEvent.click(screen.getByLabelText('Înapoi'))
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
  })

  it('afișează eroare dacă suma este <= 0', async () => {
    renderAddRoute()
    fireEvent.change(screen.getByLabelText(/Sumă \(RON\)/i), { target: { value: '0' } })
    fireEvent.change(screen.getByLabelText(/Categorie/i), { target: { value: 'mancare' } })
    fireEvent.click(screen.getByRole('button', { name: /Salvează cheltuiala/i }))
    expect(await screen.findByText('Suma trebuie să fie strict mai mare ca 0!')).toBeInTheDocument()
  })

  it('creează o cheltuială și afișează mesajul de succes în modul add', async () => {
    vi.mocked(createExpense).mockResolvedValueOnce({
      id: 10,
      amount: 150.5,
      category: 'mancare',
      expenseDate: '2026-05-05',
      description: 'Mega Image',
      person: 'Eduard',
      location: null,
    } as unknown as ApiExpenseListDto)

    renderAddRoute()
    fireEvent.change(screen.getByLabelText(/Sumă \(RON\)/i), { target: { value: '150.5' } })
    fireEvent.change(screen.getByLabelText(/Categorie/i), { target: { value: 'mancare' } })
    fireEvent.change(screen.getByLabelText(/Descriere/i), { target: { value: 'Mega Image' } })
    fireEvent.change(screen.getByLabelText(/Persoană/i), { target: { value: 'Eduard' } })
    fireEvent.click(screen.getByRole('button', { name: /Salvează cheltuiala/i }))

    await waitFor(() => expect(createExpense).toHaveBeenCalledTimes(1))
    expect(screen.getByText('Cheltuială adăugată cu succes!')).toBeInTheDocument()
  })

  it('completează datele prin OCR la selectarea unei imagini', async () => {
    vi.mocked(processReceiptOCR).mockResolvedValueOnce({
      amount: 250,
      category: 'facturi',
      date: '2026-04-30T12:00:00',
    } as unknown as OcrResponseDTO)

    renderAddRoute()
    fireEvent.click(screen.getByText('Simulare Încărcare OCR'))
    expect(screen.getByText('Procesăm bonul tău…')).toBeInTheDocument()

    await waitFor(() => {
      expect((screen.getByLabelText(/Sumă \(RON\)/i) as HTMLInputElement).value).toBe('250')
      expect((screen.getByLabelText(/Categorie/i) as HTMLSelectElement).value).toBe('facturi')
    })
  })

  it('prepopulează formularul în modul edit și trimite updateExpense', async () => {
    const expense = {
      id: 42,
      amount: 88.4,
      category: 'transport',
      expenseDate: '2026-05-01T10:30:00Z',
      description: 'OMV',
      person: 'Eduard',
      userId: 'user-1',
      location: null,
    } as unknown as ApiExpenseListDto

    vi.mocked(updateExpense).mockResolvedValueOnce({
      ...expense,
      amount: 100,
      description: 'OMV București',
    } as unknown as ApiExpenseListDto)

    renderEditRoute(expense)

    expect(screen.getByText('Editează cheltuiala')).toBeInTheDocument()
    expect((screen.getByLabelText(/Sumă \(RON\)/i) as HTMLInputElement).value).toBe('88.4')
    expect((screen.getByLabelText(/Categorie/i) as HTMLSelectElement).value).toBe('transport')

    fireEvent.change(screen.getByLabelText(/Sumă \(RON\)/i), { target: { value: '100' } })
    fireEvent.change(screen.getByLabelText(/Descriere/i), { target: { value: 'OMV București' } })
    fireEvent.click(screen.getByRole('button', { name: /Salvează modificările/i }))

    await waitFor(() => expect(updateExpense).toHaveBeenCalledWith(42, expect.objectContaining({
      amount: 100,
      category: 'transport',
      expenseDate: '2026-05-01',
      description: 'OMV București',
      person: 'Eduard',
    })))
    expect(mockNavigate).toHaveBeenCalledWith('/expenses', { replace: true, state: { updatedExpense: expect.any(Object) } })
  })
})
