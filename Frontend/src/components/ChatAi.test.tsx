import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react'
import ChatAI from './ChatAi'

vi.mock('../api/api', () => ({
    default: {
        post: vi.fn(),
    },
}))

import api from '../api/api'

describe('ChatAI Component', () => {
    let scrollIntoViewMock: any

    beforeEach(() => {
        scrollIntoViewMock = vi.fn()
        window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock
        vi.mocked(api.post).mockReset()
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    it('1. Randează doar butonul FAB inițial și rulează auto-scroll la deschidere', () => {
        render(<ChatAI />)
        const fabButton = screen.getByRole('button', { name: /deschide asistentul ai/i })
        expect(fabButton).toBeInTheDocument()
        expect(screen.queryByText('FamilyAgent AI')).not.toBeInTheDocument()
        fireEvent.click(fabButton)
        expect(scrollIntoViewMock).toHaveBeenCalled()
    })

    it('2. Închide fereastra de chat la click pe X', () => {
        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))
        const closeBtn = screen.getAllByRole('button')[0]
        fireEvent.click(closeBtn)
        expect(screen.queryByText('FamilyAgent AI')).not.toBeInTheDocument()
    })

    it('3. Trimite un mesaj și primește răspuns de la API', async () => {
        vi.mocked(api.post).mockResolvedValueOnce({
            data: { reply: 'Ai cheltuit 1248 de lei luna aceasta.' },
        })

        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        fireEvent.change(input, { target: { value: 'Cat am cheltuit luna aceasta?' } })

        await act(async () => {
            fireEvent.submit(input)
        })

        expect(screen.getByText('Cat am cheltuit luna aceasta?')).toBeInTheDocument()

        await waitFor(() => {
            expect(screen.getByText('Ai cheltuit 1248 de lei luna aceasta.')).toBeInTheDocument()
        })

        expect(screen.getByPlaceholderText('Întreabă ceva...')).toBeInTheDocument()
    })

    it('4. Nu permite trimiterea de mesaje goale', () => {
        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))
        const input = screen.getByRole('textbox')
        const form = input.closest('form')!
        fireEvent.change(input, { target: { value: '   ' } })
        fireEvent.submit(form)
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
        fireEvent.mouseEnter(sendBtn)
        expect(sendBtn).not.toHaveStyle({ background: 'var(--color-primary)' })
        fireEvent.change(input, { target: { value: 'Test' } })
        fireEvent.mouseEnter(sendBtn)
        expect(sendBtn).toHaveStyle({ background: 'var(--color-primary)' })
        fireEvent.mouseLeave(sendBtn)
        expect(sendBtn).toHaveStyle({ background: 'var(--color-ink)' })
    })

    it('8. Previne trimiterea formularului în timp ce botul scrie (isTyping)', async () => {
        vi.mocked(api.post).mockResolvedValueOnce({
            data: { reply: 'Răspuns de la AI.' },
        })

        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        const form = input.closest('form')!

        fireEvent.change(input, { target: { value: 'Mesaj 1' } })
        fireEvent.submit(form)

        fireEvent.change(input, { target: { value: 'Mesaj 2' } })
        fireEvent.submit(form)

        expect(screen.queryByText('Mesaj 2')).not.toBeInTheDocument()

        await waitFor(() => {
            expect(screen.getByPlaceholderText('Întreabă ceva...')).toBeInTheDocument()
        })
    })

    it('9. Afișează mesaj de eroare când API-ul eșuează', async () => {
        vi.mocked(api.post).mockRejectedValueOnce(new Error('Network Error'))

        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        fireEvent.change(input, { target: { value: 'Test eroare' } })

        await act(async () => {
            fireEvent.submit(input)
        })

        await waitFor(() => {
            expect(screen.getByText('Eroare la conectarea cu asistentul. Încearcă din nou.')).toBeInTheDocument()
        })
    })
})