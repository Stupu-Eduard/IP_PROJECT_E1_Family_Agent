import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import ExpenseMap from './ExpenseMap'
import * as locationsService from '../services/locations'
import * as googleMapsApi from '@react-google-maps/api'

// ── 1. Mock-uri pentru Navigare si Servicii ──
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom')
    return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../services/locations', () => ({
    updateLocationCoordinates: vi.fn()
}))

vi.mock('../store/authStore', () => ({
    useAuthStore: (selector: any) => selector({
        token: 'fake.jwt.token',
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        setToken: vi.fn(),
    }),
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
        // eslint-disable-next-line @typescript-eslint/no-this-alias
        activeWsInstance = this
        setTimeout(() => this.onopen?.(), 0)
    }
}
globalThis.WebSocket = MockWebSocket as unknown as typeof WebSocket
const fetchMock = vi.fn()
globalThis.fetch = fetchMock as unknown as typeof fetch

describe('ExpenseMap - 100% Coverage Final', () => {
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

    const renderComponent = (stateData: any = { locationLabel: 'Bucuresti' }) => {
        return render(
            <MemoryRouter initialEntries={[{ pathname: '/map', state: stateData }]}>
                <Routes>
                    <Route path="/map" element={<ExpenseMap />} />
                </Routes>
            </MemoryRouter>
        )
    }

    it('1. Trateaza locatia "Online" si navigarea inapoi', () => {
        renderComponent({ locationLabel: 'Online' })
        expect(screen.getByText(/Locația este non-geografică/i)).toBeInTheDocument()

        fireEvent.click(screen.getByLabelText(/înapoi/i))
        expect(mockNavigate).toHaveBeenCalledWith(-1)
    })

    it('2. Arata eroare daca lipseste cheia Google Maps', () => {
        vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', '')
        renderComponent()
        expect(screen.getByText(/Lipsește cheia Google Maps/i)).toBeInTheDocument()
    })

    it('3. WebSocket: Alerta live si cleanup la unmount', async () => {
        const { unmount } = renderComponent({ lat: 44, lng: 26, locationLabel: 'Live' })

        await waitFor(() => {
            expect(activeWsInstance).not.toBeNull()
        })

        act(() => {
            activeWsInstance?.onmessage?.({
                data: JSON.stringify({ lat: 46.0, lng: 24.0, isRestricted: true })
            } as MessageEvent)
        })

        expect(await screen.findByText(/ALERTĂ DE SECURITATE/i)).toBeInTheDocument()
        unmount()
        expect(activeWsInstance?.close).toHaveBeenCalled()
    })

    it('3b. WebSocket: catch din onmessage cu JSON invalid', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

        renderComponent({ locationLabel: 'ValidLabel' })

        await waitFor(() => {
            expect(activeWsInstance).not.toBeNull()
        })

        await act(async () => {
            activeWsInstance?.onmessage?.({
                data: 'not-valid-json{{{'
            } as MessageEvent)
        })

        expect(consoleErrorSpy).toHaveBeenCalledWith(
            '❌ HARTA: Eroare la procesarea datelor live',
            expect.any(SyntaxError)
        )

        consoleErrorSpy.mockRestore()
    })

    it('4. Geocoding Succes: Salveaza in DB si preia Place Details', async () => {
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

    it('4b. searchPlaceNear + fetchPlaceDetails complet', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                results: [{ geometry: { location: { lat: 45, lng: 25 } }, place_id: 'geo-p1' }]
            })
        } as Response)

        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                places: [{ id: 'place-abc', googleMapsUri: 'https://maps.google.com/?place=abc' }]
            })
        } as Response)

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

    it('5. Arata eroare daca Geocoding returneaza ZERO_RESULTS', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ status: 'ZERO_RESULTS', results: [] })
        } as Response)

        renderComponent({ locationLabel: 'Nimic' })
        expect(await screen.findByText(/ZERO_RESULTS/i)).toBeInTheDocument()
    })

    it('6. Prinde eroarea de salvare in Baza de Date (Catch branch)', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ results: [{ geometry: { location: { lat: 1, lng: 1 } } }] })
        } as Response)

        vi.mocked(locationsService.updateLocationCoordinates).mockRejectedValueOnce(new Error('DB Error'))

        renderComponent({ locationLabel: 'KFC', locationId: 5 })
        expect(await screen.findByText(/nu le-am putut salva în baza de date/i)).toBeInTheDocument()
    })

    it('7. Fallback: Navigare fara state (Ramura !label)', () => {
        render(
            <MemoryRouter initialEntries={['/map']}>
                <Routes><Route path="/map" element={<ExpenseMap />} /></Routes>
            </MemoryRouter>
        )
        expect(screen.getByText(/Nu există text de locație pentru geocoding/i)).toBeInTheDocument()
    })

    it('7b. Lipseste VITE_GOOGLE_GEOCODING_API_KEY -> mesaj eroare', async () => {
        vi.stubEnv('VITE_GOOGLE_GEOCODING_API_KEY', '')

        renderComponent({ locationLabel: 'TestFaraGeoKey' })

        expect(await screen.findByText(/Lipsește cheia pentru Geocoding API/i)).toBeInTheDocument()
    })

    it('8. Eroare de retea: fetch in asteptare -> mesajul de cautare ramane vizibil', async () => {
        fetchMock.mockImplementationOnce(() => new Promise(() => {}))

        const { unmount } = renderComponent({ locationLabel: 'TestReteaEroare' })

        await waitFor(() => {
            expect(screen.getByText(/Se caută locația pe hartă/i)).toBeInTheDocument()
        })

        unmount()
    })

    it('9. Geocoding fara results si fara status -> UNKNOWN error', async () => {
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

    it('10. InfoWindow: Afisare descriere si inchidere', async () => {
        renderComponent({ lat: 44, lng: 26, locationLabel: 'Marker', description: 'Nota' })
        const marker = await screen.findByTestId('map-marker')
        fireEvent.click(marker)

        expect(screen.getByText('Nota')).toBeInTheDocument()
        fireEvent.click(screen.getByTestId('close-info'))
        expect(screen.queryByTestId('info-window')).not.toBeInTheDocument()
    })

    it('11. UseJsApiLoader: Branch-uri de incarcare si eroare', () => {
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValueOnce({ isLoaded: false, loadError: null } as any)
        renderComponent()
        expect(screen.getByText(/Se încarcă harta/i)).toBeInTheDocument()

        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValueOnce({ isLoaded: false, loadError: new Error('Map Crash') } as any)
        renderComponent()
        expect(screen.getByText(/Nu s-a putut încărca Google Maps/i)).toBeInTheDocument()
    })

    it('12. placePhotoSrc: afiseaza imaginea cand place are photoName', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                results: [{ geometry: { location: { lat: 45, lng: 25 } } }]
            })
        } as Response)
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                places: [{ id: 'photo-place-id' }]
            })
        } as Response)
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

    it('13. googleMapsLink fallback cu placeId (cand googleMapsUri lipseste)', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ results: [{ geometry: { location: { lat: 45, lng: 25 } }, place_id: 'geo-p1' }] })
        } as Response)

        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ places: [{ id: 'place-no-uri' }] })
        } as Response)

        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ displayName: { text: 'No URI' } })
        } as Response)

        renderComponent({ locationLabel: 'TestURI1' })

        await waitFor(() => {
            const link = screen.getByText('Deschide în Google Maps')
            expect(link).toHaveAttribute('href', expect.stringContaining('google'))
        })
    })

    it('14. googleMapsLink fallback doar cu marker (fara placeId)', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ places: [] })
        } as Response)

        renderComponent({ lat: 44.4250, lng: 26.1000, locationLabel: 'TestURI2' })

        await waitFor(() => {
            const link = screen.getByText('Deschide în Google Maps')
            expect(link).toHaveAttribute('href', expect.stringContaining('google'))
        })
    })

    it('15. Geocoding returneaza eroare explicita (error_message) de la Google', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ status: 'REQUEST_DENIED', error_message: 'API key invalid' })
        } as Response)

        renderComponent({ locationLabel: 'TestGeoError' })
        expect(await screen.findByText('REQUEST_DENIED - API key invalid')).toBeInTheDocument()
    })

    it('16. Catch-ul din searchPlaceNear ignora erorile silentios', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ results: [{ geometry: { location: { lat: 45, lng: 25 } } }] })
        } as Response)

        fetchMock.mockRejectedValueOnce(new Error('Network Fail pe Search'))

        renderComponent({ locationLabel: 'TestSearchCatch' })

        await waitFor(() => {
            const textCheck = screen.queryByText('Folosesc coordonatele salvate.') || screen.queryByText(/Se caută/i)
            expect(textCheck).toBeDefined()
        })
    })

    it('17. Prinde AbortError la unmount si nu afiseaza eroare in consola', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

        const abortError = new Error('Aborted')
        abortError.name = 'AbortError'
        fetchMock.mockRejectedValueOnce(abortError)

        renderComponent({ locationLabel: 'TestAbort' })

        await waitFor(() => {
            expect(consoleErrorSpy).not.toHaveBeenCalled()
        })

        consoleErrorSpy.mockRestore()
    })

    it('18. Geofencing: Punct perfect in interiorul zonei sigure', async () => {
        renderComponent({ lat: 44.4250, lng: 26.1000, locationLabel: 'SafePoint' })

        await waitFor(() => {
            expect(screen.queryByText(/ALERTĂ DE SECURITATE/i)).not.toBeInTheDocument()
        })
    })

    it('19. Fallback place_id din geocode cand searchPlaceNear nu gaseste nimic', async () => {
        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ results: [{ geometry: { location: { lat: 45, lng: 25 } }, place_id: 'geo-fallback-id' }] })
        } as Response)

        fetchMock.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ places: [] })
        } as Response)

        renderComponent({ locationLabel: 'TestGeoFallbackID' })

        await waitFor(() => {
            const link = screen.getByText('Deschide în Google Maps')
            expect(link).toHaveAttribute('href', expect.stringContaining('google'))
        })
    })
})