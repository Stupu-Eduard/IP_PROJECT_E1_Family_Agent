import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Dashboard from './Dashboard'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

const mockLogout = vi.fn()
vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector({ logout: mockLogout }),
}))

describe('Dashboard Component - Redesign Core', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderComponent = () => render(<BrowserRouter><Dashboard /></BrowserRouter>)

    it('ar trebui să randeze interfața de redesign (Parent View)', () => {
        renderComponent()
        expect(screen.getByText(/Bine ai revenit,/i)).toBeInTheDocument()
        expect(screen.getByText('SUMAR LUNA CURENTĂ')).toBeInTheDocument()
    })

    it('ar trebui să navigheze corect către Adaugă Cheltuială', () => {
        renderComponent()
        // Căutăm textul butonului pe care l-am păstrat în varianta trunchiată
        const addExpenseAction = screen.getByText('Adaugă cheltuială').closest('div')!
        fireEvent.click(addExpenseAction)
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense')
    })

    it('ar trebui să execute logout corect', () => {
        renderComponent()
        const logoutButton = screen.getByTitle('Logout')
        fireEvent.click(logoutButton)
        expect(mockLogout).toHaveBeenCalledTimes(1)
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
    })
})