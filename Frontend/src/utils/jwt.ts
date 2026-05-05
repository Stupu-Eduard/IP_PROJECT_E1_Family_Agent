export interface JwtPayload {
  exp?: number
  sub?: string
  email?: string
  name?: string
  id?: string | number
  userId?: string | number
  role?: string
}

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const paddingLength = (4 - (normalized.length % 4)) % 4
  const padded = normalized.padEnd(normalized.length + paddingLength, '=')

  return atob(padded)
}

export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      return null
    }

    const payload = JSON.parse(decodeBase64Url(parts[1])) as JwtPayload
    return payload
  } catch {
    return null
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodeJwtPayload(token)
  if (!payload?.exp) {
    return true
  }

  const nowInSeconds = Math.floor(Date.now() / 1000)
  return payload.exp <= nowInSeconds
}

