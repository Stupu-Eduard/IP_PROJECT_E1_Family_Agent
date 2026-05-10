import { api } from './api'
import type { UserProfile } from '../types/UserProfileDTO'

export type UpdateCurrentUserProfileInput = {
  name: string
  preferences: UserProfile['preferences']
  avatarFile?: File | null
}

function normalizeProfileResponse(data: unknown): UserProfile {
  if (data && typeof data === 'object') {
    const maybeData = data as Record<string, unknown>
    const payload = (maybeData.user ?? maybeData.profile ?? maybeData.data ?? maybeData) as Record<string, unknown>
    const preferences = (payload.preferences ?? {}) as UserProfile['preferences']

    return {
      name: typeof payload.name === 'string' ? payload.name : 'Eduard P.',
      avatarUrl: typeof payload.avatarUrl === 'string' ? payload.avatarUrl : null,
      role: typeof payload.role === 'string' ? payload.role as UserProfile['role'] : undefined,
      email: typeof payload.email === 'string' ? payload.email : null,
      preferences: {
        theme: preferences.theme === 'dark' || preferences.theme === 'light' || preferences.theme === 'system' ? preferences.theme : 'system',
        language: preferences.language === 'en' ? 'en' : 'ro',
        emailNotifications: preferences.emailNotifications !== false,
      },
    }
  }

  return {
    name: 'Eduard P.',
    avatarUrl: null,
    role: 'Parent',
    email: null,
    preferences: {
      theme: 'system',
      language: 'ro',
      emailNotifications: true,
    },
  }
}

export async function updateCurrentUserProfile(input: UpdateCurrentUserProfileInput): Promise<UserProfile> {
  const formData = new FormData()
  formData.append('name', input.name)
  formData.append('preferences[theme]', input.preferences.theme)
  formData.append('preferences[language]', input.preferences.language)
  formData.append('preferences[emailNotifications]', String(input.preferences.emailNotifications))

  if (input.avatarFile) {
    formData.append('avatar', input.avatarFile)
  }

  const response = await api.put('/api/users/me', formData)
  return normalizeProfileResponse(response.data)
}
