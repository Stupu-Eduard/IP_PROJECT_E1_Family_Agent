import { create } from 'zustand'

export const AUTH_TOKEN_STORAGE_KEY = 'auth_token'
export const AVATAR_STORAGE_KEY = 'user_avatar'

interface AuthState {
    token: string | null
    isAuthenticated: boolean
    avatarUrl: string | null
    setToken: (token: string | null) => void
    login: (token: string) => void
    logout: () => void
    setAvatar: (url: string | null) => void
}

export const useAuthStore = create<AuthState>((set) => ({
    token: null,
    isAuthenticated: false,
    avatarUrl: localStorage.getItem(AVATAR_STORAGE_KEY) ?? null,
    setToken: (token) => {
        if (token) {
            localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
            set({ token, isAuthenticated: true })
            return
        }

        localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
        set({ token: null, isAuthenticated: false })
    },
    login: (token) => {
        localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
        set({ token, isAuthenticated: true })
    },
    logout: () => {
        localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
        localStorage.removeItem(AVATAR_STORAGE_KEY)
        set({ token: null, isAuthenticated: false, avatarUrl: null })
    },
    setAvatar: (url) => {
        if (url) {
            localStorage.setItem(AVATAR_STORAGE_KEY, url)
        } else {
            localStorage.removeItem(AVATAR_STORAGE_KEY)
        }
        set({ avatarUrl: url })
    },
}))
