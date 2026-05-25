import { AUTH_TOKEN_STORAGE_KEY, AVATAR_STORAGE_KEY, useAuthStore } from '../store/authStore'
import { isTokenExpired } from './jwt'
import { profileApi } from '../services/api'

export function initializeAuthSession(): void {
  const token = sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  const { login, logout, setAvatar } = useAuthStore.getState()

  if (!token || isTokenExpired(token)) {
    logout()
    return
  }

  login(token)

  if (!localStorage.getItem(AVATAR_STORAGE_KEY)) {
    profileApi.getAvatar().then((res) => {
      if (res.data.avatarUrl) setAvatar(res.data.avatarUrl)
    }).catch(() => {})
  }
}

