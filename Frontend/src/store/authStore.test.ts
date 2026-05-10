// @vitest-environment jsdom

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { useAuthStore, AUTH_TOKEN_STORAGE_KEY, AUTH_PROFILE_STORAGE_KEY } from './authStore'

describe('AuthStore - Zustand & Persistență', () => {
    beforeEach(() => {
        // Clear localStorage by removing all items
        Object.keys(localStorage).forEach(key => {
            localStorage.removeItem(key)
        })
        useAuthStore.getState().logout()
    })

    afterEach(() => {
        // Clean up after each test
        Object.keys(localStorage).forEach(key => {
            localStorage.removeItem(key)
        })
    })

    it('ar trebui să inițializeze starea ca fiind neautentificată', () => {
        const state = useAuthStore.getState()
        expect(state.token).toBe(null)
        expect(state.isAuthenticated).toBe(false)
    })

    it('ar trebui să salveze token-ul în localStorage la apelarea login()', () => {
        const mockToken = 'token-de-test-123'
        useAuthStore.getState().login(mockToken)

        expect(useAuthStore.getState().token).toBe(mockToken)
        expect(useAuthStore.getState().isAuthenticated).toBe(true)
        expect(localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe(mockToken)
    })

    it('ar trebui să curețe localStorage și starea la apelarea logout()', () => {
        useAuthStore.getState().login('test-token')
        useAuthStore.getState().setProfile({
            name: 'Alex Popescu',
            avatarUrl: 'https://cdn.test/avatar.png',
            role: 'Parent',
            preferences: { theme: 'dark', language: 'en', emailNotifications: false },
        })
        useAuthStore.getState().logout()

        expect(useAuthStore.getState().token).toBe(null)
        expect(useAuthStore.getState().isAuthenticated).toBe(false)
        expect(localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe(null)
        expect(localStorage.getItem(AUTH_PROFILE_STORAGE_KEY)).toBe(null)
        expect(useAuthStore.getState().profile).toBe(null)
    })

    it('4. setToken() - Ramura IF: salvează token dacă valoarea este validă', () => {
        const newToken = 'token-set-456'
        useAuthStore.getState().setToken(newToken)

        expect(useAuthStore.getState().token).toBe(newToken)
        expect(useAuthStore.getState().isAuthenticated).toBe(true)
        expect(localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe(newToken)
    })

    it('5. setToken() - Ramura ELSE: șterge token dacă valoarea este null', () => {
        // Mai întâi setăm un token
        useAuthStore.getState().setToken('temp-token')

        // Apoi apelăm setToken cu null (pentru a intra pe ramura else)
        useAuthStore.getState().setToken(null)

        expect(useAuthStore.getState().token).toBe(null)
        expect(useAuthStore.getState().isAuthenticated).toBe(false)
        expect(localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe(null)
    })

    it('6. setProfile() și updateProfile() persistă și fuzionează preferințele', () => {
        useAuthStore.getState().setProfile({
            name: 'Maria Popescu',
            avatarUrl: null,
            role: 'Parent',
            preferences: { theme: 'light', language: 'ro', emailNotifications: false },
        })

        useAuthStore.getState().updateProfile({
            name: 'Maria Ionescu',
            preferences: { emailNotifications: true },
        })

        expect(useAuthStore.getState().profile).toEqual({
            name: 'Maria Ionescu',
            avatarUrl: null,
            role: 'Parent',
            preferences: { theme: 'light', language: 'ro', emailNotifications: true },
        })
        expect(JSON.parse(localStorage.getItem(AUTH_PROFILE_STORAGE_KEY) as string)).toEqual(useAuthStore.getState().profile)
    })

    it('7. setToken(null) curăță și profilul salvat', () => {
        useAuthStore.getState().setProfile({
            name: 'Alex Popescu',
            avatarUrl: null,
            role: 'Parent',
            preferences: { theme: 'system', language: 'ro', emailNotifications: true },
        })

        useAuthStore.getState().setToken(null)

        expect(useAuthStore.getState().profile).toBe(null)
        expect(localStorage.getItem(AUTH_PROFILE_STORAGE_KEY)).toBe(null)
    })
})
