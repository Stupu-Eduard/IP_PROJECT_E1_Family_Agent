import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import ExpenseMap from './ExpenseMap'
import * as locationsService from '../services/locations'
import * as googleMapsApi from '@react-google-maps/api'

// ── 1. Mock-uri pentru Navigare și Servicii ──
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../services/locations', () => ({
    updateLocationCoordinates: vi.fn()
}))

// ── 2. Mock Google Maps API ──
vi.mock('@react-google-maps/api', () => ({
    useJsApiLoader: vi.fn(() => ({ isLoaded: true, loadError: null })),
    GoogleMap: ({ children }: any) => <div data-testid="google-map">{children}</div>,
    Marker: ({ onClick }: any) => <div data-testid="map-marker" onClick={onClick} />,
    InfoWindow: ({ children, onCloseClick }: any) => (
        <div data-testid="info-window">
            <button data-testid="close-info" onClick={onCloseClick}>X</button>
            {children}
        </div>
    )
}))

// ── 3. Mock WebSocket ──
let activeWsInstance: MockWebSocket | null = null
class MockWebSocket {
    onopen: (() => void) | null = null
    onmessage: ((event: MessageEvent) => void) | null = null
    close = vi.fn()
    constructor() {
        activeWsInstance = this
        setTimeout(() => this.onopen?.(), 0)
    }
}
globalThis.WebSocket = MockWebSocket as unknown as typeof WebSocket
const fetchMock = vi.fn()
globalThis.fetch = fetchMock as unknown as typeof fetch

describe('ExpenseMap - 100% Coverage Final Resolve', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        activeWsInstance = null

        fetchMock.mockResolvedValue({
            ok: true,
            json: async () => ({ results: [], places: [] })
        } as Response)

        vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', 'fake-key')
        vi.stubEnv('VITE_GOOGLE_GEOCODING_API_KEY', 'fake-geo-key')
    })

    afterEach(() => {
        vi.unstubAllEnvs()
    })

    const renderComponent = (stateData: any = { locationLabel: 'București' }) => {
        return render(
            <MemoryRouter initialEntries={[{ pathname: '/map', state: stateData }]}>
                <Routes>
                    <Route path="/map" element={<ExpenseMap />} />
                </Routes>
            </MemoryRouter>
        )
    }

    it('1. Tratează locația "Online" și navigarea înapoi', () => {
        renderComponent({ locationLabel: 'Online' })
        expect(screen.getByText(/Locația este non-geografică/i)).toBeInTheDocument()

        fireEvent.click(screen.getByLabelText(/înapoi/i))
        expect(mockNavigate).toHaveBeenCalledWith(-1)
    })

    it('2. Arată eroare dacă lipsește cheia API Google Maps', () => {
        vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', '')
        renderComponent()
        expect(screen.getByText(/Lipsește cheia Google Maps/i)).toBeInTheDocument()
    })

    it('3. WebSocket: Alertă live și cleanup la unmount', async () => {
        const { unmount } = renderComponent({ lat: 44, lng: 26, locationLabel: 'Live' })

        act(() => {
            activeWsInstance?.onmessage?.({
                data: JSON.stringify({ lat: 46.0, lng: 24.0, isRestricted: true })
            } as MessageEvent)
        })

        expect(await screen.findByText(/ALERTĂ DE SECURITATE/i)).toBeInTheDocument()
        unmount()
        expect(activeWsInstance?.close).toHaveBeenCalled()
    })

    it('3b. WebSocket: catch din onmessage cu JSON invalid (linia 106)', async () => {
        // Suprimăm console.error — componenta îl apelează în catch și Vitest îl tratează ca eroare
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

        renderComponent({ locationLabel: 'ValidLabel' })

        await act(async () => {
            activeWsInstance?.onmessage?.({
                data: 'not-valid-json{{{'
            } as MessageEvent)
        })

        // console.error a fost apelat cu mesajul de eroare din catch (linia 106)
        expect(consoleErrorSpy).toHaveBeenCalledWith(
            '❌ HARTA: Eroare la procesarea datelor live',
            expect.any(SyntaxError)
        )

        consoleErrorSpy.mockRestore()
    })

    it('4. Geocoding Succes: Salvează în DB și preia Place Details', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                results: [{ geometry: { location: { lat: 45, lng: 25 } }, place_id: 'p1' }]
            })
        } as Response)

        renderComponent({ locationLabel: 'Mall', locationId: 10 })

        await waitFor(() => {
            expect(locationsService.updateLocationCoordinates).toHaveBeenCalledWith(10, 45, 25)
        })
    })

    it('4b. searchPlaceNear + fetchPlaceDetails complet (liniile 163-185, 282-288, 313, 407)', async () => {
        // 1) Geocoding → coordonate
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                results: [{ geometry: { location: { lat: 45, lng: 25 } }, place_id: 'geo-p1' }]
            })
        } as Response)

        // 2) searchPlaceNear → place cu id (acoperă liniile 163-166)
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                places: [{ id: 'place-abc', googleMapsUri: 'https://maps.google.com/?place=abc' }]
            })
        } as Response)

        // 3) fetchPlaceDetails → date complete (acoperă liniile 169-196, 282-288, 407)
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                displayName: { text: 'Mall Vitan' },
                formattedAddress: 'Str. Vitan 1, București',
                rating: 4.5,
                userRatingCount: 1200,
                websiteUri: 'https://mallvitan.ro',
                regularOpeningHours: {
                    openNow: true,
                    weekdayDescriptions: ['Luni: 10:00-22:00', 'Marți: 10:00-22:00', 'Miercuri: 10:00-22:00']
                },
                internationalPhoneNumber: '+40 21 123 4567',
                photos: [{ name: 'places/place-abc/photos/photo1' }],
                googleMapsUri: 'https://maps.google.com/?place=abc-details'
            })
        } as Response)

        renderComponent({ locationLabel: 'MallVitanTest', locationId: 99 })

        await waitFor(() => expect(screen.getByText('Deschis')).toBeInTheDocument())
        await waitFor(() => expect(screen.getByText('+40 21 123 4567')).toBeInTheDocument())
        await waitFor(() => expect(screen.getByText('Luni: 10:00-22:00')).toBeInTheDocument())
    })

    it('5. Arată eroare dacă Geocoding returnează ZERO_RESULTS', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ status: 'ZERO_RESULTS', results: [] })
        } as Response)

        renderComponent({ locationLabel: 'Nimic' })
        expect(await screen.findByText(/ZERO_RESULTS/i)).toBeInTheDocument()
    })

    it('6. Prinde eroarea de salvare în Baza de Date (Catch branch)', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ results: [{ geometry: { location: { lat: 1, lng: 1 } } }] })
        } as Response)

        vi.mocked(locationsService.updateLocationCoordinates).mockRejectedValueOnce(new Error('DB Error'))

        renderComponent({ locationLabel: 'KFC', locationId: 5 })
        expect(await screen.findByText(/nu le-am putut salva în baza de date/i)).toBeInTheDocument()
    })

    it('7. Fallback: Navigare fără state (Ramura !label)', () => {
        render(
            <MemoryRouter initialEntries={['/map']}>
                <Routes><Route path="/map" element={<ExpenseMap />} /></Routes>
            </MemoryRouter>
        )
        expect(screen.getByText(/Nu există text de locație pentru geocoding/i)).toBeInTheDocument()
    })

    it('7b. Lipsește VITE_GOOGLE_GEOCODING_API_KEY → mesaj eroare (liniile 236-238)', async () => {
        vi.stubEnv('VITE_GOOGLE_GEOCODING_API_KEY', '')

        renderComponent({ locationLabel: 'TestFaraGeoKey' })

        expect(await screen.findByText(/Lipsește cheia pentru Geocoding API/i)).toBeInTheDocument()
    })

    it('8. Eroare de rețea: fetch în așteptare → mesajul de căutare rămâne vizibil', async () => {
        fetchMock.mockImplementationOnce(() => new Promise(() => { /* pending forever */ }))

        const { unmount } = renderComponent({ locationLabel: 'TestReteaEroare' })

        await waitFor(() => {
            expect(screen.getByText(/Se caută locația pe hartă/i)).toBeInTheDocument()
        })

        unmount()
    })

    it('9. Geocoding fără results și fără status → UNKNOWN error', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({})
        } as Response)

        renderComponent({ locationLabel: 'Locatie404' })

        await waitFor(() => {
            const allDivs = Array.from(document.querySelectorAll('div'))
            const messageDiv = allDivs.find(el => el.textContent === 'UNKNOWN')
            expect(messageDiv).toBeInTheDocument()
        })
    })

    it('10. InfoWindow: Afișare descriere și închidere', async () => {
        renderComponent({ lat: 44, lng: 26, locationLabel: 'Marker', description: 'Nota' })
        const marker = await screen.findByTestId('map-marker')
        fireEvent.click(marker)

        expect(screen.getByText('Nota')).toBeInTheDocument()
        fireEvent.click(screen.getByTestId('close-info'))
        expect(screen.queryByTestId('info-window')).not.toBeInTheDocument()
    })

    it('11. UseJsApiLoader: Branch-uri de încărcare și eroare', () => {
        // Cazul 1: Loading
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValueOnce({ isLoaded: false, loadError: null } as any)
        renderComponent()
        expect(screen.getByText(/Se încarcă harta/i)).toBeInTheDocument()

        // Cazul 2: Error
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValueOnce({ isLoaded: false, loadError: new Error('Map Crash') } as any)
        renderComponent()
        expect(screen.getByText(/Nu s-a putut încărca Google Maps/i)).toBeInTheDocument()
    })

    it('12. placePhotoSrc: afișează imaginea când place are photoName (linia 313)', async () => {
        // 1) Geocoding
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                results: [{ geometry: { location: { lat: 45, lng: 25 } } }]
            })
        } as Response)
        // 2) searchPlaceNear → place cu id
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                places: [{ id: 'photo-place-id' }]
            })
        } as Response)
        // 3) fetchPlaceDetails → cu photos (acoperă linia 313)
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                displayName: { text: 'Loc cu Poza' },
                photos: [{ name: 'places/photo-place-id/photos/xyz' }]
            })
        } as Response)

        renderComponent({ locationLabel: 'LocCuPozaTest' })

        await waitFor(() => {
            const imgs = document.querySelectorAll('img')
            const photoImg = Array.from(imgs).find(img =>
                img.src.includes('places.googleapis.com/v1/places')
            )
            expect(photoImg).toBeInTheDocument()
        })
    })
})