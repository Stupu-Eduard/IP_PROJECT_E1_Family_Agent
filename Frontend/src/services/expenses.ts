import type { OcrResponseDTO } from '../types/OcrResponseDTO';
import { api } from './api'

export interface ApiLocationDto {
  id: number
  store: string | null
  address: string | null
  city: string | null
  country: string | null
  lat: number | null
  lng: number | null
}

export interface ApiExpenseListDto {
  id: number
  amount: string | number
  currency: string | null
  description: string | null
  expenseDate: string | null
  category: string | null
  person: string | null
  location: ApiLocationDto | null
  userId?: string | number | null
  ownerId?: string | number | null
  createdById?: string | number | null
  personId?: string | number | null
}

export interface ExpenseMutationPayload {
  amount: number
  category: string
  expenseDate: string
  description?: string
  person?: string
  currency?: string | null
  locationId?: number | null
  userId?: string | number | null
}

export type ExpenseFilters = {
  date?: string
  category?: string
  person?: string
}

const normalizeExpensesPayload = (payload: unknown): ApiExpenseListDto[] => {
  if (Array.isArray(payload)) {
    return payload as ApiExpenseListDto[]
  }

  if (payload && typeof payload === 'object') {
    const maybeWrapped = payload as { data?: unknown; items?: unknown }
    if (Array.isArray(maybeWrapped.data)) {
      return maybeWrapped.data as ApiExpenseListDto[]
    }
    if (Array.isArray(maybeWrapped.items)) {
      return maybeWrapped.items as ApiExpenseListDto[]
    }
  }

  return []
}

export async function fetchExpenses(filters?: ExpenseFilters, signal?: AbortSignal): Promise<ApiExpenseListDto[]> {
  const params: Record<string, string> = {}
  if (filters?.date) params.date = filters.date
  if (filters?.category) params.category = filters.category
  if (filters?.person) params.person = filters.person

  const response = await api.get<ApiExpenseListDto[]>('/api/v1/expenses', { params, signal })
  return normalizeExpensesPayload(response.data)
}

export async function createExpense(payload: ExpenseMutationPayload): Promise<ApiExpenseListDto> {
  const response = await api.post<ApiExpenseListDto>('/api/v1/expenses', payload)
  return response.data
}

export async function updateExpense(id: number, payload: ExpenseMutationPayload): Promise<ApiExpenseListDto> {
  const response = await api.put<ApiExpenseListDto>(`/api/v1/expenses/${id}`, payload)
  return response.data
}

export async function deleteExpense(id: number): Promise<void> {
  await api.delete(`/api/v1/expenses/${id}`)
}

export async function processReceiptOCR(file: File): Promise<OcrResponseDTO> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await api.post<OcrResponseDTO>('/api/v1/ocr/process', formData);

  return response.data;
}
