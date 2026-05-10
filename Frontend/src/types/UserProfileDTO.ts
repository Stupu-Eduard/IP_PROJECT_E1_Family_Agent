export type UserRole = 'Parent' | 'Co-Parent' | 'Child'

export type ProfileTheme = 'system' | 'light' | 'dark'
export type ProfileLanguage = 'ro' | 'en'

export interface UserPreferences {
  theme: ProfileTheme
  language: ProfileLanguage
  emailNotifications: boolean
}

export interface UserProfile {
  name: string
  avatarUrl: string | null
  preferences: UserPreferences
  role?: UserRole
  email?: string | null
}

export type UserProfileUpdate = Partial<Omit<UserProfile, 'preferences'>> & {
  preferences?: Partial<UserPreferences>
}

export const DEFAULT_USER_PREFERENCES: UserPreferences = {
  theme: 'system',
  language: 'ro',
  emailNotifications: true,
}

export const DEFAULT_USER_PROFILE: UserProfile = {
  name: 'Eduard P.',
  avatarUrl: null,
  preferences: DEFAULT_USER_PREFERENCES,
  role: 'Parent',
}
