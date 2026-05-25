// @vitest-environment jsdom

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { useAuthStore, AUTH_TOKEN_STORAGE_KEY, AVATAR_STORAGE_KEY } from './authStore'

describe('AuthStore - Zustand & Persistență', () => {
    beforeEach(() => {
        // Clear sessionStorage by removing all items
        Object.keys(sessionStorage).forEach(key => {
            sessionStorage.removeItem(key)
        })
        localStorage.removeItem(AVATAR_STORAGE_KEY)
        useAuthStore.getState().logout()
    })

    afterEach(() => {
        // Clean up after each test
        Object.keys(sessionStorage).forEach(key => {
            sessionStorage.removeItem(key)
        })
        localStorage.removeItem(AVATAR_STORAGE_KEY)
    })

    it('ar trebui să inițializeze starea ca fiind neautentificată', () => {
        const state = useAuthStore.getState()
        expect(state.token).toBe(null)
        expect(state.isAuthenticated).toBe(false)
    })

    it('ar trebui să salveze token-ul în sessionStorage la apelarea login()', () => {
        const mockToken = 'token-de-test-123'
        useAuthStore.getState().login(mockToken)

        expect(useAuthStore.getState().token).toBe(mockToken)
        expect(useAuthStore.getState().isAuthenticated).toBe(true)
        expect(sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe(mockToken)
    })

    it('ar trebui să curețe sessionStorage și starea la apelarea logout()', () => {
        useAuthStore.getState().login('test-token')
        useAuthStore.getState().logout()

        expect(useAuthStore.getState().token).toBe(null)
        expect(useAuthStore.getState().isAuthenticated).toBe(false)
        expect(sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe(null)
    })

    it('4. setToken() - Ramura IF: salvează token dacă valoarea este validă', () => {
        const newToken = 'token-set-456'
        useAuthStore.getState().setToken(newToken)

        expect(useAuthStore.getState().token).toBe(newToken)
        expect(useAuthStore.getState().isAuthenticated).toBe(true)
        expect(sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe(newToken)
    })

    it('5. setToken() - Ramura ELSE: șterge token dacă valoarea este null', () => {
        useAuthStore.getState().setToken('temp-token')
        useAuthStore.getState().setToken(null)

        expect(useAuthStore.getState().token).toBe(null)
        expect(useAuthStore.getState().isAuthenticated).toBe(false)
        expect(sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe(null)
    })

    // ─── AVATAR ───────────────────────────────────────────────────────────

    it('6. avatarUrl este null inițial când localStorage e gol', () => {
        expect(useAuthStore.getState().avatarUrl).toBe(null)
    })

    it('7. setAvatar(url) salvează URL-ul în state și localStorage', () => {
        const url = 'https://res.cloudinary.com/demo/avatar.jpg'
        useAuthStore.getState().setAvatar(url)

        expect(useAuthStore.getState().avatarUrl).toBe(url)
        expect(localStorage.getItem(AVATAR_STORAGE_KEY)).toBe(url)
    })

    it('8. setAvatar(null) șterge avatarul din state și localStorage', () => {
        useAuthStore.getState().setAvatar('https://example.com/avatar.jpg')
        useAuthStore.getState().setAvatar(null)

        expect(useAuthStore.getState().avatarUrl).toBe(null)
        expect(localStorage.getItem(AVATAR_STORAGE_KEY)).toBe(null)
    })

    it('9. logout() șterge și avatarUrl din state și localStorage', () => {
        useAuthStore.getState().login('test-token')
        useAuthStore.getState().setAvatar('https://example.com/avatar.jpg')

        useAuthStore.getState().logout()

        expect(useAuthStore.getState().avatarUrl).toBe(null)
        expect(localStorage.getItem(AVATAR_STORAGE_KEY)).toBe(null)
    })

    it('10. setAvatar cu ID predefinit (av_f_pink) salvează corect ca string', () => {
        const avatarId = 'av_f_pink'
        useAuthStore.getState().setAvatar(avatarId)

        expect(useAuthStore.getState().avatarUrl).toBe(avatarId)
        expect(localStorage.getItem(AVATAR_STORAGE_KEY)).toBe(avatarId)
    })
})
