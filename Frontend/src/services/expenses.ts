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
  sourceType: string | null
  receiptUrl: string | null
}

export type ExpenseFilters = {
  date?: string
  category?: string
  person?: string
}

export async function fetchExpenses(filters?: ExpenseFilters, signal?: AbortSignal): Promise<ApiExpenseListDto[]> {
  const params: Record<string, string> = {}
  if (filters?.date) params.date = filters.date
  if (filters?.category) params.category = filters.category
  if (filters?.person) params.person = filters.person

  const response = await api.get<ApiExpenseListDto[]>('/api/v1/expenses', { params, signal })
  return response.data
}

export interface CreateExpenseDto {
  amount: number
  description?: string
  categoryName: string
  date: string  // YYYY-MM-DD
  storeName?: string
  city?: string
  receiptUrl?: string
}

export async function createExpense(payload: CreateExpenseDto): Promise<ApiExpenseListDto> {
  const response = await api.post<ApiExpenseListDto>('/api/v1/expenses', payload)
  return response.data
}

export async function updateExpense(id: number, payload: CreateExpenseDto): Promise<ApiExpenseListDto> {
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
