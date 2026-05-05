import axios from 'axios'
import { api } from './api'

export interface LoginResponseDto {
  message: string
  userName?: string
}

export async function loginWithEmailPassword(email: string, password: string): Promise<LoginResponseDto> {
  const response = await api.post<LoginResponseDto>('/api/v1/auth/login', { email, password })
  return response.data
}

export function getLoginErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as unknown
    if (data && typeof data === 'object' && 'error' in data) {
      const msg = (data as { error?: unknown }).error
      if (typeof msg === 'string' && msg.trim()) return msg
    }

    if (typeof err.message === 'string' && err.message.trim()) return err.message
  }

  if (err instanceof Error) return err.message

  return 'A apărut o eroare neașteptată.'
}
