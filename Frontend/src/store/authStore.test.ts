// @vitest-environment jsdom

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { useAuthStore, AUTH_TOKEN_STORAGE_KEY } from './authStore'

describe('AuthStore - Zustand & Persistență', () => {
    beforeEach(() => {
        // Clear sessionStorage by removing all items
        Object.keys(sessionStorage).forEach(key => {
            sessionStorage.removeItem(key)
        })
        useAuthStore.getState().logout()
    })

    afterEach(() => {
        // Clean up after each test
        Object.keys(sessionStorage).forEach(key => {
            sessionStorage.removeItem(key)
        })
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
        // Mai întâi setăm un token
        useAuthStore.getState().setToken('temp-token')

        // Apoi apelăm setToken cu null (pentru a intra pe ramura else)
        useAuthStore.getState().setToken(null)

        expect(useAuthStore.getState().token).toBe(null)
        expect(useAuthStore.getState().isAuthenticated).toBe(false)
        expect(sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe(null)
    })
})
