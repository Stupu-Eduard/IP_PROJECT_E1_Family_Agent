import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import ChatAI from './ChatAi'

window.HTMLElement.prototype.scrollIntoView = vi.fn()

describe('ChatAI Component (Task 3.1)', () => {
    beforeEach(() => {
        vi.useFakeTimers()
    })

    afterEach(() => {
        act(() => {
            vi.runOnlyPendingTimers()
        })
        vi.useRealTimers()
    })

    it('ar trebui să se deschidă fereastra de chat la click pe butonul FAB', () => {
        render(<ChatAI />)

        // Inițial, fereastra e închisă, vedem doar butonul FAB (are un SVG, dar nu are text, așa că selectăm butonul global)
        const fabButton = screen.getByRole('button')
        fireEvent.click(fabButton)

        // După click, trebuie să apară interfața de chat
        expect(screen.getByText('FamilyAgent AI')).toBeInTheDocument()
        expect(screen.getByText('Salut! Sunt asistentul tău FamilyAgent. Cum te pot ajuta cu bugetul astăzi?')).toBeInTheDocument()
    })

    it('ar trebui să afișeze mesajul utilizatorului și starea de typing după trimitere', () => {
        render(<ChatAI />)

        // Deschidem chat-ul
        fireEvent.click(screen.getByRole('button'))

        // Tastăm un mesaj
        const input = screen.getByPlaceholderText('Întreabă ceva...')
        fireEvent.change(input, { target: { value: 'Vreau un raport' } })

        // Trimitem mesajul
        const form = screen.getByRole('textbox').closest('form')
        if(form) {
            fireEvent.submit(form)
        }

        // Verificăm dacă mesajul utilizatorului a apărut pe ecran
        expect(screen.getByText('Vreau un raport')).toBeInTheDocument()

        // Verificăm dacă inputul este dezactivat și textul s-a schimbat în 'Agentul scrie...'
        expect(screen.getByPlaceholderText('Agentul scrie...')).toBeDisabled()

        act(() => {
            vi.advanceTimersByTime(1500)
        })

        expect(screen.getByText('Analizez datele tale financiare... (Simulare)')).toBeInTheDocument()
    })

    it('nu ar trebui să permită trimiterea unui mesaj gol', () => {
        render(<ChatAI />)

        fireEvent.click(screen.getByRole('button')) // Deschide chat

        const input = screen.getByPlaceholderText('Întreabă ceva...')

        // Asigură-te că inputul e gol
        fireEvent.change(input, { target: { value: '   ' } })

        // Butonul de trimitere ar trebui să fie dezactivat (fiindcă inputul e gol)
        // Căutăm butonul de Send (ultimul buton din formular)
        const sendButton = screen.getAllByRole('button').pop()
        expect(sendButton).toBeDisabled()
    })
})