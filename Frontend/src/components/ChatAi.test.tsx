import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react'
import ChatAI from './ChatAi'

vi.mock('../api/api', () => ({
    default: {
        post: vi.fn(),
    },
}))

vi.mock('./AgentResponseRenderer', () => ({
    default: ({ response }: { response: { type: string; text?: string } }) => (
        <div data-testid={`agent-renderer-${response.type}`}>
            {response.type === 'text' ? response.text : `[${response.type}]`}
        </div>
    ),
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
            data: { type: 'text', text: 'Ai cheltuit 1248 de lei luna aceasta.' },
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
            data: { type: 'text', text: 'Răspuns de la AI.' },
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

    // ─────────────────────────────────────────────────────────────────────
    // Teste noi pentru US 3.3 — integrare răspunsuri vizuale + adapter
    // ─────────────────────────────────────────────────────────────────────

    it('10. Randează inline un grafic când backend-ul trimite un ChartResponse', async () => {
        vi.mocked(api.post).mockResolvedValueOnce({
            data: {
                type: 'chart',
                payload: {
                    chartType: 'bar',
                    title: 'Cheltuieli pe categorii',
                    data: [{ name: 'Mâncare', value: 320 }, { name: 'Transport', value: 90 }],
                },
            },
        })

        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        fireEvent.change(input, { target: { value: 'Arată-mi cheltuielile pe categorii' } })

        await act(async () => {
            fireEvent.submit(input)
        })

        await waitFor(() => {
            expect(screen.getByTestId('agent-renderer-chart')).toBeInTheDocument()
        })
    })

    it('11. Randează inline o hartă când backend-ul trimite un MapResponse', async () => {
        vi.mocked(api.post).mockResolvedValueOnce({
            data: {
                type: 'map',
                title: 'Locații cheltuieli',
                pins: [{ lat: 44.43, lng: 26.10, label: 'Kaufland' }],
            },
        })

        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        fireEvent.change(input, { target: { value: 'Unde am cheltuit cel mai mult?' } })

        await act(async () => {
            fireEvent.submit(input)
        })

        await waitFor(() => {
            expect(screen.getByTestId('agent-renderer-map')).toBeInTheDocument()
        })
    })

    it('12. Backend trimite payload malformat → AgentResponseRenderer afișează fallback', async () => {
        vi.mocked(api.post).mockResolvedValueOnce({
            data: { type: 'unknown-type', payload: 'something' },
        })

        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        fireEvent.change(input, { target: { value: 'test' } })

        await act(async () => {
            fireEvent.submit(input)
        })

        await waitFor(() => {
            expect(screen.getByTestId('agent-renderer-unknown-type')).toBeInTheDocument()
        })
    })

    it('13. Mesajul utilizatorului e randat ca text simplu (nu prin AgentResponseRenderer)', async () => {
        vi.mocked(api.post).mockResolvedValueOnce({
            data: { type: 'text', text: 'OK.' },
        })

        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        fireEvent.change(input, { target: { value: 'Mesaj user' } })

        await act(async () => {
            fireEvent.submit(input)
        })

        const userBubble = screen.getByText('Mesaj user')
        expect(userBubble).toBeInTheDocument()
        expect(userBubble.closest('[data-testid="agent-renderer-text"]')).toBeNull()
    })

    it('14. Bula de chat cu răspuns vizual are stilul transparent (fără border/background)', async () => {
        vi.mocked(api.post).mockResolvedValueOnce({
            data: {
                type: 'chart',
                payload: {
                    chartType: 'line',
                    data: [{ name: 'A', value: 1 }],
                },
            },
        })

        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox')
        fireEvent.change(input, { target: { value: 'grafic' } })

        await act(async () => {
            fireEvent.submit(input)
        })

        await waitFor(() => {
            const rendererNode = screen.getByTestId('agent-renderer-chart')
            const bubble = rendererNode.parentElement as HTMLElement
            expect(bubble.style.background).toBe('transparent')
            expect(bubble.style.padding).toBe('0px')
        })
    })

    it('15. Butonul Send: handler-ul onMouseEnter respectă condiția input.trim() && !isTyping', async () => {
        let resolveApi: (val: unknown) => void = () => {}
        const pendingPromise = new Promise(resolve => { resolveApi = resolve })
        vi.mocked(api.post).mockReturnValueOnce(pendingPromise as never)

        render(<ChatAI />)
        fireEvent.click(screen.getByRole('button', { name: /deschide asistentul ai/i }))

        const input = screen.getByRole('textbox') as HTMLInputElement
        const sendBtn = screen.getAllByRole('button')[1] as HTMLButtonElement

        const getMouseEnterHandler = () => {
            const propsKey = Object.keys(sendBtn).find(k => k.startsWith('__reactProps$'))
            if (!propsKey) throw new Error('React props not found on element')
            const props = (sendBtn as unknown as Record<string, { onMouseEnter: (e: { currentTarget: HTMLElement }) => void }>)[propsKey]
            return props.onMouseEnter
        }

        sendBtn.style.background = 'var(--color-ink)'
        getMouseEnterHandler()({ currentTarget: sendBtn })
        expect(sendBtn.style.background).toBe('var(--color-ink)')

        fireEvent.change(input, { target: { value: 'mesaj' } })
        getMouseEnterHandler()({ currentTarget: sendBtn })
        expect(sendBtn.style.background).toBe('var(--color-primary)')

        fireEvent.mouseLeave(sendBtn)
        expect(sendBtn.style.background).toBe('var(--color-ink)')

        await act(async () => {
            fireEvent.submit(input)
        })

        fireEvent.change(input, { target: { value: 'alt mesaj' } })
        sendBtn.style.background = 'var(--color-ink)'
        getMouseEnterHandler()({ currentTarget: sendBtn })
        expect(sendBtn.style.background).toBe('var(--color-ink)')

        await act(async () => {
            resolveApi({ data: { type: 'text', text: 'ok' } })
            await pendingPromise
        })
    })
})