import { create } from 'zustand'
import { DEFAULT_USER_PROFILE } from '../types/UserProfileDTO'
import type { UserProfile, UserProfileUpdate } from '../types/UserProfileDTO'

export const AUTH_TOKEN_STORAGE_KEY = 'auth_token'
export const AUTH_PROFILE_STORAGE_KEY = 'auth_profile'

const readStoredProfile = (): UserProfile | null => {
    const rawProfile = localStorage.getItem(AUTH_PROFILE_STORAGE_KEY)
    if (!rawProfile) return null

    try {
        return JSON.parse(rawProfile) as UserProfile
    } catch {
        return null
    }
}

const writeStoredProfile = (profile: UserProfile | null) => {
    if (!profile) {
        localStorage.removeItem(AUTH_PROFILE_STORAGE_KEY)
        return
    }

    localStorage.setItem(AUTH_PROFILE_STORAGE_KEY, JSON.stringify(profile))
}

const mergeProfile = (current: UserProfile | null, next: UserProfileUpdate): UserProfile => {
    const base = current ?? DEFAULT_USER_PROFILE

    return {
        ...base,
        ...next,
        preferences: {
            ...base.preferences,
            ...next.preferences,
        },
    }
}

interface AuthState {
    token: string | null
    isAuthenticated: boolean
    profile: UserProfile | null
    setToken: (token: string | null) => void
    login: (token: string, profile?: UserProfile | null) => void
    logout: () => void
    setProfile: (profile: UserProfile | null) => void
    updateProfile: (profile: UserProfileUpdate) => void
}

export const useAuthStore = create<AuthState>((set) => ({
    token: null,
    isAuthenticated: false,
    profile: null,
    setToken: (token) => {
        if (token) {
            localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
            set({ token, isAuthenticated: true })
            return
        }

        localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
        writeStoredProfile(null)
        set({ token: null, isAuthenticated: false, profile: null })
    },
    login: (token, profile) => {
        localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
        if (profile !== undefined) {
            writeStoredProfile(profile)
            set({ token, isAuthenticated: true, profile })
            return
        }

        set({ token, isAuthenticated: true })
    },
    logout: () => {
        localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
        writeStoredProfile(null)
        set({ token: null, isAuthenticated: false, profile: null })
    },
    setProfile: (profile) => {
        writeStoredProfile(profile)
        set({ profile })
    },
    updateProfile: (profile) => {
        const nextProfile = mergeProfile(readStoredProfile(), profile)
        writeStoredProfile(nextProfile)
        set({ profile: nextProfile })
    },
}))
