import '@testing-library/jest-dom'
import { render, screen, fireEvent} from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import ForgotPassword from './ForgotPassword'

describe('ForgotPassword Component', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderComponent = () => render(<MemoryRouter><ForgotPassword /></MemoryRouter>)

    it('1. Randează corect formularul inițial', () => {
        renderComponent()
        expect(screen.getByPlaceholderText('adresa@exemplu.com')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: /Trimite link de resetare/i })).toBeInTheDocument()
    })

    it('2. Arată eroare dacă email-ul este gol la submit', async () => {
        renderComponent()
        const input = screen.getByPlaceholderText('adresa@exemplu.com')
        const form = input.closest('form')!

        // Forțăm submit-ul pe formular pentru a ignora blocajele HTML5 de tip="email"
        fireEvent.submit(form)

        // Folosim un matcher funcțional pentru a găsi textul indiferent de ⚠ sau newline
        const errorMsg = await screen.findByText((content, element) => {
            const hasText = content.includes("Adresa de email este obligatorie")
            const isCorrectElement = element?.tagName.toLowerCase() === 'div'
            return hasText && isCorrectElement
        })

        expect(errorMsg).toBeInTheDocument()
    })

    it('3. Arată eroare dacă email-ul are format invalid', async () => {
        renderComponent()
        const input = screen.getByPlaceholderText('adresa@exemplu.com')
        const form = input.closest('form')!

        fireEvent.change(input, { target: { value: 'email-invalid' } })
        fireEvent.submit(form)

        // Căutăm cuvântul "invalid" care este unic în zona de eroare
        const errorMsg = await screen.findByText((content, element) => {
            const hasText = content.includes("Email invalid")
            return hasText && element?.tagName.toLowerCase() === 'div'
        })

        expect(errorMsg).toBeInTheDocument()
    })

    it('4. Trece prin fluxul de loading și afișează succesul', async () => {
        renderComponent()
        const input = screen.getByPlaceholderText('adresa@exemplu.com')
        const form = input.closest('form')!

        fireEvent.change(input, { target: { value: 'test@familie.com' } })
        fireEvent.submit(form)

        // Verificăm starea de loading
        expect(await screen.findByText(/Se procesează/i)).toBeInTheDocument()
        expect(input).toBeDisabled()

        // Așteptăm mesajul de succes folosind textul exact — apare o singură dată în DOM
        const successMsg = await screen.findByText(
            'Dacă adresa există în sistem, vei primi un link pentru resetarea parolei.',
            { exact: true },
            { timeout: 3000 }
        )
        expect(successMsg).toBeInTheDocument()
        expect(screen.getByText(/Email trimis!/i)).toBeInTheDocument()
    })

    it('5. Navigarea înapoi la login funcționează', () => {
        renderComponent()
        const link = screen.getByRole('link', { name: /Autentificare/i })
        expect(link).toHaveAttribute('href', '/login')
    })
})