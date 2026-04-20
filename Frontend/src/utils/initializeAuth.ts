import { AUTH_TOKEN_STORAGE_KEY, useAuthStore } from '../store/authStore'
import { isTokenExpired } from './jwt'

export function initializeAuthSession(): void {
  const token = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  const { login, logout } = useAuthStore.getState()

  if (!token || isTokenExpired(token)) {
    logout()
    return
  }

  login(token)
}

