import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import ForgotPassword from './ForgotPassword'

describe('ForgotPassword Component', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderComponent = () => render(<BrowserRouter><ForgotPassword /></BrowserRouter>)

    it('ar trebui să randeze elementele vizuale corecte', () => {
        renderComponent()

        expect(screen.getByText('Resetare Parolă')).toBeInTheDocument()
        expect(screen.getByPlaceholderText('adresa@exemplu.com')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: /Trimite Link/i })).toBeInTheDocument()
    })

    it('ar trebui să afișeze eroare dacă se trimite un email invalid', async () => {
        renderComponent()

        const emailInput = screen.getByPlaceholderText('adresa@exemplu.com')
        const form = emailInput.closest('form')!

        fireEvent.change(emailInput, { target: { value: 'email-fara-aron' } })

        await act(async () => {
            // Folosim submit pe formular pentru a by-passa validarea nativă HTML5 din browser/JSDOM
            fireEvent.submit(form)
        })

        await waitFor(() => {
            expect(screen.getByText(/Email invalid/i)).toBeInTheDocument()
        })
    })

    it('ar trebui să simuleze trimiterea cu succes și să afișeze mesajul de confirmare', async () => {
        renderComponent()

        const emailInput = screen.getByPlaceholderText('adresa@exemplu.com')
        const form = emailInput.closest('form')!

        fireEvent.change(emailInput, { target: { value: 'user@example.com' } })

        await act(async () => {
            fireEvent.submit(form)
        })

        await waitFor(() => {
            expect(screen.getByText(/vei primi un link pentru resetarea parolei/i)).toBeInTheDocument()
        }, { timeout: 2500 })

        expect(emailInput).toBeDisabled()
    })
})