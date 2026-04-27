import '@testing-library/jest-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import LogoutTopBar from './LogoutTopBar'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

const mockLogout = vi.fn()
vi.mock('../store/authStore', () => ({
  useAuthStore: (selector: any) => selector({ logout: mockLogout }),
}))

describe('LogoutTopBar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('randeaza butonul global de logout', () => {
    render(
      <BrowserRouter>
        <LogoutTopBar />
      </BrowserRouter>,
    )

    expect(screen.getByRole('button', { name: /logout/i })).toBeInTheDocument()
  })

  it('apeleaza logout si navigheaza catre login', () => {
    render(
      <BrowserRouter>
        <LogoutTopBar />
      </BrowserRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: /logout/i }))

    expect(mockLogout).toHaveBeenCalledTimes(1)
    expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
  })
})

