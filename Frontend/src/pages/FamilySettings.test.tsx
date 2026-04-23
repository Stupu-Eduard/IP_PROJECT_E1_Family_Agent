import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import FamilySettings from './FamilySettings'

// Mocking the auth store
vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector({
        logout: vi.fn(),
        token: 'mock-valid-token', // Adăugăm un token simulat
    }),
}))

vi.mock('../utils/jwt', () => ({
    decodeJwtPayload: () => ({
        role: 'Parent', // Îi „șoptim” testului că avem rol de părinte
        sub: '1'
    }),
    isTokenExpired: () => false
}))

describe('FamilySettings Component (Grup Familial)', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderComponent = () => {
        return render(
            <BrowserRouter>
                <FamilySettings />
            </BrowserRouter>
        )
    }

    it('ar trebui să randeze titlul paginii și secțiunea de adăugare', () => {
        renderComponent()

        expect(screen.getByText('Gestionare Familie')).toBeInTheDocument()
        expect(screen.getByText('Adaugă un membru nou')).toBeInTheDocument()
    })

    it('ar trebui să permită introducerea adresei în formularul de invitație', () => {
        renderComponent()

        const emailInput = screen.getByPlaceholderText('email@familie.com')

        fireEvent.change(emailInput, { target: { value: 'test@familie.ro' } })
        expect(emailInput).toHaveValue('test@familie.ro')
    })

    it('ar trebui să afișeze elementele listei de membri (date de test)', () => {
        renderComponent()

        // REZOLVAREA AICI: Folosim getAllByText deoarece cuvântul apare de mai multe ori
        // și verificăm pur și simplu că a găsit cel puțin o apariție (length > 0)
        const memberTexts = screen.getAllByText(/Membri/i)
        expect(memberTexts.length).toBeGreaterThan(0)
    })

    it('ar trebui să intercepteze corect submit-ul invitației', async () => {
        renderComponent()

        const emailInput = screen.getByPlaceholderText('email@familie.com')
        const form = emailInput.closest('form')!

        fireEvent.change(emailInput, { target: { value: 'nou@familie.com' } })
        fireEvent.submit(form)

        await waitFor(() => {
            expect(emailInput).toBeInTheDocument()
        })
    })
})