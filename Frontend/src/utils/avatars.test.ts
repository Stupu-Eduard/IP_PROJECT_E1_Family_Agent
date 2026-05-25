import { describe, it, expect } from 'vitest'
import { findPredefinedAvatar, isRemoteAvatarUrl, PREDEFINED_AVATARS } from './avatars'

describe('avatars utils', () => {

    // ─── findPredefinedAvatar ─────────────────────────────────────────────

    it('1. findPredefinedAvatar returnează avatarul corect pentru un ID valid', () => {
        const result = findPredefinedAvatar('av_f_pink')

        expect(result).not.toBeNull()
        expect(result?.id).toBe('av_f_pink')
        expect(result?.bg).toBe('#FBEAF0')
        expect(result?.fg).toBe('#72243E')
        expect(result?.variant).toBe('F')
        expect(result?.label).toBe('Feminin Roz')
    })

    it('2. findPredefinedAvatar returnează null pentru un ID inexistent', () => {
        expect(findPredefinedAvatar('av_invalid')).toBeNull()
    })

    it('3. findPredefinedAvatar returnează null când avatarUrl este null', () => {
        expect(findPredefinedAvatar(null)).toBeNull()
    })

    it('4. findPredefinedAvatar returnează null pentru un URL remote (http)', () => {
        expect(findPredefinedAvatar('https://res.cloudinary.com/demo/avatar.jpg')).toBeNull()
    })

    it('5. findPredefinedAvatar funcționează corect pentru toate cele 10 avataruri predefinite', () => {
        for (const avatar of PREDEFINED_AVATARS) {
            const result = findPredefinedAvatar(avatar.id)
            expect(result?.id).toBe(avatar.id)
        }
    })

    // ─── isRemoteAvatarUrl ────────────────────────────────────────────────

    it('6. isRemoteAvatarUrl returnează true pentru URL cu https', () => {
        expect(isRemoteAvatarUrl('https://res.cloudinary.com/demo/avatar.jpg')).toBe(true)
    })

    it('7. isRemoteAvatarUrl returnează true pentru URL cu http', () => {
        expect(isRemoteAvatarUrl('http://example.com/avatar.jpg')).toBe(true)
    })

    it('8. isRemoteAvatarUrl returnează true pentru blob URL', () => {
        expect(isRemoteAvatarUrl('blob:http://localhost:5173/abc-123')).toBe(true)
    })

    it('9. isRemoteAvatarUrl returnează false pentru un ID predefinit', () => {
        expect(isRemoteAvatarUrl('av_f_pink')).toBe(false)
        expect(isRemoteAvatarUrl('av_m_dark')).toBe(false)
    })

    it('10. isRemoteAvatarUrl returnează false pentru null', () => {
        expect(isRemoteAvatarUrl(null)).toBe(false)
    })

    it('11. isRemoteAvatarUrl returnează false pentru string gol', () => {
        expect(isRemoteAvatarUrl('')).toBe(false)
    })
})
