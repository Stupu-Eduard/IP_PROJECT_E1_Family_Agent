import { useEffect } from 'react'
import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { isTokenExpired } from '../utils/jwt'

interface PrivateRouteProps {
  children: ReactNode
}

export default function PrivateRoute({ children }: PrivateRouteProps) {
  const token = useAuthStore((state) => state.token)
  const logout = useAuthStore((state) => state.logout)

  const isInvalidToken = !token || isTokenExpired(token)

  useEffect(() => {
    if (isInvalidToken) {
      logout()
    }
  }, [isInvalidToken, logout])

  if (isInvalidToken) {
    return <Navigate to="/login" replace />
  }

  return children
}

