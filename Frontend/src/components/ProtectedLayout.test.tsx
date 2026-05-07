import '@testing-library/jest-dom'
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ProtectedLayout from './ProtectedLayout'
import { useAuthStore } from '../store/authStore'

vi.mock('../store/authStore', () => ({
    useAuthStore: vi.fn()
}))

// Mock pentru componentele copil
vi.mock('./Sidebar', () => ({ default: () => <div data-testid="sidebar">Sidebar Mock</div> }))
vi.mock('./ChatAi', () => ({ default: () => <div data-testid="chatai">ChatAI Mock</div> }))

describe('ProtectedLayout Component', () => {

    const renderComponent = () => render(
        <MemoryRouter>
            <ProtectedLayout />
        </MemoryRouter>
    )

    const createMockToken = (role: string) => {
        const payload = { role }
        return `header.${btoa(JSON.stringify(payload))}.signature`
    }

    it('1. Randează Sidebar și Outlet-ul implicit', () => {
        vi.mocked(useAuthStore).mockReturnValueOnce(null) // Fără token
        renderComponent()
        expect(screen.getByTestId('sidebar')).toBeInTheDocument()
        expect(screen.getByRole('main')).toHaveClass('fa-layout-content')
    })

    it('2. Randează ChatAI dacă utilizatorul NU este copil (Părinte)', () => {
        // Mock token pentru Parent
        vi.mocked(useAuthStore).mockReturnValueOnce(createMockToken('Parent'))
        renderComponent()
        expect(screen.getByTestId('chatai')).toBeInTheDocument()
    })

    it('3. NU randează ChatAI dacă utilizatorul are rolul de Child', () => {
        // Mock token pentru Child
        vi.mocked(useAuthStore).mockReturnValueOnce(createMockToken('Child'))
        renderComponent()
        expect(screen.queryByTestId('chatai')).not.toBeInTheDocument()
    })

    it('4. Randează ChatAI dacă decodarea token-ului eșuează (fallback la părinte)', () => {
        vi.mocked(useAuthStore).mockReturnValueOnce('token-invalid-fara-puncte')
        renderComponent()
        expect(screen.getByTestId('chatai')).toBeInTheDocument()
    })
})