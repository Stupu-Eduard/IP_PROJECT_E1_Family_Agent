import { AUTH_PROFILE_STORAGE_KEY, AUTH_TOKEN_STORAGE_KEY, useAuthStore } from '../store/authStore'
import { isTokenExpired } from './jwt'

export function initializeAuthSession(): void {
  const token = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  const profileRaw = localStorage.getItem(AUTH_PROFILE_STORAGE_KEY)
  const { login, logout } = useAuthStore.getState()

  if (!token || isTokenExpired(token)) {
    logout()
    return
  }

  login(token)

  if (!profileRaw) {
    useAuthStore.getState().setProfile(null)
    return
  }

  try {
    useAuthStore.getState().setProfile(JSON.parse(profileRaw))
  } catch {
    useAuthStore.getState().setProfile(null)
  }
}

