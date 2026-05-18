import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import ResetPassword from './ResetPassword'
import { api } from '../services/api'

vi.mock('../services/api', () => ({
    api: {
        post: vi.fn(),
    },
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

describe('ResetPassword Component', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        sessionStorage.clear()
        sessionStorage.setItem('resetPayload', JSON.stringify({
            email: 'reset@example.com',
            question1: 'ANIMAL',
            answer1: 'cat',
            question2: 'COLOR',
            answer2: 'blue',
        }))
        ;(api.post as any).mockResolvedValue({ data: { message: 'Parola a fost actualizată.' } })
    })

    const renderComponent = () => render(
        <MemoryRouter initialEntries={['/reset-password']}>
            <Routes>
                <Route path="/reset-password" element={<ResetPassword />} />
                <Route path="/login" element={<div>Login Page</div>} />
            </Routes>
        </MemoryRouter>
    )

    it('1. Randează formularul inițial', () => {
        renderComponent()
        expect(screen.getByText(/Parolă nouă/i)).toBeInTheDocument()
        expect(screen.getByRole('button', { name: /Schimbă parola/i })).toBeInTheDocument()
    })

    it('2. Arată eroare dacă parola este prea scurtă', async () => {
        renderComponent()
        fireEvent.change(screen.getByLabelText?.(/PAROLĂ NOUĂ/i) ?? screen.getAllByPlaceholderText(/••••••••/i)[0], {
            target: { value: '123' },
        })
        fireEvent.submit(screen.getByRole('button', { name: /Schimbă parola/i }))

        const error = await screen.findByText((content, element) => {
            const hasText = content.includes('Minim 8 caractere')
            return hasText && element?.tagName.toLowerCase() === 'div'
        })

        expect(error).toBeInTheDocument()
    })

    it('3. Resetează parola cu succes și navighează la login', async () => {
        renderComponent()

        const passFields = screen.getAllByPlaceholderText(/••••••••/i)
        fireEvent.change(passFields[0], { target: { value: 'Password123!' } })
        fireEvent.change(passFields[1], { target: { value: 'Password123!' } })
        fireEvent.submit(screen.getByRole('button', { name: /Schimbă parola/i }))

        expect(await screen.findByText(/Se procesează/i)).toBeInTheDocument()

        await waitFor(() => {
            expect(api.post).toHaveBeenCalledWith('/api/v1/auth/reset-password', {
                email: 'reset@example.com',
                question1: 'ANIMAL',
                answer1: 'cat',
                question2: 'COLOR',
                answer2: 'blue',
                newPassword: 'Password123!',
            })
            expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })
        })
    })

    it('4. Arată eroare dacă lipsesc datele de verificare', async () => {
        sessionStorage.clear()
        renderComponent()

        const passFields = screen.getAllByPlaceholderText(/••••••••/i)
        fireEvent.change(passFields[0], { target: { value: 'Password123!' } })
        fireEvent.change(passFields[1], { target: { value: 'Password123!' } })
        fireEvent.submit(screen.getByRole('button', { name: /Schimbă parola/i }))

        const error = await screen.findByText((content, element) => {
            const hasText = content.includes('Lipsesc datele de verificare')
            return hasText && element?.tagName.toLowerCase() === 'div'
        })

        expect(error).toBeInTheDocument()
    })
})
