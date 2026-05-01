import '@testing-library/jest-dom'
import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import KidDashboard from './KidDashboard'

// Mock pentru navigare
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return {
        ...actual,
        useNavigate: () => mockNavigate
    }
})

describe('KidDashboard Component - Full Coverage Hunt', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderComponent = () => render(
        <MemoryRouter>
            <KidDashboard />
        </MemoryRouter>
    )

    it('1. Verifică identitatea vizuală și elementele de branding', () => {
        renderComponent()
        // Verificăm textul de sesiune și salutul
        expect(screen.getByText(/SESIUNE COPIL · ANDREI/i)).toBeInTheDocument()
        const greeting = screen.getByRole('heading', { level: 1 })
        expect(greeting).toHaveTextContent(/Salut, Andrei/i)

        // Verificăm prezența avatarului și a etichetei de cont
        expect(screen.getByText('Andrei P.')).toBeInTheDocument()
        expect(screen.getByText(/Cont copil/i)).toBeInTheDocument()
    })

    it('2. Validează calculele financiare și formatarea prețurilor', () => {
        renderComponent()
        // Balance calculat: 100 - 55 = 45
        expect(screen.getByText('45')).toBeInTheDocument()
        expect(screen.getByText(/din 100 RON alocați/i)).toBeInTheDocument()

        // Verificăm cheltuiala specifică formatată[cite: 2]
        expect(screen.getByText(/55 RON/i)).toBeInTheDocument()
        expect(screen.getByText('55% din buget')).toBeInTheDocument()

        // Verificăm formatarea zecimală a cheltuielilor din listă (8.50, 24.00, 12.50)[cite: 2]
        expect(screen.getByText('8.50')).toBeInTheDocument()
        expect(screen.getByText('24.00')).toBeInTheDocument()
        expect(screen.getByText('12.50')).toBeInTheDocument()
    })

    it('3. Testează interacțiunea cu toate Acțiunile Rapide', () => {
        renderComponent()

        // 1. Scanează un bon[cite: 2]
        fireEvent.click(screen.getByText('Scanează un bon'))
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense')

        // 2. Cheltuielile mele[cite: 2]
        fireEvent.click(screen.getByText('Cheltuielile mele'))
        expect(mockNavigate).toHaveBeenCalledWith('/expenses')

        // 3. Familia mea[cite: 2]
        fireEvent.click(screen.getByText('Familia mea'))
        expect(mockNavigate).toHaveBeenCalledWith('/family')
    })

    it('4. Verifică lista de cumpărături și click-ul pe rânduri', () => {
        renderComponent()

        // Verificăm dacă rândul cu "Gustare la chioșc" este prezent și navighează[cite: 2]
        const expenseRow = screen.getByText('Gustare la chioșc').closest('.row-clickable')
        if (expenseRow) fireEvent.click(expenseRow)

        expect(mockNavigate).toHaveBeenCalledWith('/expenses')
    })

    it('5. Analizează secțiunea de Obiectiv și progresul acestuia', () => {
        renderComponent()

        // Verificăm datele obiectivului "Căști noi"[cite: 2]
        const goalCard = screen.getByText(/OBIECTIVUL MEU/i).closest('.card')
        expect(goalCard).toHaveTextContent('Căști noi')
        expect(goalCard).toHaveTextContent('180') // saved[cite: 2]
        expect(goalCard).toHaveTextContent('/ 240 RON') // target[cite: 2]
        expect(goalCard).toHaveTextContent('2 luni rămase') // monthsLeft[cite: 2]

        // Calculul procentajului obiectivului (180/240 = 75%)[cite: 2]
        expect(screen.getByText('75% adunat')).toBeInTheDocument()
    })

    it('6. Testează butoanele secundare de navigare (Header și "Vezi tot")', () => {
        renderComponent()

        // Butonul de scanare din header[cite: 2]
        const headerScanBtn = screen.getByRole('button', { name: /Scanează bonul/i })
        fireEvent.click(headerScanBtn)
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense')

        // Butonul "Vezi tot →" din cardul de cheltuieli[cite: 2]
        const seeAllBtn = screen.getByText(/Vezi tot →/i)
        fireEvent.click(seeAllBtn)
        expect(mockNavigate).toHaveBeenCalledWith('/expenses')
    })
})