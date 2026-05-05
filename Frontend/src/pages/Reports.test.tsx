import '@testing-library/jest-dom'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import Reports from './Reports'
import * as summaryService from '../services/summary'

vi.mock('../services/summary', () => ({
  fetchCategorySummary: vi.fn(),
  fetchMemberSummary: vi.fn(),
}))

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  PieChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  BarChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  CartesianGrid: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
  Legend: () => null,
  Cell: () => null,
  Bar: () => null,
  Pie: ({ data, onClick }: { data?: Array<{ category: string }>; onClick?: (entry: unknown) => void }) => (
    <div>
      {(data ?? []).map((entry) => (
        <button key={entry.category} type="button" onClick={() => onClick?.(entry)}>
          {entry.category}
        </button>
      ))}
    </div>
  ),
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

const categoryResponse = [
  { category: 'Alimentare', totalAmount: 500, percentage: 50 },
  { category: 'Transport', totalAmount: 500, percentage: 50 },
]

const memberResponse = [
  { memberName: 'Eduard', totalAmount: 700, transactionCount: 4 },
  { memberName: 'Mihaela', totalAmount: 300, transactionCount: 2 },
]

const toExpectedIsoFromCustomInput = (value: string) => {
  const [day, month, year] = value.split('/')
  return new Date(`${year}-${month}-${day}T00:00:00`).toISOString().slice(0, 10)
}

describe('Reports', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(summaryService.fetchCategorySummary).mockResolvedValue(categoryResponse)
    vi.mocked(summaryService.fetchMemberSummary).mockResolvedValue(memberResponse)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const renderComponent = () => render(
    <MemoryRouter>
      <Reports />
    </MemoryRouter>,
  )

  it('afișează cele două grafice și totalul perioadei din summary API', async () => {
    renderComponent()

    expect(await screen.findByText(/DISTRIBUȚIE PE CATEGORII/i)).toBeInTheDocument()
    expect(screen.getByText(/COMPARAȚIE PE MEMBRI/i)).toBeInTheDocument()
    expect(screen.getByText('1000.00')).toBeInTheDocument()
  })

  it('click pe o felie setează categoria activă și trimite filtrul către istoric', async () => {
    renderComponent()

    await screen.findByText(/DISTRIBUȚIE PE CATEGORII/i)
    fireEvent.click(screen.getByRole('button', { name: 'Alimentare' }))

    let lastMemberCall: { from?: string; to?: string; category?: string } | undefined
    await waitFor(() => {
      const args = vi.mocked(summaryService.fetchMemberSummary).mock.calls.at(-1)?.[0] as { from?: string; to?: string; category?: string } | undefined
      expect(args?.category).toBe('Alimentare')
      expect(args?.from).toMatch(/^\d{4}-\d{2}-\d{2}$/)
      expect(args?.to).toMatch(/^\d{4}-\d{2}-\d{2}$/)
      lastMemberCall = args
    })

    expect(mockNavigate).toHaveBeenCalledWith('/expenses', {
      state: {
        initialFilters: {
          selectedCategory: 'Alimentare',
          startDate: lastMemberCall?.from,
          endDate: lastMemberCall?.to,
        },
      },
    })
  })

  it('actualizează sincronizat ambele API-uri când se schimbă perioada presetată', async () => {
    renderComponent()
    await screen.findByText(/DISTRIBUȚIE PE CATEGORII/i)

    const initialCategoryCall = vi.mocked(summaryService.fetchCategorySummary).mock.calls.at(-1)?.[0] as { from?: string; to?: string } | undefined

    fireEvent.click(screen.getByRole('button', { name: '3M' }))

    await waitFor(() => {
      const categoryArgs = vi.mocked(summaryService.fetchCategorySummary).mock.calls.at(-1)?.[0] as { from?: string; to?: string } | undefined
      const memberArgs = vi.mocked(summaryService.fetchMemberSummary).mock.calls.at(-1)?.[0] as { from?: string; to?: string; category?: string } | undefined

      expect(categoryArgs?.from).toMatch(/^\d{4}-\d{2}-\d{2}$/)
      expect(categoryArgs?.to).toMatch(/^\d{4}-\d{2}-\d{2}$/)
      expect(categoryArgs?.from).not.toBe(initialCategoryCall?.from)
      expect(memberArgs?.from).toBe(categoryArgs?.from)
      expect(memberArgs?.to).toBe(categoryArgs?.to)
      expect(memberArgs?.category).toBeUndefined()
    })
  })

  it('trimite intervalul custom la API după aplicare', async () => {
    renderComponent()
    await screen.findByText(/DISTRIBUȚIE PE CATEGORII/i)

    fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }))
    const inputs = screen.getAllByPlaceholderText(/ex: [0-9/]+/i)
    fireEvent.change(inputs[0], { target: { value: '01/04/2026' } })
    fireEvent.change(inputs[1], { target: { value: '20/04/2026' } })
    fireEvent.click(screen.getByRole('button', { name: 'Aplică' }))

    const expectedFrom = toExpectedIsoFromCustomInput('01/04/2026')
    const expectedTo = toExpectedIsoFromCustomInput('20/04/2026')

    await waitFor(() => {
      expect(summaryService.fetchCategorySummary).toHaveBeenLastCalledWith(
        { from: expectedFrom, to: expectedTo },
        expect.any(AbortSignal),
      )
    })
  })

  it('afișează mesaj de eroare dacă API-ul de summary eșuează', async () => {
    vi.mocked(summaryService.fetchCategorySummary).mockRejectedValueOnce(new Error('boom'))

    renderComponent()

    expect(await screen.findByText(/Nu am putut încărca sumarul cheltuielilor din API/i)).toBeInTheDocument()
  })
})
