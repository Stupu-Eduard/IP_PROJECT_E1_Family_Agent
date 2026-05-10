import { describe, it, expect, vi, beforeEach } from 'vitest'
import { api } from './api'
import { updateCurrentUserProfile } from './profile'

vi.mock('./api', () => ({
  api: { put: vi.fn() },
}))

describe('profile service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('trimite un multipart request către /api/users/me cu avatarul atașat', async () => {
    const avatarFile = new File(['avatar-bytes'], 'avatar.png', { type: 'image/png' })
    vi.mocked(api.put).mockResolvedValueOnce({
      data: {
        user: {
          name: 'Alex Popescu',
          avatarUrl: 'https://cdn.test/avatar.png',
          role: 'Parent',
          email: 'alex@test.com',
          preferences: { theme: 'dark', language: 'en', emailNotifications: false },
        },
      },
    } as never)

    const result = await updateCurrentUserProfile({
      name: 'Alex Popescu',
      preferences: { theme: 'dark', language: 'en', emailNotifications: false },
      avatarFile,
    })

    expect(api.put).toHaveBeenCalledWith('/api/users/me', expect.any(FormData))
    const sentFormData = vi.mocked(api.put).mock.calls[0]?.[1] as FormData
    expect(sentFormData.get('name')).toBe('Alex Popescu')
    expect(sentFormData.get('preferences[theme]')).toBe('dark')
    expect(sentFormData.get('preferences[language]')).toBe('en')
    expect(sentFormData.get('preferences[emailNotifications]')).toBe('false')
    expect(sentFormData.get('avatar')).toBe(avatarFile)
    expect(result.name).toBe('Alex Popescu')
    expect(result.avatarUrl).toBe('https://cdn.test/avatar.png')
  })

  it('revine la valori implicite când răspunsul API este neașteptat', async () => {
    vi.mocked(api.put).mockResolvedValueOnce({ data: null } as never)

    const result = await updateCurrentUserProfile({
      name: '   ',
      preferences: { theme: 'system', language: 'ro', emailNotifications: true },
    })

    expect(result.name).toBe('Eduard P.')
    expect(result.preferences.language).toBe('ro')
  })
})
