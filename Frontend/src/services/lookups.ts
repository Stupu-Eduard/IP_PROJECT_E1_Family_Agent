import { api } from './api'

export async function fetchCategoryNames(signal?: AbortSignal): Promise<string[]> {
  const response = await api.get<string[]>('/api/v1/categories', { signal })
  return response.data
}

export async function fetchUserNames(signal?: AbortSignal): Promise<string[]> {
  const response = await api.get<string[]>('/api/v1/users', { signal })
  return response.data
}
