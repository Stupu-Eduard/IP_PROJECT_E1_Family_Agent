// @vitest-environment jsdom

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { useAuthStore, AUTH_TOKEN_STORAGE_KEY } from './authStore'

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
        useAuthStore.getState().logout()

        expect(useAuthStore.getState().token).toBe(null)
        expect(localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe(null)
    })
})