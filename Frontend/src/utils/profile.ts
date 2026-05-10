import { DEFAULT_USER_PROFILE } from '../types/UserProfileDTO'
import type { UserProfile, UserRole } from '../types/UserProfileDTO'
type TokenPayload = { role?: UserRole; sub?: string; name?: string; avatarUrl?: string | null }
function decodeTokenPayload(token: string | null | undefined): TokenPayload | null {
  if (!token) return null
  try {
    const parts = token.split('.')
    return parts.length === 3 ? JSON.parse(atob(parts[1])) as TokenPayload : null
  } catch {
    return null
  }
}
export const getProfileRole = (profile: UserProfile | null, token: string | null | undefined): UserRole =>
  profile?.role ?? decodeTokenPayload(token)?.role ?? DEFAULT_USER_PROFILE.role ?? 'Parent'
export const getProfileDisplayName = (profile: UserProfile | null, token: string | null | undefined, fallback = DEFAULT_USER_PROFILE.name): string => {
  if (profile?.name?.trim()) return profile.name.trim()
  const payload = decodeTokenPayload(token)
  if (payload?.name?.trim()) return payload.name.trim()
  const prefix = payload?.sub?.split('@')[0]?.trim()
  return prefix ? prefix.split(/[._-]/).filter(Boolean).map((s) => s[0]!.toUpperCase() + s.slice(1)).join(' ') : fallback
}
export const getProfileAvatarUrl = (profile: UserProfile | null, token: string | null | undefined): string | null =>
  profile?.avatarUrl ?? decodeTokenPayload(token)?.avatarUrl ?? null
export const getProfileInitials = (profile: UserProfile | null, token: string | null | undefined, fallback = DEFAULT_USER_PROFILE.name): string => {
  const name = getProfileDisplayName(profile, token, fallback)
  const parts = name.split(/\s+/).filter(Boolean)
  if (!parts.length) return 'FA'
  // Prefer first two letters of the first name (original app used 'ED' for Eduard)
  const first = parts[0]!
  return first.slice(0, 2).toUpperCase()
}
export const getProfileRoleLabel = (role: UserRole): string => role === 'Child' ? 'Copil · Activ' : role === 'Co-Parent' ? 'Co-Părinte · Activ' : 'Părinte · Activ'
