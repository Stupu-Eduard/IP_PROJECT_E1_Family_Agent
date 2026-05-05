import { create } from 'zustand'
import type { ApiExpenseListDto } from '../services/expenses'

interface ExpensesState {
  expenses: ApiExpenseListDto[]
  setExpenses: (expenses: ApiExpenseListDto[]) => void
  upsertExpense: (expense: ApiExpenseListDto) => void
  removeExpense: (id: number) => void
  getExpenseById: (id: number) => ApiExpenseListDto | undefined
}

export const useExpensesStore = create<ExpensesState>((set, get) => ({
  expenses: [],
  setExpenses: (expenses) => set({ expenses }),
  upsertExpense: (expense) => set((state) => {
    const index = state.expenses.findIndex((item) => item.id === expense.id)
    if (index === -1) {
      return { expenses: [expense, ...state.expenses] }
    }

    const next = [...state.expenses]
    next[index] = expense
    return { expenses: next }
  }),
  removeExpense: (id) => set((state) => ({ expenses: state.expenses.filter((expense) => expense.id !== id) })),
  getExpenseById: (id) => get().expenses.find((expense) => expense.id === id),
}))

