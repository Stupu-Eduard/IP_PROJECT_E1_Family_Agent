import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import PrivateRoute from './PrivateRoute'

const mockLogout = vi.fn()
const mockIsTokenExpired = vi.fn<(token: string) => boolean>()

let mockedToken: string | null = null

vi.mock('../store/authStore', () => ({
  useAuthStore: (selector: any) =>
    selector({
      token: mockedToken,
      logout: mockLogout,
    }),
}))

vi.mock('../utils/jwt', () => ({
  isTokenExpired: (token: string) => mockIsTokenExpired(token),
}))

function renderPrivateRoute() {
  return render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route
          path="/protected"
          element={
            <PrivateRoute>
              <div>Protected content</div>
            </PrivateRoute>
          }
        />
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('PrivateRoute', () => {
  beforeEach(() => {
    mockLogout.mockClear()
    mockIsTokenExpired.mockReset()
    mockedToken = null
  })

  it('permite accesul când există un token valid', () => {
    mockedToken = 'valid.token.value'
    mockIsTokenExpired.mockReturnValue(false)

    renderPrivateRoute()

    expect(screen.getByText('Protected content')).toBeInTheDocument()
    expect(screen.queryByText('Login page')).not.toBeInTheDocument()
    expect(mockLogout).not.toHaveBeenCalled()
  })

  it('redirecționează spre login și face logout când token-ul lipsește', async () => {
    mockedToken = null

    renderPrivateRoute()

    expect(await screen.findByText('Login page')).toBeInTheDocument()
    await waitFor(() => expect(mockLogout).toHaveBeenCalledTimes(1))
    expect(mockIsTokenExpired).not.toHaveBeenCalled()
  })

  it('redirecționează spre login și face logout când token-ul este expirat', async () => {
    mockedToken = 'expired.token.value'
    mockIsTokenExpired.mockReturnValue(true)

    renderPrivateRoute()

    expect(await screen.findByText('Login page')).toBeInTheDocument()
    await waitFor(() => expect(mockLogout).toHaveBeenCalledTimes(1))
    expect(mockIsTokenExpired).toHaveBeenCalledWith('expired.token.value')
  })
})

