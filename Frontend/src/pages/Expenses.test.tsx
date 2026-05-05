import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import type { ComponentProps } from 'react'
import { MemoryRouter } from 'react-router-dom'
import type { ApiExpenseListDto } from '../services/expenses'
import Expenses from './Expenses'
import * as expensesService from '../services/expenses'
import * as lookupsService from '../services/lookups'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../services/expenses', () => ({
  fetchExpenses: vi.fn(),
  deleteExpense: vi.fn(),
}))

vi.mock('../services/lookups', () => ({
  fetchCategoryNames: vi.fn(),
  fetchUserNames: vi.fn(),
}))

type AuthState = { token: string | null }
const mockAuthState: AuthState = { token: null }
vi.mock('../store/authStore', () => ({
  useAuthStore: (selector: (state: AuthState) => unknown) => selector(mockAuthState),
}))

type ExpensesStoreState = {
  expenses: ApiExpenseListDto[]
  setExpenses: (expenses: ApiExpenseListDto[]) => void
  removeExpense: (id: number) => void
}

const mockExpensesState: ExpensesStoreState = {
  expenses: [],
  setExpenses: (expenses) => {
    mockExpensesState.expenses = expenses
  },
  removeExpense: (id) => {
    mockExpensesState.expenses = mockExpensesState.expenses.filter((expense) => expense.id !== id)
  },
}

vi.mock('../store/expensesStore', () => ({
  useExpensesStore: (selector: (state: ExpensesStoreState) => unknown) => selector(mockExpensesState),
}))

const renderWithRouter = (initialEntries?: ComponentProps<typeof MemoryRouter>['initialEntries']) => render(
  <MemoryRouter initialEntries={initialEntries}>
    <Expenses />
  </MemoryRouter>,
)

const makeToken = (payload: object) => `header.${btoa(JSON.stringify(payload))}.signature`

describe('Expenses Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockAuthState.token = makeToken({ sub: 'user-1' })
    mockExpensesState.expenses = []
    vi.mocked(lookupsService.fetchCategoryNames).mockResolvedValue(['Alimentare', 'Transport'])
    vi.mocked(lookupsService.fetchUserNames).mockResolvedValue(['Eduard', 'Mihaela'])
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('afișează scheletul de loading la inițializare', async () => {
    vi.mocked(expensesService.fetchExpenses).mockImplementation(() => new Promise(() => {}))
    vi.mocked(lookupsService.fetchCategoryNames).mockImplementation(() => new Promise(() => {}))
    vi.mocked(lookupsService.fetchUserNames).mockImplementation(() => new Promise(() => {}))

    const { container } = renderWithRouter()

    expect(screen.getByText(/Istoric Cheltuieli/i)).toBeInTheDocument()
    expect(container.querySelectorAll('.skeleton').length).toBeGreaterThan(0)
  })

  it('nu crapă dacă store-ul de cheltuieli primește accidental o valoare non-array', async () => {
    vi.mocked(expensesService.fetchExpenses).mockResolvedValue([])
    mockExpensesState.expenses = undefined as unknown as ApiExpenseListDto[]

    renderWithRouter()

    expect(await screen.findByText(/Istoric Cheltuieli/i)).toBeInTheDocument()
    expect(screen.queryByText(/Nu am putut încărca cheltuielile/i)).not.toBeInTheDocument()
  })

  it('afișează acțiunile pentru cheltuielile proprietarului', async () => {
    vi.mocked(expensesService.fetchExpenses).mockResolvedValue([
      {
        id: 1,
        amount: 50,
        category: 'Alimentare',
        description: 'Mega Image',
        expenseDate: '2026-05-05T12:00:00Z',
        person: 'Eduard',
        userId: 'user-1',
        location: { id: 9, store: 'Mega', city: 'Cluj' },
      },
    ] as unknown as ApiExpenseListDto[])

    renderWithRouter()

    await screen.findByText('Mega Image')
    expect(screen.getByLabelText('Editează cheltuiala')).toBeInTheDocument()
    expect(screen.getByLabelText('Șterge cheltuiala')).toBeInTheDocument()
  })

  it('ascunde acțiunile pentru cheltuielile altui membru', async () => {
    vi.mocked(expensesService.fetchExpenses).mockResolvedValue([
      {
        id: 2,
        amount: 40,
        category: 'Transport',
        description: 'OMV',
        expenseDate: '2026-05-05T12:00:00Z',
        person: 'Mihaela',
        userId: 'user-2',
        location: { id: 10, store: 'OMV', city: 'Cluj' },
      },
    ] as unknown as ApiExpenseListDto[])

    renderWithRouter()

    await screen.findByText('OMV')
    expect(screen.queryByLabelText('Editează cheltuiala')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Șterge cheltuiala')).not.toBeInTheDocument()
    expect(screen.getByText('Doar citire')).toBeInTheDocument()
  })

  it('deschide confirmarea și șterge optimist cheltuiala', async () => {
    vi.mocked(expensesService.fetchExpenses).mockResolvedValue([
      {
        id: 3,
        amount: 75,
        category: 'Alimentare',
        description: 'Carrefour',
        expenseDate: '2026-05-05T12:00:00Z',
        person: 'Eduard',
        userId: 'user-1',
        location: { id: 11, store: 'Carrefour', city: 'Cluj' },
      },
    ] as unknown as ApiExpenseListDto[])
    vi.mocked(expensesService.deleteExpense).mockResolvedValue(undefined)

    renderWithRouter()

    await screen.findByText('Carrefour')
    fireEvent.click(screen.getByLabelText('Șterge cheltuiala'))

    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /^Șterge$/i }))

    await waitFor(() => expect(expensesService.deleteExpense).toHaveBeenCalledWith(3))
    await waitFor(() => expect(screen.queryByText('Carrefour')).not.toBeInTheDocument())
  })

  it('navighează la edit pentru cheltuiala selectată', async () => {
    const expense = {
      id: 4,
      amount: 60,
      category: 'Alimentare',
      description: 'Auchan',
      expenseDate: '2026-05-05T12:00:00Z',
      person: 'Eduard',
      userId: 'user-1',
      location: { id: 12, store: 'Auchan', city: 'Cluj' },
    } as ApiExpenseListDto
    vi.mocked(expensesService.fetchExpenses).mockResolvedValue([expense])

    renderWithRouter()

    await screen.findByText('Auchan')
    fireEvent.click(screen.getByLabelText('Editează cheltuiala'))

    expect(mockNavigate).toHaveBeenCalledWith('/expenses/4/edit', { state: { expense } })
  })

  it('aplică filtrele și resetează lista fără reload', async () => {
    vi.mocked(expensesService.fetchExpenses).mockResolvedValue([])
    const { container } = renderWithRouter()

    await screen.findByText(/Istoric Cheltuieli/i)
    const dateInputs = container.querySelectorAll('input[type="date"]')
    fireEvent.change(dateInputs[0], { target: { value: '2026-05-01' } })
    fireEvent.change(dateInputs[1], { target: { value: '2026-05-31' } })

    fireEvent.change(screen.getAllByRole('combobox')[0], { target: { value: 'Alimentare' } })
    fireEvent.change(screen.getAllByRole('combobox')[1], { target: { value: 'Eduard' } })

    await waitFor(() => {
      expect(expensesService.fetchExpenses).toHaveBeenCalledWith(
        { category: 'Alimentare', person: 'Eduard' },
        expect.any(AbortSignal),
      )
    })

    fireEvent.click(screen.getByText(/Resetează/i))

    await waitFor(() => {
      expect(expensesService.fetchExpenses).toHaveBeenCalledWith(
        { category: undefined, person: undefined },
        expect.any(AbortSignal),
      )
    })
  })

  it('preia filtrele inițiale trimise din Reports prin navigation state', async () => {
    vi.mocked(expensesService.fetchExpenses).mockResolvedValue([])

    renderWithRouter([
      {
        pathname: '/expenses',
        state: {
          initialFilters: {
            selectedCategory: 'Transport',
            startDate: '2026-05-01',
            endDate: '2026-05-31',
          },
        },
      },
    ])

    await waitFor(() => {
      expect(expensesService.fetchExpenses).toHaveBeenCalledWith(
        { category: 'Transport', person: undefined },
        expect.any(AbortSignal),
      )
    })
  })
})