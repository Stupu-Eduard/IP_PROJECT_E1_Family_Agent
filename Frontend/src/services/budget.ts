import { api } from './api'

export interface BudgetDTO {
    monthlyLimit: number
}

export async function fetchBudget(): Promise<BudgetDTO> {
    const response = await api.get<BudgetDTO>('/api/v1/budget')
    return response.data
}

export async function saveBudget(monthlyLimit: number): Promise<BudgetDTO> {
    const response = await api.put<BudgetDTO>('/api/v1/budget', { monthlyLimit })
    return response.data
}