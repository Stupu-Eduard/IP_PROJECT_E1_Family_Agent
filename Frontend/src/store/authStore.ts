import { create } from 'zustand'

export const AUTH_TOKEN_STORAGE_KEY = 'auth_token'

interface AuthState {
    token: string | null
    isAuthenticated: boolean
    setToken: (token: string | null) => void
    login: (token: string) => void
    logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
    token: null,
    isAuthenticated: false,
    setToken: (token) => {
        if (token) {
            sessionStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
            set({ token, isAuthenticated: true })
            return
        }

        sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
        set({ token: null, isAuthenticated: false })
    },
    login: (token) => {
        sessionStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
        set({ token, isAuthenticated: true })
    },
    logout: () => {
        sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
        set({ token: null, isAuthenticated: false })
    },
}))
