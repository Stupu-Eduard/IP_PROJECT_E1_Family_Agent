import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import ChatAI from './ChatAi'

describe('ChatAI Component', () => {
    let scrollIntoViewMock: any

    beforeEach(() => {
        vi.useFakeTimers()
        // Mock pentru funcția de scroll care se declanșează în useEffect
        scrollIntoViewMock = vi.fn()
        window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock
    })

    afterEach(() => {
        vi.runOnlyPendingTimers()
        vi.useRealTimers()
        vi.restoreAllMocks()
    })

    it('1. Randează doar butonul FAB inițial și rulează auto-scroll la deschidere', () => {
        render(<ChatAI />)
        const fabButton = screen.getByRole('button', { name: /deschide asistentul ai/i })
        expect(fabButton).toBeInTheDocument()
        expect(screen.queryByText('FamilyAgent AI')).not.toBeInTheDocument()

        // Deschidem chat-ul pentru a declanșa useEffect-ul de scroll
        fireEvent.click(fabButton)
        expect(scrollIntoViewMock).toHaveBeenCalled()
    })

    it('2. Închide fereastra de chat la click pe X', () => {
        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        // Căutăm butonul de închidere
        const closeBtn = screen.getAllByRole('button')[0]
        fireEvent.click(closeBtn)

        expect(screen.queryByText('FamilyAgent AI')).not.toBeInTheDocument()
    })

    it('3. Trimite un mesaj și primește răspuns simulat', async () => {
        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        fireEvent.change(input, { target: { value: 'Vreau să adaug o cheltuială' } })
        fireEvent.submit(input)

        // Mesajul utilizatorului a apărut
        expect(screen.getByText('Vreau să adaug o cheltuială')).toBeInTheDocument()
        expect(input).toHaveValue('')
        expect(screen.getByPlaceholderText('Agentul scrie...')).toBeInTheDocument()

        // Avansează timpul pentru răspunsul bot-ului
        act(() => {
            vi.advanceTimersByTime(1500)
        })

        expect(screen.getByText('Analizez datele tale financiare... (Simulare)')).toBeInTheDocument()
        expect(screen.getByPlaceholderText('Întreabă ceva...')).toBeInTheDocument()
    })

    it('4. Nu permite trimiterea de mesaje goale', () => {
        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        const form = input.closest('form')!

        fireEvent.change(input, { target: { value: '   ' } })
        fireEvent.submit(form)

        // Verificăm că nu s-a adăugat mesajul
        expect(screen.queryAllByText('   ')).toHaveLength(0)
    })

    it('5. Tratează evenimentele de hover pe butonul FAB', () => {
        render(<ChatAI />)
        const fab = screen.getByRole('button', { name: /deschide asistentul ai/i })

        fireEvent.mouseEnter(fab)
        expect(fab).toHaveStyle({ transform: 'scale(1.08)', background: 'var(--color-primary)' })

        fireEvent.mouseLeave(fab)
        expect(fab).toHaveStyle({ transform: 'scale(1)', background: 'var(--color-ink)' })
    })

    it('6. Tratează evenimentele de hover pe butonul de închidere (X)', () => {
        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const closeBtn = screen.getAllByRole('button')[0]

        fireEvent.mouseEnter(closeBtn)
        expect(closeBtn).toHaveStyle({ background: 'rgba(255,255,255,0.16)' })

        fireEvent.mouseLeave(closeBtn)
        expect(closeBtn).toHaveStyle({ background: 'rgba(255,255,255,0.08)' })
    })

    it('7. Tratează evenimentele de hover pe butonul Send', () => {
        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        const sendBtn = screen.getAllByRole('button')[1]

        // Când input-ul este gol, hover-ul nu trebuie să schimbe culoarea
        fireEvent.mouseEnter(sendBtn)
        expect(sendBtn).not.toHaveStyle({ background: 'var(--color-primary)' })

        // Când input-ul are text, hover-ul schimbă culoarea
        fireEvent.change(input, { target: { value: 'Test' } })
        fireEvent.mouseEnter(sendBtn)
        expect(sendBtn).toHaveStyle({ background: 'var(--color-primary)' })

        // Mouse leave revine la culoarea inițială
        fireEvent.mouseLeave(sendBtn)
        expect(sendBtn).toHaveStyle({ background: 'var(--color-ink)' })
    })

    it('8. Previne trimiterea formularului în timp ce botul scrie (isTyping)', () => {
        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        const form = input.closest('form')!

        // Trimitem primul mesaj
        fireEvent.change(input, { target: { value: 'Mesaj 1' } })
        fireEvent.submit(form)

        // Încercăm să trimitem al doilea mesaj IMEDIAT, în timp ce isTyping e true
        fireEvent.change(input, { target: { value: 'Mesaj 2' } })
        fireEvent.submit(form)

        // Verificăm că al doilea mesaj nu a fost procesat
        expect(screen.queryByText('Mesaj 2')).not.toBeInTheDocument()

        // Curățăm timpul pentru a nu lăsa testul agățat
        act(() => {
            vi.advanceTimersByTime(1500)
        })
    })
})