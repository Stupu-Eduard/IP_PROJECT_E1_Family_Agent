import '@testing-library/jest-dom';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach, beforeAll } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';
import { useAuthStore } from '../store/authStore';
import * as googleMapsApi from '@react-google-maps/api';
import * as apiModule from '../services/api';

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

vi.mock('../services/api', () => ({
    api: { get: vi.fn() },
}));

// ─── Mock WebSocket ───────────────────────────────────────────────────
let activeWsInstance: MockWebSocket | null = null;
class MockWebSocket {
    onopen:    (() => void) | null = null;
    onmessage: ((event: MessageEvent) => void) | null = null;
    onerror:   ((err: Event) => void) | null = null;
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

// ─── Helper token ─────────────────────────────────────────────────────
const generateMockToken = (payloadObj: Record<string, unknown>) => {
    const header  = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify(payloadObj));
    return `${header}.${payload}.signature`;
};

// ─── Date mock API ────────────────────────────────────────────────────
const thisMonth = new Date().toISOString().slice(0, 7);
const today     = new Date().toISOString().slice(0, 10);

const prevDate = (() => {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    return d.toISOString().slice(0, 7);
})();

// Cheltuieli care acoperă TOATE ramurile din categoryEmoji + toate cazurile de desc/person
const mockExpenses = [
    // 🛒 Alimentare — rând cu store + city + person
    {
        id: 1, amount: 186.40, description: 'Cumpărături',
        expenseDate: `${thisMonth}-05T10:00:00`, category: 'Alimentare', person: 'Mihaela',
        location: { store: 'Mega Image', city: 'Cluj-Napoca' },
    },
    // 🚗 Transport — rând normal
    {
        id: 2, amount: 320.00, description: 'Plin rezervor',
        expenseDate: `${thisMonth}-08T12:00:00`, category: 'Transport', person: 'Eduard',
        location: { store: 'OMV', city: 'Cluj-Napoca' },
    },
    // 📚 Educatie — person: null (acoperă ramura r.person absent) + location: null → description
    {
        id: 3, amount: 48.90, description: 'Caiete',
        expenseDate: today + 'T09:00:00', category: 'Educație',
        person: null,
        location: null,
    },
    // 💊 Sanatate — store+city goale → foloseşte description
    {
        id: 4, amount: 75.00, description: 'Medicamente',
        expenseDate: today + 'T11:00:00', category: 'Sanatate', person: 'Mihaela',
        location: { store: '', city: '' },
    },
    // 🎮 Divertisment — description null → 'Fără detalii'
    {
        id: 5, amount: 30.00, description: null,
        expenseDate: today + 'T13:00:00', category: 'Divertisment', person: 'Andrei',
        location: { store: '', city: '' },
    },
];

// Cheltuieli cu date luna trecută (pentru ramura delta > 0)
const mockExpensesWithPrev = [
    ...mockExpenses,
    {
        id: 6, amount: 500.00, description: 'Factura curent',
        expenseDate: `${prevDate}-10T10:00:00`, category: 'Facturi', person: 'Eduard',
        location: { store: 'Enel', city: 'Cluj-Napoca' },
    },
];

// ─── SUITA DE TESTE ───────────────────────────────────────────────────

describe('Dashboard Component', () => {

    beforeAll(() => {
        Element.prototype.getBoundingClientRect = vi.fn(() => ({
            width: 100, height: 100, top: 10, left: 10,
            bottom: 0, right: 0, x: 0, y: 0, toJSON: () => ({}),
        }));
    });

    beforeEach(() => {
        vi.clearAllMocks();
        // shouldAdvanceTime: true → waitFor funcționează corect cu fake timers active
        vi.useFakeTimers({ shouldAdvanceTime: true });
        activeWsInstance = null;
        vi.mocked(apiModule.api.get).mockResolvedValue({ data: mockExpenses });
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    // ═══════════════════════════════════════════════════════════════════
    // 1. ROLURI ȘI AUTENTIFICARE (JWT)
    // ═══════════════════════════════════════════════════════════════════

    it('randează <KidDashboard /> dacă rolul este Child', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Child' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);
        expect(screen.getByTestId('kid-dashboard')).toBeInTheDocument();
    });

    it('folosește rolul implicit Parent dacă JWT-ul nu are câmpul "role"', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ otherData: 'test' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);
        expect(screen.getByText(/Bine ai revenit/i)).toBeInTheDocument();
    });

    it('folosește rolul implicit Parent dacă JWT-ul e corupt complet', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: 'corrupted-token-without-dots' })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);
        expect(screen.getByText(/Bine ai revenit/i)).toBeInTheDocument();
    });

    it('folosește rolul implicit Parent dacă token-ul lipsește (null)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: null })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);
        expect(screen.getByText(/Bine ai revenit/i)).toBeInTheDocument();
    });

    // ═══════════════════════════════════════════════════════════════════
    // 2. STARE LOADING
    // ═══════════════════════════════════════════════════════════════════

    it('afișează starea de loading înainte ca API-ul să răspundă', () => {
        vi.mocked(apiModule.api.get).mockReturnValue(new Promise(() => {}));
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        // Hero: "Se încarcă datele..."
        expect(screen.getByText('Se încarcă datele...')).toBeInTheDocument();
        // KPI cards arată '...'
        expect(screen.getAllByText('...').length).toBeGreaterThan(0);
        // Grafic loading
        expect(screen.getByText('Se încarcă...')).toBeInTheDocument();
        // Activitate recentă loading
        expect(screen.getByText('Se încarcă activitatea...')).toBeInTheDocument();
    });

    // ═══════════════════════════════════════════════════════════════════
    // 3. DATE REALE DIN API
    // ═══════════════════════════════════════════════════════════════════

    it('afișează KPI-urile și herotext-ul după încărcarea datelor', async () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent', sub: 'test@test.com' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByText('CHELTUIELI LUNA ACEASTA')).toBeInTheDocument();
            expect(screen.getByText('TOTAL CHELTUIELI')).toBeInTheDocument();
            expect(screen.getByText('TRANZACȚII LUNA')).toBeInTheDocument();
            // Hero text cu kpi !== null
            expect(screen.getByText(/cheltuieli înregistrate luna aceasta/i)).toBeInTheDocument();
        });

        expect(apiModule.api.get).toHaveBeenCalledWith(
            '/api/v1/expenses',
            expect.objectContaining({ signal: expect.any(AbortSignal) })
        );
    });

    it('afișează numele din câmpul "sub" al token-ului', async () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent', sub: 'mihaela@familie.com' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByText(/mihaela@familie\.com/i)).toBeInTheDocument();
        });
    });

    it('afișează numele din câmpul "name" al token-ului când "sub" lipsește', async () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent', name: 'Ion Popescu' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByText(/Ion Popescu/i)).toBeInTheDocument();
        });
    });

    it('afișează "utilizator" dacă token-ul nu conține sub/name/userName', async () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByText(/utilizator/i)).toBeInTheDocument();
        });
    });

    it('afișează activitatea recentă inclusiv rânduri fără person', async () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            // Rând cu store + city
            expect(screen.getByText(/Mega Image/i)).toBeInTheDocument();
            // Persoana afișată (folosim getAllByText pentru că apare de mai multe ori în mockExpenses)
            expect(screen.getAllByText('Mihaela')[0]).toBeInTheDocument();
            // Categorie rând fără person
            expect(screen.getByText('Educație')).toBeInTheDocument();
        });
    });

    it('desc fallback: afișează description când store și city sunt goale', async () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            // id:4 store:'', city:'' → foloseşte description: 'Medicamente'
            expect(screen.getByText('Medicamente')).toBeInTheDocument();
        });
    });

    it('desc fallback: afișează "Fără detalii" când store, city și description sunt toate goale', async () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            // id:5 store:'', city:'', description:null → 'Fără detalii'
            expect(screen.getByText('Fără detalii')).toBeInTheDocument();
        });
    });

    it('afișează graficul de 7 zile și media zilnică după încărcare', async () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByText('EVOLUȚIE 7 ZILE')).toBeInTheDocument();
            expect(screen.getByText(/Medie zilnică/i)).toBeInTheDocument();
        });
    });

    it('afișează mesaj empty state când API-ul returnează listă goală', async () => {
        vi.mocked(apiModule.api.get).mockResolvedValue({ data: [] });
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByText('Nu există cheltuieli înregistrate încă.')).toBeInTheDocument();
        });
    });

    it('afișează "Bine ai venit în FamilyAgent." când kpi este null (eroare API)', async () => {
        vi.mocked(apiModule.api.get).mockRejectedValue(new Error('Network error'));
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            // isLoading=false, kpi=null → ramura else din hero text
            expect(screen.getByText('Bine ai venit în FamilyAgent.')).toBeInTheDocument();
        });

        // KPI-urile afișează '—' (kpi null, loading false)
        expect(screen.getAllByText('—').length).toBeGreaterThan(0);
    });

    it('calculează delta față de luna trecută când există cheltuieli anterioare (ramura totalLunaT > 0)', async () => {
        vi.mocked(apiModule.api.get).mockResolvedValue({ data: mockExpensesWithPrev });
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            // Delta formatat cu % și semn → conține "vs luna trecută"
            expect(screen.getByText(/vs luna trecută/i)).toBeInTheDocument();
        });
    });

    it('nu face fetch și nu creează WebSocket dacă rolul este Child', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Child' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        expect(apiModule.api.get).not.toHaveBeenCalled();
        expect(activeWsInstance).toBeNull();
    });

    it('ignoră eroarea AbortError la unmount (ramura silențioasă din catch)', async () => {
        const abortError = new Error('AbortError');
        abortError.name = 'AbortError';
        vi.mocked(apiModule.api.get).mockRejectedValue(abortError);
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );

        expect(() => render(<MemoryRouter><Dashboard /></MemoryRouter>)).not.toThrow();

        await waitFor(() => {
            // finally → setIsLoading(false) rulează chiar și cu AbortError
            expect(screen.queryByText('Se încarcă datele...')).not.toBeInTheDocument();
        });
    });

    it('ignoră eroarea CanceledError (axios cancel) — ramura silențioasă din catch', async () => {
        const canceledError = new Error('CanceledError');
        canceledError.name = 'CanceledError';
        vi.mocked(apiModule.api.get).mockRejectedValue(canceledError);
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );

        expect(() => render(<MemoryRouter><Dashboard /></MemoryRouter>)).not.toThrow();

        await waitFor(() => {
            expect(screen.queryByText('Se încarcă datele...')).not.toBeInTheDocument();
        });
    });

    // ═══════════════════════════════════════════════════════════════════
    // 4. NAVIGARE
    // ═══════════════════════════════════════════════════════════════════

    it('navighează corect la toate KPI-urile', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        fireEvent.click(screen.getByText('CHELTUIELI LUNA ACEASTA').closest('.kpi')!);
        expect(mockNavigate).toHaveBeenCalledWith('/expenses');

        fireEvent.click(screen.getByText('TOTAL CHELTUIELI').closest('.kpi')!);
        expect(mockNavigate).toHaveBeenCalledWith('/expenses');

        fireEvent.click(screen.getByText('TRANZACȚII LUNA').closest('.kpi')!);
        expect(mockNavigate).toHaveBeenCalledWith('/reports');
    });

    it('navighează corect la toate acțiunile rapide', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        fireEvent.click(screen.getByText('Adaugă cheltuială').closest('.qa')!);
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense');

        fireEvent.click(screen.getByText('Scanează bon').closest('.qa')!);
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense');

        fireEvent.click(screen.getByText('Evoluție cheltuieli').closest('.qa')!);
        expect(mockNavigate).toHaveBeenCalledWith('/reports');

        fireEvent.click(screen.getByText('Membri familie').closest('.qa')!);
        expect(mockNavigate).toHaveBeenCalledWith('/family');
    });

    it('navighează la butoanele secundare (traseul complet, detalii, vezi toate)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        fireEvent.click(screen.getByText(/Vezi traseul complet/i));
        expect(mockNavigate).toHaveBeenCalledWith('/expenses/map');

        fireEvent.click(screen.getByText(/Detalii/i));
        expect(mockNavigate).toHaveBeenCalledWith('/reports');

        fireEvent.click(screen.getByText(/Vezi toate/i));
        expect(mockNavigate).toHaveBeenCalledWith('/expenses');
    });

    it('navighează la /expenses la click pe un rând din activitatea recentă', async () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByText(/Mega Image/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText(/Mega Image/i).closest('.row-clickable')!);
        expect(mockNavigate).toHaveBeenCalledWith('/expenses');
    });

    // ═══════════════════════════════════════════════════════════════════
    // 5. EFECT GLOW KPI (onMouseMove)
    // ═══════════════════════════════════════════════════════════════════

    it('aplică efectul de glow (--mx, --my) la mousemove pe KPI', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        const kpiCard = screen.getByText('CHELTUIELI LUNA ACEASTA').closest('.kpi')!;
        fireEvent.mouseMove(kpiCard, { clientX: 50, clientY: 50 });

        // 50 - 10 (left/top din mock getBoundingClientRect) = 40px
        expect((kpiCard as HTMLElement).style.getPropertyValue('--mx')).toBe('40px');
        expect((kpiCard as HTMLElement).style.getPropertyValue('--my')).toBe('40px');
    });

    // ═══════════════════════════════════════════════════════════════════
    // 6. TIMER ACTUALIZARE
    // ═══════════════════════════════════════════════════════════════════

    it('incrementează timer-ul și ciclul se reia după 5 iterații (tick % 5)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        expect(screen.getByText(/Actualizat acum 1s/i)).toBeInTheDocument();
        act(() => { vi.advanceTimersByTime(4500); });
        expect(screen.getByText(/Actualizat acum 2s/i)).toBeInTheDocument();
        // Avansăm încă 4 cicluri → tick=5 → tick%5=0 → 0+1=1
        act(() => { vi.advanceTimersByTime(4500 * 4); });
        expect(screen.getByText(/Actualizat acum 1s/i)).toBeInTheDocument();
    });

    // ═══════════════════════════════════════════════════════════════════
    // 7. WEBSOCKET — LOCAȚIE LIVE
    // ═══════════════════════════════════════════════════════════════════

    it('randează harta reală când locația este RESTRICȚIONATĂ', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValue({ isLoaded: true, loadError: undefined });

        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        act(() => {
            activeWsInstance?.onmessage?.({
                data: JSON.stringify({ lat: 44.4268, lng: 26.1025, isRestricted: true }),
            } as MessageEvent);
        });

        expect(screen.getByText('Locație detectată')).toBeInTheDocument();
        expect(screen.getByTestId('google-map')).toBeInTheDocument();
        expect(screen.getByText('⚠ ZONĂ RESTRICȚIONATĂ!')).toBeInTheDocument();
        // Coordonatele sunt afișate
        expect(screen.getByText(/Lat:/i)).toBeInTheDocument();
        expect(screen.getByText(/Lng:/i)).toBeInTheDocument();
    });

    it('randează harta reală când locația este SIGURĂ (isRestricted = false)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValue({ isLoaded: true, loadError: undefined });

        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        act(() => {
            activeWsInstance?.onmessage?.({
                data: JSON.stringify({ lat: 44.4268, lng: 26.1025, isRestricted: false }),
            } as MessageEvent);
        });

        expect(screen.getByText('✓ Zonă Sigură')).toBeInTheDocument();
    });

    it('randează harta MOCK (fallback) dacă locația este incompletă (fără lng)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValue({ isLoaded: true, loadError: undefined });

        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        act(() => {
            activeWsInstance?.onmessage?.({
                data: JSON.stringify({ lat: 44.4268 }),
            } as MessageEvent);
        });

        expect(screen.getByText('Locație detectată')).toBeInTheDocument();
        expect(screen.queryByTestId('google-map')).not.toBeInTheDocument();
        expect(screen.getByText(/Așteptare semnal GPS/i)).toBeInTheDocument();
        // Elementele din starea de așteptare sunt prezente
        expect(screen.getByText(/Așteptând date de localizare/i)).toBeInTheDocument();
        expect(screen.getByText('✓ Conectat')).toBeInTheDocument();
    });

    it('procesează erori JSON de la WebSocket (catch block → { raw: ... })', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        vi.mocked(googleMapsApi.useJsApiLoader).mockReturnValue({ isLoaded: true, loadError: undefined });

        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        act(() => {
            activeWsInstance?.onmessage?.({
                data: 'date invalide non-JSON {{{',
            } as MessageEvent);
        });

        // { raw: '...' } → liveLocation.lat e undefined → "În așteptare..."
        expect(screen.getByText('În așteptare...')).toBeInTheDocument();
        expect(screen.queryByTestId('google-map')).not.toBeInTheDocument();
    });

    it('triggera onopen și onerror pe WebSocket fără crash', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        // onopen și onerror sunt setate în componentă — le apelăm direct
        expect(() => {
            activeWsInstance?.onopen?.();
            activeWsInstance?.onerror?.(new Event('error'));
        }).not.toThrow();
    });

    it('WebSocket-ul nu se conectează dacă rolul este Child', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Child' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);
        expect(activeWsInstance).toBeNull();
    });

    // ═══════════════════════════════════════════════════════════════════
    // 8. CLEANUP (unmount)
    // ═══════════════════════════════════════════════════════════════════

    it('curăță WebSocket-ul și intervalul la demontare (unmount cleanup)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        const { unmount } = render(<MemoryRouter><Dashboard /></MemoryRouter>);

        expect(activeWsInstance).not.toBeNull();
        unmount();
        expect(activeWsInstance?.close).toHaveBeenCalled();
    });

    it('anulează request-ul API la unmount (AbortController)', () => {
        vi.mocked(apiModule.api.get).mockReturnValue(new Promise(() => {}));
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );

        const { unmount } = render(<MemoryRouter><Dashboard /></MemoryRouter>);
        unmount();

        expect(apiModule.api.get).toHaveBeenCalledWith(
            '/api/v1/expenses',
            expect.objectContaining({ signal: expect.any(AbortSignal) })
        );
    });
// ═══════════════════════════════════════════════════════════════════
    // 9. CATEGORY EMOJI — TOATE RAMURILE din categoryEmoji()
    // ═══════════════════════════════════════════════════════════════════

    it('afișează emoji-ul corect pentru categoriile principale (partea 1)', async () => {
        const batch1 = [
            { id: 10, amount: 10, description: 'a', expenseDate: today + 'T10:00:00', category: 'Alimentare',   person: null, location: null },
            { id: 11, amount: 10, description: 'b', expenseDate: today + 'T10:01:00', category: 'Transport',    person: null, location: null },
            { id: 12, amount: 10, description: 'c', expenseDate: today + 'T10:02:00', category: 'Educatie',     person: null, location: null },
            { id: 13, amount: 10, description: 'd', expenseDate: today + 'T10:03:00', category: 'Sanatate',     person: null, location: null },
        ];

        vi.mocked(apiModule.api.get).mockResolvedValue({ data: batch1 });
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByText('🛒')).toBeInTheDocument();  // Alimentare
            expect(screen.getByText('🚗')).toBeInTheDocument();  // Transport
            expect(screen.getByText('📚')).toBeInTheDocument();  // Educatie
            expect(screen.getByText('💊')).toBeInTheDocument();  // Sanatate
        });
    });

    it('afișează emoji-ul corect pentru categoriile principale (partea 2)', async () => {
        const batch2 = [
            { id: 14, amount: 10, description: 'e', expenseDate: today + 'T10:04:00', category: 'Divertisment', person: null, location: null },
            { id: 15, amount: 10, description: 'f', expenseDate: today + 'T10:05:00', category: 'Facturi',      person: null, location: null },
            { id: 16, amount: 10, description: 'g', expenseDate: today + 'T10:06:00', category: 'Altele',       person: null, location: null },
        ];

        vi.mocked(apiModule.api.get).mockResolvedValue({ data: batch2 });
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByText('🎮')).toBeInTheDocument();  // Divertisment
            expect(screen.getByText('📄')).toBeInTheDocument();  // Facturi
            expect(screen.getByText('💳')).toBeInTheDocument();  // Default (Altele)
        });
    });

    it('categoryEmoji: acoperă variantele alternative ale cuvintelor cheie (partea 1)', async () => {
        const altBatch1 = [
            { id: 20, amount: 5, description: 'a', expenseDate: today + 'T08:00:00', category: 'mancare',       person: null, location: null },
            { id: 21, amount: 5, description: 'b', expenseDate: today + 'T08:01:00', category: 'food',          person: null, location: null },
            { id: 22, amount: 5, description: 'c', expenseDate: today + 'T08:02:00', category: 'combustibil',   person: null, location: null },
            { id: 23, amount: 5, description: 'd', expenseDate: today + 'T08:03:00', category: 'medicamente',   person: null, location: null },
        ];

        vi.mocked(apiModule.api.get).mockResolvedValue({ data: altBatch1 });
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getAllByText('🛒').length).toBeGreaterThan(0); // mancare + food
            expect(screen.getAllByText('🚗').length).toBeGreaterThan(0); // combustibil
            expect(screen.getAllByText('💊').length).toBeGreaterThan(0); // medicamente
        });
    });

    it('categoryEmoji: acoperă variantele alternative ale cuvintelor cheie (partea 2)', async () => {
        const altBatch2 = [
            { id: 24, amount: 5, description: 'e', expenseDate: today + 'T08:04:00', category: 'health',        person: null, location: null },
            { id: 25, amount: 5, description: 'f', expenseDate: today + 'T08:05:00', category: 'entertainment', person: null, location: null },
            { id: 26, amount: 5, description: 'g', expenseDate: today + 'T08:06:00', category: 'utilitati',     person: null, location: null },
        ];

        vi.mocked(apiModule.api.get).mockResolvedValue({ data: altBatch2 });
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: generateMockToken({ role: 'Parent' }) })
        );
        render(<MemoryRouter><Dashboard /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getAllByText('💊').length).toBeGreaterThan(0); // health
            expect(screen.getAllByText('🎮').length).toBeGreaterThan(0); // entertainment
            expect(screen.getAllByText('📄').length).toBeGreaterThan(0); // utilitati
        });
    });
});