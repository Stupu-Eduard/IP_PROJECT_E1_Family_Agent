import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import BudgetWidget from './BudgetWidget'
import * as budgetService from '../services/budget'

// ─── MOCKS ────────────────────────────────────────────────────────────────────

vi.mock('../services/budget', () => ({
    fetchBudget: vi.fn(),
    saveBudget:  vi.fn(),
}))

// ─── HELPERS ──────────────────────────────────────────────────────────────────

const renderWidget = (expenses = 0, role = 'Parent') =>
    render(<BudgetWidget currentMonthExpenses={expenses} userRole={role} />)

// ─── SUITA DE TESTE ───────────────────────────────────────────────────────────

describe('BudgetWidget Component', () => {

    beforeEach(() => {
        vi.clearAllMocks()
        // shouldAdvanceTime permite microtask-urilor (Promise) să se rezolve
        vi.useFakeTimers({ shouldAdvanceTime: true })
    })

    afterEach(() => {
        vi.runAllTimers()
        vi.useRealTimers()
    })

    // ─── 1. STAREA DE LOADING ─────────────────────────────────────────────────

    it('1. afișează indicatorul de loading în timp ce se preia bugetul', () => {
        // fetchBudget nu se rezolvă niciodată (Promise pendingă) → loading vizibil
        vi.mocked(budgetService.fetchBudget).mockReturnValue(new Promise(() => {}))

        renderWidget()

        expect(screen.getByTestId('budget-loading')).toBeInTheDocument()
        expect(screen.queryByTestId('budget-progress-section')).not.toBeInTheDocument()
        expect(screen.queryByTestId('budget-edit-form')).not.toBeInTheDocument()
    })

    // ─── 2. FĂRĂ BUGET SETAT ─────────────────────────────────────────────────

    it('2a. afișează mesaj „niciun buget" pentru Admin când API-ul întoarce null', async () => {
        vi.mocked(budgetService.fetchBudget).mockRejectedValue(new Error('Not found'))

        renderWidget(0, 'Parent')

        await waitFor(() => expect(screen.queryByTestId('budget-loading')).not.toBeInTheDocument())

        expect(screen.getByTestId('no-budget-msg')).toBeInTheDocument()
        // Butonul „Setează buget" este prezent (Admin vede butonul de editare)
        expect(screen.getByTestId('edit-budget-btn')).toBeInTheDocument()
    })

    it('2b. afișează mesaj „niciun buget" generic pentru Child când bugetul nu există', async () => {
        vi.mocked(budgetService.fetchBudget).mockRejectedValue(new Error('Not found'))

        renderWidget(0, 'Child')

        await waitFor(() => expect(screen.queryByTestId('budget-loading')).not.toBeInTheDocument())

        expect(screen.getByTestId('no-budget-msg')).toBeInTheDocument()
        expect(screen.getByText(/Niciun buget definit/i)).toBeInTheDocument()
        // Copilul nu vede butonul de editare
        expect(screen.queryByTestId('edit-budget-btn')).not.toBeInTheDocument()
    })

    // ─── 3. BARA DE PROGRES – STĂRI VIZUALE ──────────────────────────────────

    it('3a. afișează bara de progres verde la <75% consum', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(500, 'Parent') // 50%

        await waitFor(() => expect(screen.getByTestId('budget-progress-section')).toBeInTheDocument())

        const fill = screen.getByTestId('budget-progress-bar-fill')
        expect(fill).toHaveStyle({ background: 'var(--color-primary)', width: '50%' })
        expect(screen.getByTestId('budget-status-label')).toHaveTextContent('În limitele bugetului')
        expect(screen.getByTestId('budget-percent')).toHaveTextContent('50%')

        // Nicio alertă
        expect(screen.queryByTestId('budget-danger-alert')).not.toBeInTheDocument()
        expect(screen.queryByTestId('budget-warning-alert')).not.toBeInTheDocument()
    })

    it('3b. afișează bara galbenă și alertă warning între 75% și 90%', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(800, 'Parent') // 80%

        await waitFor(() => expect(screen.getByTestId('budget-progress-section')).toBeInTheDocument())

        const fill = screen.getByTestId('budget-progress-bar-fill')
        expect(fill).toHaveStyle({ background: '#F59E0B' })
        expect(screen.getByTestId('budget-status-label')).toHaveTextContent('Aproape de limită')
        expect(screen.getByTestId('budget-warning-alert')).toBeInTheDocument()
        expect(screen.queryByTestId('budget-danger-alert')).not.toBeInTheDocument()
    })

    it('3c. afișează bara roșie și alertă danger la ≥90% consum', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(950, 'Parent') // 95%

        await waitFor(() => expect(screen.getByTestId('budget-progress-section')).toBeInTheDocument())

        const fill = screen.getByTestId('budget-progress-bar-fill')
        expect(fill).toHaveStyle({ background: '#E24B4A' })
        expect(screen.getByTestId('budget-status-label')).toHaveTextContent('Depășit limita!')
        expect(screen.getByTestId('budget-danger-alert')).toBeInTheDocument()
        expect(screen.queryByTestId('budget-warning-alert')).not.toBeInTheDocument()
    })

    it('3d. limitează bara de progres la 100% chiar dacă cheltuielile depășesc bugetul', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(2000, 'Parent') // 200% → trebuie clamped la 100%

        await waitFor(() => expect(screen.getByTestId('budget-progress-section')).toBeInTheDocument())

        expect(screen.getByTestId('budget-progress-bar-fill')).toHaveStyle({ width: '100%' })
        expect(screen.getByTestId('budget-percent')).toHaveTextContent('100%')
    })

    it('3e. afișează corect valorile numerice RON (cheltuieli și limită)', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 3000 })

        renderWidget(1248, 'Parent')

        await waitFor(() => expect(screen.getByTestId('budget-progress-section')).toBeInTheDocument())

        // Verificăm că suma cheltuielilor și limita sunt prezente în DOM
        expect(screen.getByText(/1\.248,00/)).toBeInTheDocument()
        expect(screen.getByText(/3\.000,00/)).toBeInTheDocument()
    })

    // ─── 4. RBAC – CONTROL ACCES ──────────────────────────────────────────────

    it('4a. utilizatorul Child NU vede butonul de editare a bugetului', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(300, 'Child')

        await waitFor(() => expect(screen.getByTestId('budget-progress-section')).toBeInTheDocument())

        expect(screen.queryByTestId('edit-budget-btn')).not.toBeInTheDocument()
    })

    it('4b. utilizatorul Co-Parent VEDE butonul de editare a bugetului', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 2000 })

        renderWidget(400, 'Co-Parent')

        await waitFor(() => expect(screen.getByTestId('edit-budget-btn')).toBeInTheDocument())
    })

    it('4c. utilizatorul Parent VEDE butonul de editare a bugetului', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 2000 })

        renderWidget(400, 'Parent')

        await waitFor(() => expect(screen.getByTestId('edit-budget-btn')).toBeInTheDocument())
    })

    // ─── 5. FORMULAR EDITARE BUGET ────────────────────────────────────────────

    it('5a. click pe „Modifică" deschide formularul de editare', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1500 })

        renderWidget(0, 'Parent')

        await waitFor(() => expect(screen.getByTestId('edit-budget-btn')).toBeInTheDocument())

        fireEvent.click(screen.getByTestId('edit-budget-btn'))

        expect(screen.getByTestId('budget-edit-form')).toBeInTheDocument()
        expect(screen.queryByTestId('budget-progress-section')).not.toBeInTheDocument()
    })

    it('5b. click pe „Setează buget" deschide formularul când nu există buget', async () => {
        vi.mocked(budgetService.fetchBudget).mockRejectedValue(new Error('Not found'))

        renderWidget(0, 'Parent')

        await waitFor(() => expect(screen.getByTestId('no-budget-msg')).toBeInTheDocument())

        fireEvent.click(screen.getByTestId('edit-budget-btn'))

        expect(screen.getByTestId('budget-edit-form')).toBeInTheDocument()
    })

    it('5c. „Anulează" închide formularul și restaurează valoarea anterioară', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1500 })

        renderWidget(0, 'Parent')

        await waitFor(() => fireEvent.click(screen.getByTestId('edit-budget-btn')))

        const input = screen.getByTestId('budget-input') as HTMLInputElement
        fireEvent.change(input, { target: { value: '9999' } })
        expect(input.value).toBe('9999')

        fireEvent.click(screen.getByTestId('budget-cancel-btn'))

        // Formularul dispare și ne întoarcem la progress bar
        expect(screen.queryByTestId('budget-edit-form')).not.toBeInTheDocument()
        expect(screen.getByTestId('budget-progress-section')).toBeInTheDocument()
    })

    it('5d. „Anulează" din modul „fără buget" curăță erorile și închide formularul', async () => {
        vi.mocked(budgetService.fetchBudget).mockRejectedValue(new Error('Not found'))

        renderWidget(0, 'Parent')

        await waitFor(() => fireEvent.click(screen.getByTestId('edit-budget-btn')))

        fireEvent.click(screen.getByTestId('budget-cancel-btn'))

        // Formularul este închis
        expect(screen.queryByTestId('budget-edit-form')).not.toBeInTheDocument()
    })

    it('5e. schimbarea inputului resetează erorile de validare', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(0, 'Parent')

        await waitFor(() => fireEvent.click(screen.getByTestId('edit-budget-btn')))

        // Introducem o valoare invalidă și salvăm pentru a genera eroarea
        const input = screen.getByTestId('budget-input')
        fireEvent.change(input, { target: { value: '-5' } })
        fireEvent.click(screen.getByTestId('budget-save-btn'))

        expect(screen.getByTestId('budget-validation-error')).toBeInTheDocument()

        // Schimbăm inputul → eroarea dispare
        fireEvent.change(input, { target: { value: '500' } })
        expect(screen.queryByTestId('budget-validation-error')).not.toBeInTheDocument()
    })

    // ─── 6. VALIDARE INPUT ────────────────────────────────────────────────────

    it('6a. afișează eroare de validare pentru valoare negativă', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(0, 'Parent')

        await waitFor(() => fireEvent.click(screen.getByTestId('edit-budget-btn')))

        fireEvent.change(screen.getByTestId('budget-input'), { target: { value: '-100' } })
        fireEvent.click(screen.getByTestId('budget-save-btn'))

        expect(screen.getByTestId('budget-validation-error')).toHaveTextContent(
            'Bugetul trebuie să fie un număr mai mare decât 0.'
        )
        expect(budgetService.saveBudget).not.toHaveBeenCalled()
    })

    it('6b. afișează eroare de validare pentru valoare zero', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(0, 'Parent')

        await waitFor(() => fireEvent.click(screen.getByTestId('edit-budget-btn')))

        fireEvent.change(screen.getByTestId('budget-input'), { target: { value: '0' } })
        fireEvent.click(screen.getByTestId('budget-save-btn'))

        expect(screen.getByTestId('budget-validation-error')).toBeInTheDocument()
        expect(budgetService.saveBudget).not.toHaveBeenCalled()
    })

    it('6c. afișează eroare de validare pentru câmp gol (NaN)', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(0, 'Parent')

        await waitFor(() => fireEvent.click(screen.getByTestId('edit-budget-btn')))

        fireEvent.change(screen.getByTestId('budget-input'), { target: { value: '' } })
        fireEvent.click(screen.getByTestId('budget-save-btn'))

        expect(screen.getByTestId('budget-validation-error')).toBeInTheDocument()
        expect(budgetService.saveBudget).not.toHaveBeenCalled()
    })

    // ─── 7. SALVARE CU SUCCES ─────────────────────────────────────────────────

    it('7a. salvarea cu succes actualizează bugetul și afișează confirmarea', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })
        vi.mocked(budgetService.saveBudget).mockResolvedValue({ monthlyLimit: 3000 })

        renderWidget(0, 'Parent')

        await waitFor(() => fireEvent.click(screen.getByTestId('edit-budget-btn')))

        fireEvent.change(screen.getByTestId('budget-input'), { target: { value: '3000' } })

        await act(async () => {
            fireEvent.click(screen.getByTestId('budget-save-btn'))
        })

        await waitFor(() => expect(screen.getByTestId('budget-success-msg')).toBeInTheDocument())

        expect(budgetService.saveBudget).toHaveBeenCalledWith(3000)
        // Formularul se închide după salvare reușită
        expect(screen.queryByTestId('budget-edit-form')).not.toBeInTheDocument()
        expect(screen.getByTestId('budget-progress-section')).toBeInTheDocument()
    })

    it('7b. mesajul de succes dispare automat după 3 secunde', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })
        vi.mocked(budgetService.saveBudget).mockResolvedValue({ monthlyLimit: 2500 })

        renderWidget(0, 'Parent')

        await waitFor(() => fireEvent.click(screen.getByTestId('edit-budget-btn')))

        fireEvent.change(screen.getByTestId('budget-input'), { target: { value: '2500' } })

        await act(async () => {
            fireEvent.click(screen.getByTestId('budget-save-btn'))
        })

        await waitFor(() => expect(screen.getByTestId('budget-success-msg')).toBeInTheDocument())

        act(() => { vi.advanceTimersByTime(3000) })

        await waitFor(() =>
            expect(screen.queryByTestId('budget-success-msg')).not.toBeInTheDocument()
        )
    })

    it('7c. butonul „Salvează" afișează „Se salvează..." în timp ce request-ul este în curs', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        let resolve!: (val: { monthlyLimit: number }) => void
        vi.mocked(budgetService.saveBudget).mockReturnValue(
            new Promise((res) => { resolve = res })
        )

        renderWidget(0, 'Parent')

        await waitFor(() => fireEvent.click(screen.getByTestId('edit-budget-btn')))

        fireEvent.change(screen.getByTestId('budget-input'), { target: { value: '2000' } })
        fireEvent.click(screen.getByTestId('budget-save-btn'))

        expect(screen.getByTestId('budget-save-btn')).toHaveTextContent('Se salvează...')
        expect(screen.getByTestId('budget-save-btn')).toBeDisabled()
        expect(screen.getByTestId('budget-cancel-btn')).toBeDisabled()

        // Rezolvăm Promise-ul
        await act(async () => { resolve({ monthlyLimit: 2000 }) })
    })

    // ─── 8. EROARE LA SALVARE (API) ───────────────────────────────────────────

    it('8a. afișează eroarea API dacă saveBudget eșuează', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })
        vi.mocked(budgetService.saveBudget).mockRejectedValue(new Error('Server error'))

        renderWidget(0, 'Parent')

        await waitFor(() => fireEvent.click(screen.getByTestId('edit-budget-btn')))

        fireEvent.change(screen.getByTestId('budget-input'), { target: { value: '2000' } })

        await act(async () => {
            fireEvent.click(screen.getByTestId('budget-save-btn'))
        })

        await waitFor(() => expect(screen.getByTestId('budget-save-error')).toBeInTheDocument())
        expect(screen.getByTestId('budget-save-error')).toHaveTextContent(
            'Eroare la salvare. Încearcă din nou.'
        )
        // Formularul rămâne deschis
        expect(screen.getByTestId('budget-edit-form')).toBeInTheDocument()
    })

    // ─── 9. CLEANUP EFECT (COMPONENT UNMOUNT) ────────────────────────────────

    it('9. nu actualizează starea după demontare (cancelled flag)', async () => {
        let resolve!: (val: { monthlyLimit: number }) => void
        vi.mocked(budgetService.fetchBudget).mockReturnValue(
            new Promise((res) => { resolve = res })
        )

        const { unmount } = renderWidget(0, 'Parent')

        // Demontăm înainte ca fetch-ul să se fi rezolvat
        unmount()

        // Rezolvăm după unmount → nu trebuie să arunce erori de tip "state update on unmounted"
        await act(async () => { resolve({ monthlyLimit: 5000 }) })

        // Testul trece dacă nu apare nicio eroare de React
    })

    // ─── 10. STARE MIXTĂ: buget = 0 (limită invalidă) ────────────────────────

    it('10. tratează corect monthlyLimit = 0 primit de la API (afișează mesaj fără buget)', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 0 })

        renderWidget(0, 'Parent')

        await waitFor(() => expect(screen.queryByTestId('budget-loading')).not.toBeInTheDocument())

        // monthlyLimit = 0 este tratat ca „fără buget setat"
        expect(screen.getByTestId('no-budget-msg')).toBeInTheDocument()
        expect(screen.queryByTestId('budget-progress-section')).not.toBeInTheDocument()
    })

    // ─── 11. EXACT LA PRAGUL DE 75% ──────────────────────────────────────────

    it('11. afișează alertă warning exact la 75% consum (prag incluziv)', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(750, 'Parent') // exact 75%

        await waitFor(() => expect(screen.getByTestId('budget-progress-section')).toBeInTheDocument())

        expect(screen.getByTestId('budget-warning-alert')).toBeInTheDocument()
        expect(screen.queryByTestId('budget-danger-alert')).not.toBeInTheDocument()
    })

    // ─── 12. EXACT LA PRAGUL DE 90% ──────────────────────────────────────────

    it('12. afișează alertă danger exact la 90% consum (prag incluziv)', async () => {
        vi.mocked(budgetService.fetchBudget).mockResolvedValue({ monthlyLimit: 1000 })

        renderWidget(900, 'Parent') // exact 90%

        await waitFor(() => expect(screen.getByTestId('budget-progress-section')).toBeInTheDocument())

        expect(screen.getByTestId('budget-danger-alert')).toBeInTheDocument()
        expect(screen.queryByTestId('budget-warning-alert')).not.toBeInTheDocument()
    })
})