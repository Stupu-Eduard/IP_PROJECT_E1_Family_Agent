import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import AgentResponseRenderer, {
    AgentFallback,
    InlineChart,
    InlineMap,
    isTextResponse,
    isChartResponse,
    isMapResponse,
} from './AgentResponseRenderer'
import type {
    AgentResponse,
    ChartResponse,
    MapResponse,
    TextResponse,
} from '../types/AgentResponseDTO'

vi.mock('recharts', async () => {
    const actual = await vi.importActual<typeof import('recharts')>('recharts')
    return {
        ...actual,
        ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
            <div data-testid="recharts-container" style={{ width: 400, height: 160 }}>
                {children}
            </div>
        ),
    }
})


let mapsLoadedState = { isLoaded: true, loadError: null as unknown }

vi.mock('@react-google-maps/api', () => ({
    useJsApiLoader: () => mapsLoadedState,
    GoogleMap: ({ children }: { children: React.ReactNode }) => (
        <div data-testid="google-map">{children}</div>
    ),
    Marker: ({ onClick }: { onClick: () => void }) => (
        <button data-testid="map-marker" onClick={onClick}>marker</button>
    ),
    InfoWindow: ({ children, onCloseClick }: { children: React.ReactNode; onCloseClick: () => void }) => (
        <div data-testid="info-window">
            <button data-testid="close-info" onClick={onCloseClick}>X</button>
            {children}
        </div>
    ),
}))

const setMapsApiKey = (key: string | undefined) => {
    if (key === undefined) {
        vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', '')
    } else {
        vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', key)
    }
}

describe('AgentResponseRenderer — Type Guards (100% coverage)', () => {

    describe('isTextResponse', () => {
        it('1. Returnează true pentru un TextResponse valid', () => {
            expect(isTextResponse({ type: 'text', text: 'hello' })).toBe(true)
        })
        it('2. Returnează false pentru null', () => {
            expect(isTextResponse(null)).toBe(false)
        })
        it('3. Returnează false pentru un obiect cu type greșit', () => {
            expect(isTextResponse({ type: 'chart', text: 'x' })).toBe(false)
        })
        it('4. Returnează false dacă lipsește câmpul text', () => {
            expect(isTextResponse({ type: 'text' })).toBe(false)
        })
        it('5. Returnează false dacă text nu e string', () => {
            expect(isTextResponse({ type: 'text', text: 123 })).toBe(false)
        })
        it('6. Returnează false pentru primitive', () => {
            expect(isTextResponse('hello')).toBe(false)
            expect(isTextResponse(undefined)).toBe(false)
        })
    })

    describe('isChartResponse', () => {
        it('7. Returnează true pentru ChartResponse valid', () => {
            expect(isChartResponse({
                type: 'chart',
                chartType: 'bar',
                data: [{ name: 'a', value: 1 }],
            })).toBe(true)
        })
        it('8. Returnează false pentru null', () => {
            expect(isChartResponse(null)).toBe(false)
        })
        it('9. Returnează false pentru type greșit', () => {
            expect(isChartResponse({ type: 'text', chartType: 'bar', data: [{ name: 'a', value: 1 }] })).toBe(false)
        })
        it('10. Returnează false dacă data nu e array', () => {
            expect(isChartResponse({ type: 'chart', chartType: 'bar', data: 'not-array' })).toBe(false)
        })
        it('11. Returnează false dacă data e gol', () => {
            expect(isChartResponse({ type: 'chart', chartType: 'bar', data: [] })).toBe(false)
        })
        it('12. Returnează false dacă chartType lipsește sau nu e string', () => {
            expect(isChartResponse({ type: 'chart', data: [{ name: 'a', value: 1 }] })).toBe(false)
            expect(isChartResponse({ type: 'chart', chartType: 42, data: [{ name: 'a', value: 1 }] })).toBe(false)
        })
        it('13. Returnează false pentru chartType nesuportat', () => {
            expect(isChartResponse({ type: 'chart', chartType: 'donut', data: [{ name: 'a', value: 1 }] })).toBe(false)
        })
    })

    describe('isMapResponse', () => {
        it('14. Returnează true pentru MapResponse valid', () => {
            expect(isMapResponse({ type: 'map', pins: [{ lat: 1, lng: 2 }] })).toBe(true)
        })
        it('15. Returnează false pentru null', () => {
            expect(isMapResponse(null)).toBe(false)
        })
        it('16. Returnează false pentru type greșit', () => {
            expect(isMapResponse({ type: 'text', pins: [{ lat: 1, lng: 2 }] })).toBe(false)
        })
        it('17. Returnează false dacă pins nu e array', () => {
            expect(isMapResponse({ type: 'map', pins: null })).toBe(false)
        })
        it('18. Returnează false dacă pins e gol', () => {
            expect(isMapResponse({ type: 'map', pins: [] })).toBe(false)
        })
    })
})

describe('AgentFallback Component', () => {
    it('19. Afișează mesajul default când nu primește prop', () => {
        render(<AgentFallback />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
        expect(screen.getByText(/Nu am putut afișa răspunsul vizual/i)).toBeInTheDocument()
    })

    it('20. Afișează mesajul custom când e furnizat', () => {
        render(<AgentFallback message="Mesaj specific" />)
        expect(screen.getByText('Mesaj specific')).toBeInTheDocument()
    })
})

describe('InlineChart Component', () => {

    it('21. Randează un chart LINE cu titlu', () => {
        const r: ChartResponse = {
            type: 'chart',
            chartType: 'line',
            title: 'Cheltuieli pe zi',
            data: [{ name: 'Lun', value: 100 }, { name: 'Mar', value: 200 }],
        }
        render(<InlineChart response={r} />)
        expect(screen.getByTestId('inline-chart')).toBeInTheDocument()
        expect(screen.getByText('Cheltuieli pe zi')).toBeInTheDocument()
        expect(screen.getByTestId('recharts-container')).toBeInTheDocument()
    })

    it('22. Randează un chart BAR fără titlu (nu apare bara de titlu)', () => {
        const r: ChartResponse = {
            type: 'chart',
            chartType: 'bar',
            data: [{ name: 'Mâncare', value: 320 }, { name: 'Transport', value: 90 }],
        }
        render(<InlineChart response={r} />)
        expect(screen.getByTestId('inline-chart')).toBeInTheDocument()
        // Fără titlu, header-ul de titlu nu apare
        expect(screen.queryByText('Cheltuieli pe zi')).not.toBeInTheDocument()
    })

    it('23. Randează un chart AREA', () => {
        const r: ChartResponse = {
            type: 'chart',
            chartType: 'area',
            title: 'Trend',
            data: [{ name: 'A', value: 1 }, { name: 'B', value: 2 }],
        }
        render(<InlineChart response={r} />)
        expect(screen.getByTestId('inline-chart')).toBeInTheDocument()
        expect(screen.getByText('Trend')).toBeInTheDocument()
    })

    it('24. Randează un chart PIE cu mai multe culori (Cell loop)', () => {
        const r: ChartResponse = {
            type: 'chart',
            chartType: 'pie',
            title: 'Distribuție',
            // 7 elemente forțează wrap-ul (idx % palette.length)
            data: [
                { name: 'A', value: 10 },
                { name: 'B', value: 20 },
                { name: 'C', value: 15 },
                { name: 'D', value: 5 },
                { name: 'E', value: 25 },
                { name: 'F', value: 8 },
                { name: 'G', value: 12 },
            ],
        }
        render(<InlineChart response={r} />)
        expect(screen.getByTestId('inline-chart')).toBeInTheDocument()
        expect(screen.getByText('Distribuție')).toBeInTheDocument()
    })
})

describe('InlineMap Component', () => {

    beforeEach(() => {
        mapsLoadedState = { isLoaded: true, loadError: null }
        setMapsApiKey('FAKE_KEY')
    })

    const mapResponse: MapResponse = {
        type: 'map',
        title: 'Cheltuieli pe locații',
        pins: [
            { lat: 44.43, lng: 26.10, label: 'Kaufland', description: '120 lei' },
            { lat: 44.44, lng: 26.11, label: 'Mega' },
        ],
    }

    it('25. Randează harta cu titlu și markeri când API-ul e încărcat', () => {
        render(<InlineMap response={mapResponse} />)
        expect(screen.getByTestId('inline-map')).toBeInTheDocument()
        expect(screen.getByText('Cheltuieli pe locații')).toBeInTheDocument()
        expect(screen.getByTestId('google-map')).toBeInTheDocument()
        expect(screen.getAllByTestId('map-marker')).toHaveLength(2)
    })

    it('26. Deschide InfoWindow la click pe marker cu label și descriere', () => {
        render(<InlineMap response={mapResponse} />)
        const markers = screen.getAllByTestId('map-marker')
        fireEvent.click(markers[0])
        expect(screen.getByTestId('info-window')).toBeInTheDocument()
        expect(screen.getByText('Kaufland')).toBeInTheDocument()
        expect(screen.getByText('120 lei')).toBeInTheDocument()
    })

    it('27. Închide InfoWindow la click pe X', () => {
        render(<InlineMap response={mapResponse} />)
        fireEvent.click(screen.getAllByTestId('map-marker')[0])
        expect(screen.getByTestId('info-window')).toBeInTheDocument()
        fireEvent.click(screen.getByTestId('close-info'))
        expect(screen.queryByTestId('info-window')).not.toBeInTheDocument()
    })

    it('28. InfoWindow afișează doar label dacă nu există descriere', () => {
        render(<InlineMap response={mapResponse} />)
        // pin-ul 2 (Mega) nu are description
        fireEvent.click(screen.getAllByTestId('map-marker')[1])
        expect(screen.getByText('Mega')).toBeInTheDocument()
        expect(screen.queryByText('120 lei')).not.toBeInTheDocument()
    })

    it('29. InfoWindow nu afișează label dacă lipsește', () => {
        const r: MapResponse = {
            type: 'map',
            pins: [{ lat: 1, lng: 2, description: 'doar desc' }],
        }
        render(<InlineMap response={r} />)
        fireEvent.click(screen.getByTestId('map-marker'))
        expect(screen.getByText('doar desc')).toBeInTheDocument()
    })

    it('30. Fără pini cu label/description, InfoWindow apare gol (acoperă ambele branch-uri falsy)', () => {
        const r: MapResponse = {
            type: 'map',
            pins: [{ lat: 1, lng: 2 }],
        }
        render(<InlineMap response={r} />)
        fireEvent.click(screen.getByTestId('map-marker'))
        expect(screen.getByTestId('info-window')).toBeInTheDocument()
    })

    it('31. Folosește centrul și zoom-ul furnizate dacă există', () => {
        const r: MapResponse = {
            type: 'map',
            pins: [{ lat: 1, lng: 2 }],
            center: { lat: 50, lng: 50 },
            zoom: 8,
        }
        render(<InlineMap response={r} />)
        expect(screen.getByTestId('google-map')).toBeInTheDocument()
    })

    it('32. Randează fără titlu dacă lipsește', () => {
        const r: MapResponse = {
            type: 'map',
            pins: [{ lat: 1, lng: 2, label: 'A' }],
        }
        render(<InlineMap response={r} />)
        expect(screen.getByTestId('inline-map')).toBeInTheDocument()
        expect(screen.queryByText('Cheltuieli pe locații')).not.toBeInTheDocument()
    })

    it('33. Afișează fallback dacă lipsește VITE_GOOGLE_MAPS_API_KEY (string gol)', () => {
        setMapsApiKey(undefined)
        render(<InlineMap response={mapResponse} />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
        expect(screen.getByText(/Lipsește cheia Google Maps/i)).toBeInTheDocument()
    })

    it('33b. Afișează fallback și când env este complet absent (acoperă mapsApiKey ?? "")', () => {
        // Forțăm `undefined` peste import.meta.env pentru a acoperi branch-ul `??`
        const env = import.meta.env as Record<string, string | undefined>
        const originalEnv = env.VITE_GOOGLE_MAPS_API_KEY
        delete env.VITE_GOOGLE_MAPS_API_KEY
        try {
            render(<InlineMap response={mapResponse} />)
            expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
        } finally {
            env.VITE_GOOGLE_MAPS_API_KEY = originalEnv
        }
    })

    it('34. Afișează fallback la eroare de încărcare', () => {
        mapsLoadedState = { isLoaded: false, loadError: new Error('boom') }
        render(<InlineMap response={mapResponse} />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
        expect(screen.getByText(/Nu s-a putut încărca harta/i)).toBeInTheDocument()
    })

    it('35. Afișează state-ul de loading înainte ca API-ul să fie gata', () => {
        mapsLoadedState = { isLoaded: false, loadError: null }
        render(<InlineMap response={mapResponse} />)
        expect(screen.getByTestId('inline-map-loading')).toBeInTheDocument()
        expect(screen.getByText(/Se încarcă harta/i)).toBeInTheDocument()
    })
})

describe('AgentResponseRenderer — Switch / Discriminator', () => {

    beforeEach(() => {
        mapsLoadedState = { isLoaded: true, loadError: null }
        setMapsApiKey('FAKE_KEY')
    })

    it('36. Randează fallback când răspunsul e null', () => {
        render(<AgentResponseRenderer response={null} />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
        expect(screen.getByText(/gol sau lipsește/i)).toBeInTheDocument()
    })

    it('37. Randează fallback când răspunsul e undefined', () => {
        render(<AgentResponseRenderer response={undefined} />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
    })

    it('38. Randează TextResponse corect', () => {
        const r: TextResponse = { type: 'text', text: 'Ai cheltuit 1248 lei.' }
        render(<AgentResponseRenderer response={r} />)
        expect(screen.getByTestId('agent-text')).toHaveTextContent('Ai cheltuit 1248 lei.')
    })

    it('39. Randează fallback pentru text malformat (type=text dar text non-string)', () => {
        // Forțăm payload invalid (din backend pot veni date murdare)
        const malformed = { type: 'text', text: 42 } as unknown as AgentResponse
        render(<AgentResponseRenderer response={malformed} />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
        expect(screen.getByText(/text malformat/i)).toBeInTheDocument()
    })

    it('40. Randează ChartResponse (line) prin componenta InlineChart', () => {
        const r: ChartResponse = {
            type: 'chart',
            chartType: 'line',
            title: 'Evoluție',
            data: [{ name: 'A', value: 1 }, { name: 'B', value: 2 }],
        }
        render(<AgentResponseRenderer response={r} />)
        expect(screen.getByTestId('inline-chart')).toBeInTheDocument()
        expect(screen.getByText('Evoluție')).toBeInTheDocument()
    })

    it('41. Randează fallback pentru ChartResponse cu date lipsă', () => {
        const malformed = { type: 'chart', chartType: 'bar', data: [] } as unknown as AgentResponse
        render(<AgentResponseRenderer response={malformed} />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
        expect(screen.getByText(/Date insuficiente pentru afișarea graficului/i)).toBeInTheDocument()
    })

    it('42. Randează fallback pentru ChartResponse cu chartType invalid', () => {
        const malformed = { type: 'chart', chartType: 'unknown-chart', data: [{ name: 'x', value: 1 }] } as unknown as AgentResponse
        render(<AgentResponseRenderer response={malformed} />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
    })

    it('43. Randează MapResponse prin componenta InlineMap', () => {
        const r: MapResponse = {
            type: 'map',
            pins: [{ lat: 1, lng: 2, label: 'Pin' }],
        }
        render(<AgentResponseRenderer response={r} />)
        expect(screen.getByTestId('inline-map')).toBeInTheDocument()
    })

    it('44. Randează fallback pentru MapResponse fără pin-uri', () => {
        const malformed = { type: 'map', pins: [] } as unknown as AgentResponse
        render(<AgentResponseRenderer response={malformed} />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
        expect(screen.getByText(/Date insuficiente pentru afișarea hărții/i)).toBeInTheDocument()
    })

    it('45. Randează fallback pentru tip de răspuns necunoscut (default branch)', () => {
        const unknown = { type: 'spaceship', payload: 'x' } as unknown as AgentResponse
        render(<AgentResponseRenderer response={unknown} />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
        expect(screen.getByText(/Tip de răspuns necunoscut/i)).toBeInTheDocument()
    })

    it('46. Randează fallback pentru obiect fără câmpul type', () => {
        const noType = {} as unknown as AgentResponse
        render(<AgentResponseRenderer response={noType} />)
        expect(screen.getByTestId('agent-fallback')).toBeInTheDocument()
    })
})