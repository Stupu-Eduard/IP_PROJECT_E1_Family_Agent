import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { authApi } from '../services/api'
import { isTokenExpired } from '../utils/jwt'

interface PrivateRouteProps {
  children: ReactNode
}

export default function PrivateRoute({ children }: PrivateRouteProps) {
  const token    = useAuthStore((state) => state.token)
  const setToken = useAuthStore((state) => state.setToken)
  const logout   = useAuthStore((state) => state.logout)
  const refreshed = useRef(false)

  const isInvalidToken = !token || isTokenExpired(token)

  useEffect(() => {
    if (isInvalidToken) {
      logout()
      return
    }
    if (!refreshed.current) {
      refreshed.current = true
      authApi.refresh()
        .then(({ data }) => setToken(data.token))
        .catch(() => {})
    }
  }, [isInvalidToken, logout, setToken])

  if (isInvalidToken) {
    return <Navigate to="/login" replace />
  }

  return children
}

