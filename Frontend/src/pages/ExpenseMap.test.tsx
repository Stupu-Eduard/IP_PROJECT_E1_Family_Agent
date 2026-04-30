import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { MemoryRouter, useLocation } from 'react-router-dom'
import ExpenseMap from './ExpenseMap'
import * as locationsService from '../services/locations'
import * as googleMapsApi from '@react-google-maps/api'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return {
        ...actual,
        useNavigate: () => mockNavigate,
        useLocation: vi.fn(() => ({ state: {} }))
    }
})

vi.mock('../services/locations', () => ({
    updateLocationCoordinates: vi.fn()
}))

vi.mock('@react-google-maps/api', () => ({
    useJsApiLoader: vi.fn(() => ({ isLoaded: true, loadError: null })),
    GoogleMap: ({ children }: { children: React.ReactNode }) => <div data-testid="google-map">{children}</div>,
    Marker: ({ onClick }: { onClick?: () => void }) => <div data-testid="map-marker" onClick={onClick} />,
    InfoWindow: ({ children, onCloseClick }: { children: React.ReactNode; onCloseClick?: () => void }) => (
        <div data-testid="info-window">
            <button data-testid="close-info" onClick={onCloseClick}>X</button>
            {children}
        </div>
    )
}))

let activeWsInstance: MockWebSocket | null = null
class MockWebSocket {
    onopen: (() => void) | null = null
    onmessage: ((event: MessageEvent) => void) | null = null
    close = vi.fn()
    constructor() {
        // eslint-disable-next-line @typescript-eslint/no-this-alias
        activeWsInstance = this
        setTimeout(() => this.onopen?.(), 10)
    }
}

globalThis.WebSocket = MockWebSocket as unknown as typeof WebSocket

const fetchMock = vi.fn()
globalThis.fetch = fetchMock as unknown as typeof fetch

describe('ExpenseMap Component', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        activeWsInstance = null

        vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', 'fake-google-api-key')
        vi.stubEnv('VITE_GOOGLE_GEOCODING_API_KEY', 'fake-geocoding-api-key')
    })

    afterEach(() => {
        vi.unstubAllEnvs()
    })

    const renderComponent = (stateData: Record<string, unknown> = {}) => {
        vi.mocked(useLocation).mockReturnValue({ state: stateData } as unknown as ReturnType<typeof useLocation>)
        return render(<MemoryRouter><ExpenseMap /></MemoryRouter>)
    }

    it('1. Arată mesaj de eroare dacă lipsește cheia API', () => {
        vi.unstubAllEnvs()
        renderComponent({ locationLabel: 'București' })
        expect(screen.getByText(/Lipsește cheia Google Maps/i)).toBeInTheDocument()
    })

    it('2. Tratează cazurile fără locație validă ("Online")', () => {
        renderComponent({ locationLabel: 'Online' })
        expect(screen.getByText(/Locația este non-geografică/i)).toBeInTheDocument()
    })

    it('3. Randează folosind coordonatele salvate și validează Geofencing-ul (Safe Zone)', async () => {
        const safeCoords = { lat: 44.4250, lng: 26.1000, locationLabel: 'Universitate' }
        renderComponent(safeCoords)

        await waitFor(() => {
            expect(screen.getByText('Folosesc coordonatele salvate.')).toBeInTheDocument()
        })
        expect(screen.getByTestId('google-map')).toBeInTheDocument()
        expect(screen.getByTestId('map-marker')).toBeInTheDocument()

        expect(screen.queryByText(/ALERTĂ DE SECURITATE/i)).not.toBeInTheDocument()
    })

    it('4. Arată alertă de Geofencing dacă coordonatele din DB sunt în afara zonei', async () => {
        const unsafeCoords = { lat: 46.7700, lng: 23.5900, locationLabel: 'Cluj' }
        renderComponent(unsafeCoords)

        await waitFor(() => {
            expect(screen.getByText(/ALERTĂ DE SECURITATE/i)).toBeInTheDocument()
        })
    })

    it('5. Face geocoding automat dacă lipsesc coordonatele și salvează în BD', async () => {
        fetchMock.mockResolvedValueOnce({
            json: async () => ({
                results: [{
                    geometry: { location: { lat: 44.4268, lng: 26.1025 } },
                    formatted_address: 'București, România',
                    place_id: 'place_123'
                }]
            })
        } as Response)

        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ places: [{ id: 'place_123', googleMapsUri: 'https://maps...' }] })
        } as Response)

        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ displayName: { text: 'Magazin Test' }, formattedAddress: 'Adresa Test' })
        } as Response)

        renderComponent({ locationLabel: 'Mega Image', locationId: 1 })

        await waitFor(() => {
            expect(locationsService.updateLocationCoordinates).toHaveBeenCalledWith(1, 44.4268, 26.1025)
            expect(screen.getByText('Adresa Test')).toBeInTheDocument()
        })
    })

    it('6. Deschide și închide InfoWindow-ul la click pe marker', async () => {
        renderComponent({ lat: 44.4250, lng: 26.1000, locationLabel: 'KFC' })

        const marker = await screen.findByTestId('map-marker')
        fireEvent.click(marker)

        expect(screen.getByTestId('info-window')).toBeInTheDocument()
        expect(screen.getAllByText('KFC').length).toBeGreaterThan(0)

        fireEvent.click(screen.getByTestId('close-info'))
        expect(screen.queryByTestId('info-window')).not.toBeInTheDocument()
    })

    it('7. Reacționează la datele live primite prin WebSocket', async () => {
        renderComponent({ lat: 44.4250, lng: 26.1000, locationLabel: 'Acasa' })

        expect(screen.queryByText(/ALERTĂ DE SECURITATE/i)).not.toBeInTheDocument()

        act(() => {
            activeWsInstance?.onmessage?.({
                data: JSON.stringify({ lat: 46.0000, lng: 24.0000, isRestricted: true })
            } as MessageEvent)
        })

        await waitFor(() => {
            expect(screen.getByText(/ALERTĂ DE SECURITATE/i)).toBeInTheDocument()
        })
    })

    it('8. Navighează înapoi la click pe butonul de back', () => {
        renderComponent()
        fireEvent.click(screen.getByRole('button', { name: /înapoi/i }))
        expect(mockNavigate).toHaveBeenCalledWith(-1)
    })

    it('9. Arată eroare dacă lipsește cheia de Geocoding API', async () => {
        vi.stubEnv('VITE_GOOGLE_GEOCODING_API_KEY', '')
        renderComponent({ locationLabel: 'Paris' })
        expect(await screen.findByText(/Lipsește cheia pentru Geocoding API/i)).toBeInTheDocument()
    })

    it('10. Arată eroare dacă Geocoding API returnează eroare', async () => {
        fetchMock.mockResolvedValueOnce({
            json: async () => ({ status: 'ZERO_RESULTS', error_message: 'No results found' })
        } as Response)
        renderComponent({ locationLabel: 'AdresaInexistenta123' })

        expect(await screen.findByText(/ZERO_RESULTS - No results found/i)).toBeInTheDocument()
    })

    it('11. Prinde eroarea dacă salvarea coordonatelor în BD eșuează', async () => {
        fetchMock.mockResolvedValueOnce({
            json: async () => ({
                results: [{ geometry: { location: { lat: 44.0, lng: 26.0 } }, formatted_address: 'Test BD' }]
            })
        } as Response)

        fetchMock.mockResolvedValueOnce({ ok: false } as Response)

        vi.mocked(locationsService.updateLocationCoordinates).mockRejectedValueOnce(new Error('DB Sync Error'))

        renderComponent({ locationLabel: 'KFC', locationId: 99 })

        expect(await screen.findByText(/Am găsit coordonatele, dar nu le-am putut salva în baza de date/i)).toBeInTheDocument()
    })

    it('12. Afișează descrierea și aplică fallback pentru Place ID din Geocoding', async () => {
        fetchMock.mockResolvedValueOnce({
            json: async () => ({
                results: [{ geometry: { location: { lat: 44.0, lng: 26.0 } }, formatted_address: 'Bucuresti', place_id: 'geo_place_123' }]
            })
        } as Response)

        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ places: [] })
        } as Response)

        renderComponent({ locationLabel: 'Test Fallback', description: 'O descriere custom din state' })

        expect(await screen.findByText('O descriere custom din state')).toBeInTheDocument()

        const mapLink = await screen.findByRole('link', { name: /Deschide în Google Maps/i })
        expect(mapLink).toHaveAttribute('href', expect.stringContaining('place_id:geo_place_123'))
    })

    it('13. Arată eroare dacă harta eșuează la încărcare (loadError)', () => {
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValueOnce({ isLoaded: false, loadError: new Error('Failed') } as any)
        renderComponent({ locationLabel: 'Bucuresti' })
        expect(screen.getByText(/Nu s-a putut încărca Google Maps/i)).toBeInTheDocument()
    })

    it('14. Arată starea de încărcare dacă harta nu este încă loaded', () => {
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValueOnce({ isLoaded: false, loadError: undefined } as any)
        renderComponent({ locationLabel: 'Bucuresti' })
        expect(screen.getByText(/Se încarcă harta/i)).toBeInTheDocument()
    })
})