import type { OcrResponseDTO } from '../types/OcrResponseDTO';
import type { ExpenseDTO } from '../types/ExpenseDTO';
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

export async function processReceiptOCR(file: File): Promise<OcrResponseDTO> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await api.post<OcrResponseDTO>('/api/v1/ocr/process', formData);

  return response.data;
}

export async function createExpense(payload: ExpenseDTO): Promise<ApiExpenseListDto> {
  const body = {
    amount: payload.amount,
    category: payload.category,
    date: payload.date,
    currency: payload.currency ?? 'RON',
    description: payload.description ?? null,
    locationName: payload.locationName ?? null,
  };

  const response = await api.post<ApiExpenseListDto>('/api/v1/expenses', body);
  return response.data;
}
