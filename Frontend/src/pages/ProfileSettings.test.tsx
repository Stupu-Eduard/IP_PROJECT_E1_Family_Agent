import '@testing-library/jest-dom'
import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import ProfileSettings from './ProfileSettings'
import { useAuthStore } from '../store/authStore'

// ─── MOCKS ────────────────────────────────────────────────────────────

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../store/authStore', () => ({
    useAuthStore: vi.fn(),
}))

vi.mock('../services/api', () => ({
    profileApi: {
        updatePredefinedAvatar: vi.fn(),
        uploadAvatarFile: vi.fn(),
        deleteAccount: vi.fn(),
    },
}))

// ── Helper: token minimal ──────────────────────────────────────────────
const makeToken = (payload: Record<string, unknown>) =>
    `header.${btoa(JSON.stringify(payload))}.signature`

describe('ProfileSettings Component', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderWithRole = (role: string, avatarUrl: string | null = null) => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => {
            const state = {
                token: makeToken({ role, name: 'Test User' }),
                avatarUrl,
                setAvatar: vi.fn(),
                logout: vi.fn(),
            }
            return selector(state)
        })
        return render(<MemoryRouter><ProfileSettings /></MemoryRouter>)
    }

    // ─── 1. RANDARE GENERALĂ ──────────────────────────────────────────

    it('1. Randează titlul paginii și secțiunea Avatar', () => {
        renderWithRole('Parent')

        expect(screen.getByText('Setări Profil')).toBeInTheDocument()
        expect(screen.getByText('Avatar')).toBeInTheDocument()
    })

    it('2. Afișează cele 10 avataruri predefinite', () => {
        renderWithRole('Parent')

        // Fiecare avatar predefinit are un title cu label-ul său
        expect(screen.getByTitle('Feminin Roz')).toBeInTheDocument()
        expect(screen.getByTitle('Masculin Roz')).toBeInTheDocument()
        expect(screen.getByTitle('Feminin Mov')).toBeInTheDocument()
        expect(screen.getAllByRole('button').length).toBeGreaterThanOrEqual(10)
    })

    it('3. Secțiunea "Ștergere cont" este vizibilă pentru Parent', () => {
        renderWithRole('Parent')

        expect(screen.getByText('Ștergere cont')).toBeInTheDocument()
        expect(screen.getByText('Șterge contul meu')).toBeInTheDocument()
    })

    it('4. Secțiunea "Ștergere cont" este ASCUNSĂ pentru Child', () => {
        renderWithRole('Child')

        expect(screen.queryByText('Ștergere cont')).not.toBeInTheDocument()
        expect(screen.queryByText('Șterge contul meu')).not.toBeInTheDocument()
    })

    it('5. Secțiunea "Ștergere cont" este vizibilă pentru Co-Parent', () => {
        renderWithRole('Co-Parent')

        expect(screen.getByText('Ștergere cont')).toBeInTheDocument()
    })

    // ─── 2. MODAL ȘTERGERE CONT ──────────────────────────────────────

    it('6. Click pe "Șterge contul meu" deschide modalul de confirmare', () => {
        renderWithRole('Parent')

        fireEvent.click(screen.getByText('Șterge contul meu'))

        expect(screen.getByText(/Tastează/)).toBeInTheDocument()
        expect(screen.getByPlaceholderText('STERGE CONT')).toBeInTheDocument()
    })

    it('7. Butonul de confirmare din modal este dezactivat cât timp textul e greșit', () => {
        renderWithRole('Parent')

        fireEvent.click(screen.getByText('Șterge contul meu'))

        const confirmBtn = screen.getByText('Șterge contul')
        expect(confirmBtn).toBeDisabled()

        const input = screen.getByPlaceholderText('STERGE CONT')
        fireEvent.change(input, { target: { value: 'STERGE' } })
        expect(confirmBtn).toBeDisabled()
    })

    it('8. Butonul de confirmare se activează când se tastează exact "STERGE CONT"', () => {
        renderWithRole('Parent')

        fireEvent.click(screen.getByText('Șterge contul meu'))

        const input = screen.getByPlaceholderText('STERGE CONT')
        fireEvent.change(input, { target: { value: 'STERGE CONT' } })

        const confirmBtn = screen.getByText('Șterge contul')
        expect(confirmBtn).not.toBeDisabled()
    })

    it('9. Click pe "Anulează" din modal închide modalul', () => {
        renderWithRole('Parent')

        fireEvent.click(screen.getByText('Șterge contul meu'))
        expect(screen.getByPlaceholderText('STERGE CONT')).toBeInTheDocument()

        fireEvent.click(screen.getByText('Anulează'))
        expect(screen.queryByPlaceholderText('STERGE CONT')).not.toBeInTheDocument()
    })

    // ─── 3. AVATAR CURENT ────────────────────────────────────────────

    it('10. Afișează imaginea curentă când avatarUrl e un URL remote', () => {
        const remoteUrl = 'https://res.cloudinary.com/demo/avatar.jpg'
        renderWithRole('Parent', remoteUrl)

        const preview = screen.getByAltText('Avatar preview')
        expect(preview).toBeInTheDocument()
        expect(preview).toHaveAttribute('src', remoteUrl)
    })

    it('11. Butonul înapoi navighează spre /dashboard', () => {
        renderWithRole('Parent')

        const backBtn = screen.getByTitle('Înapoi')
        fireEvent.click(backBtn)

        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
    })
})
