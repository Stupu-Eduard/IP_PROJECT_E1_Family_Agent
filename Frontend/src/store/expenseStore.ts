import { create } from 'zustand'

interface ExpenseState {
    version: number
    notifyExpenseAdded: () => void
}

export const useExpenseStore = create<ExpenseState>((set) => ({
    version: 0,
    notifyExpenseAdded: () => set((s) => ({ version: s.version + 1 })),
}))
