import { describe, it, expect } from 'vitest'
import { isTokenExpired } from './jwt'

describe('JWT Utility - isTokenExpired', () => {
    it('ar trebui să returneze true pentru un token complet invalid', () => {
        expect(isTokenExpired('not.a.token')).toBe(true)
    })

    it('ar trebui să returneze true dacă token-ul a expirat', () => {
        const expiredPayload = btoa(JSON.stringify({ exp: 1000 }))
        const expiredToken = `header.${expiredPayload}.signature`

        expect(isTokenExpired(expiredToken)).toBe(true)
    })

    it('ar trebui să returneze false dacă token-ul este încă valabil', () => {
        const futureDate = Math.floor(Date.now() / 1000) + 3600
        const validPayload = btoa(JSON.stringify({ exp: futureDate }))
        const validToken = `header.${validPayload}.signature`

        expect(isTokenExpired(validToken)).toBe(false)
    })
})