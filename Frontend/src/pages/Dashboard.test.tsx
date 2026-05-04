import '@testing-library/jest-dom';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach, beforeAll } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';
import { useAuthStore } from '../store/authStore';
import * as googleMapsApi from '@react-google-maps/api';

// ─── MOCKS ────────────────────────────────────────────────────────────

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../store/authStore', () => ({
    useAuthStore: vi.fn(),
}));

vi.mock('./KidDashboard', () => ({
    default: () => <div data-testid="kid-dashboard">Kid Dashboard Mock</div>,
}));

vi.mock('@react-google-maps/api', () => ({
    useJsApiLoader: vi.fn(() => ({ isLoaded: false })),
    GoogleMap: ({ children }: { children: React.ReactNode }) => <div data-testid="google-map">{children}</div>,
    Marker: () => <div data-testid="map-marker" />,
}));

// Mock pentru WebSocket
let activeWsInstance: MockWebSocket | null = null;
class MockWebSocket {
    onmessage: ((event: MessageEvent) => void) | null = null;
    close = vi.fn();
    public url: string;
    constructor(url: string) {
        this.url = url;
        // eslint-disable-next-line @typescript-eslint/no-this-alias
        activeWsInstance = this;
    }
}
// eslint-disable-next-line @typescript-eslint/no-explicit-any
(globalThis as any).WebSocket = MockWebSocket;

// Generatoare de token-uri pentru toate cazurile
const generateMockToken = (payloadObj: Record<string, unknown>) => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify(payloadObj));
    return `${header}.${payload}.signature`;
};

// ─── SUITA DE TESTE ───────────────────────────────────────────────────

describe('Dashboard Component', () => {

    beforeAll(() => {
        // Mock pentru getBoundingClientRect ca să acoperim 100% onMouseMove-ul
        Element.prototype.getBoundingClientRect = vi.fn(() => ({
            width: 100, height: 100, top: 10, left: 10, bottom: 0, right: 0, x: 0, y: 0, toJSON: () => ({})
        }));
    });

    beforeEach(() => {
        vi.clearAllMocks();
        vi.useFakeTimers();
        activeWsInstance = null;
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    // ─── 1. TESTE DE AUTENTIFICARE ȘI ROLURI (JWT) ───

    it('randează <KidDashboard /> dacă rolul este Child', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: generateMockToken({ role: 'Child' }) }));
        render(<MemoryRouter><Dashboard /></MemoryRouter>);
        expect(screen.getByTestId('kid-dashboard')).toBeInTheDocument();
    });

    it('folosește rolul implicit Parent dacă JWT-ul este valid, dar nu are campul "role"', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: generateMockToken({ otherData: 'test' }) }));
        render(<MemoryRouter><Dashboard /></MemoryRouter>);
        expect(screen.getByText(/Bine ai revenit/i)).toBeInTheDocument();
    });

    it('folosește rolul implicit Parent dacă JWT-ul e corupt complet', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'corrupted-token-without-dots' }));
        render(<MemoryRouter><Dashboard /></MemoryRouter>);
        expect(screen.getByText(/Bine ai revenit/i)).toBeInTheDocument();
    });

    it('folosește rolul implicit Parent dacă token-ul lipsește (null)', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: null }));
        render(<MemoryRouter><Dashboard /></MemoryRouter>);
        expect(screen.getByText(/Bine ai revenit/i)).toBeInTheDocument();
    });

    // ─── 2. TESTE DE INTERACȚIUNE ȘI NAVIGARE ───

    it('navighează corect la toate linkurile (KPI, Acțiuni, Extra)', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: generateMockToken({ role: 'Parent' }) }));
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        // Test KPI Click
        fireEvent.click(screen.getByText('CHELTUIELI LUNA ACEASTA').closest('.kpi')!);
        expect(mockNavigate).toHaveBeenCalledWith('/expenses');

        // Test Quick Action Click
        fireEvent.click(screen.getByText('Membri familie').closest('.qa')!);
        expect(mockNavigate).toHaveBeenCalledWith('/family');

        // Test Butoane secundare
        fireEvent.click(screen.getByText(/Vezi traseul complet/i));
        expect(mockNavigate).toHaveBeenCalledWith('/expenses/map');

        fireEvent.click(screen.getByText(/Detalii/i));
        expect(mockNavigate).toHaveBeenCalledWith('/reports');

        fireEvent.click(screen.getByText(/Vezi toate/i));
        expect(mockNavigate).toHaveBeenCalledWith('/expenses');
    });

    it('aplică efectul de glow la mousemove pe KPI (getBoundingClientRect)', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: generateMockToken({ role: 'Parent' }) }));
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        const kpiCard = screen.getByText('CHELTUIELI LUNA ACEASTA').closest('.kpi')!;
        fireEvent.mouseMove(kpiCard, { clientX: 50, clientY: 50 });

        // Verificăm dacă s-au setat corect variabilele CSS custom (--mx, --my)
        expect((kpiCard as HTMLElement).style.getPropertyValue('--mx')).toBe('40px'); // 50 - 10 (din mock-ul bounding box)
        expect((kpiCard as HTMLElement).style.getPropertyValue('--my')).toBe('40px');
    });

    // ─── 3. TESTE DE INTERVAL ȘI WEBSOCKET ───

    it('incrementează timer-ul de actualizare corect', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: generateMockToken({ role: 'Parent' }) }));
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        expect(screen.getByText(/Actualizat acum 1s/i)).toBeInTheDocument();
        act(() => { vi.advanceTimersByTime(4500); });
        expect(screen.getByText(/Actualizat acum 2s/i)).toBeInTheDocument();
    });

    it('randează harta reală când locația este primită și este RESTRICȚIONATĂ', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: generateMockToken({ role: 'Parent' }) }));
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValue({ isLoaded: true, loadError: undefined });

        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        act(() => {
            activeWsInstance?.onmessage?.({ data: JSON.stringify({ lat: 44.4268, lng: 26.1025, isRestricted: true }) } as MessageEvent);
        });

        expect(screen.getByText('Locație detectată')).toBeInTheDocument();
        expect(screen.getByTestId('google-map')).toBeInTheDocument();
        expect(screen.getByText('⚠ ZONĂ RESTRICȚIONATĂ!')).toBeInTheDocument();
    });

    it('randează harta reală când locația este primită și este SIGURĂ (isRestricted = false)', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: generateMockToken({ role: 'Parent' }) }));
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValue({ isLoaded: true, loadError: undefined });

        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        act(() => {
            activeWsInstance?.onmessage?.({ data: JSON.stringify({ lat: 44.4268, lng: 26.1025, isRestricted: false }) } as MessageEvent);
        });

        // Acoperim cazul cu textul verde
        expect(screen.getByText('✓ Zonă Sigură')).toBeInTheDocument();
    });

    it('randează harta MOCK (fallback) dacă locația este incompletă (fără lng)', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: generateMockToken({ role: 'Parent' }) }));
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValue({ isLoaded: true, loadError: undefined });

        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        act(() => {
            // Trimitem doar latitudinea (fără lng), ar trebui să prindă "Locație detectată" dar să afișeze harta Mock
            activeWsInstance?.onmessage?.({ data: JSON.stringify({ lat: 44.4268 }) } as MessageEvent);
        });

        expect(screen.getByText('Locație detectată')).toBeInTheDocument();
        expect(screen.queryByTestId('google-map')).not.toBeInTheDocument();
        expect(screen.getByText(/Andrei · 1.2 km/i)).toBeInTheDocument(); // Element din mock map
    });

    it('randează harta MOCK și procesează erori JSON de la WebSocket (catch block)', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: generateMockToken({ role: 'Parent' }) }));
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValue({ isLoaded: true, loadError: undefined });

        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        act(() => {
            // Date invalide JSON (ajunge in block-ul catch care setează { raw: e.data })
            activeWsInstance?.onmessage?.({ data: 'O eroare neprevazuta de server' } as MessageEvent);
        });

        // liveLocation.lat va fi false, deci apare 'În drum spre școală'
        expect(screen.getByText('În drum spre școală')).toBeInTheDocument();
        expect(screen.queryByTestId('google-map')).not.toBeInTheDocument();
    });

    it('curăță socket-ul și intervalul la demontare (unmount cleanup)', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: generateMockToken({ role: 'Parent' }) }));
        const { unmount } = render(<MemoryRouter><Dashboard /></MemoryRouter>);

        expect(activeWsInstance).not.toBeNull();

        unmount();

        // Verificăm cleanup function-ul din useEffect
        expect(activeWsInstance?.close).toHaveBeenCalled();
    });
});
