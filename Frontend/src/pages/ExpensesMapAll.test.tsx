import '@testing-library/jest-dom';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ExpensesMapAll from './ExpensesMapAll';
import { fetchExpenses } from '../services/expenses';

// ── Mock Google Maps ───────────────────────────────────────────────────────
// Capturăm callback-ul onPolygonComplete prin global, astfel încât testele
// să-l poată apela direct cu un fake polygon controlabil.
let lastPolygonCompleteCb: ((poly: any) => void) | null = null;
// Track-uim toate listener-urile create de cod (.addListener) ca să verificăm
// că `.remove()` este chemat pe ele când se face cleanup.
const trackedListenerRemoves: ReturnType<typeof vi.fn>[] = [];
// Track-uim toate setMap-urile ca să verificăm cleanup-ul de poligon.
const trackedSetMaps: ReturnType<typeof vi.fn>[] = [];

const buildFakePoly = () => {
  const setMap = vi.fn();
  trackedSetMaps.push(setMap);
  return {
    setEditable: vi.fn(),
    setMap,
    getPath: () => ({
      getArray: () => [
        { lat: () => 44.4, lng: () => 26.1 },
        { lat: () => 44.5, lng: () => 26.2 },
      ],
      addListener: (_: string, _cb: any) => {
        const removeFn = vi.fn();
        trackedListenerRemoves.push(removeFn);
        return { remove: removeFn };
      },
    }),
    __listeners: null,
  };
};

vi.mock('@react-google-maps/api', () => ({
  useJsApiLoader: () => ({ isLoaded: true, loadError: undefined }),
  GoogleMap: ({ children }: any) => <div data-testid="google-map">{children}</div>,
  Marker: ({ onClick }: any) => <div data-testid="marker" onClick={onClick} />,
  DrawingManager: ({ onPolygonComplete }: any) => {
    lastPolygonCompleteCb = onPolygonComplete;
    return (
        <div data-testid="drawing-manager" onClick={() => {
          if (onPolygonComplete) {
            onPolygonComplete(buildFakePoly());
          }
        }} />
    );
  },
  MarkerClusterer: ({ children }: any) => (
      <div data-testid="marker-clusterer">
        {typeof children === 'function' ? children({}) : children}
      </div>
  ),
}));

vi.mock('../services/lookups', () => ({
  fetchCategoryNames: vi.fn().mockResolvedValue(['Food', 'Fun']),
  fetchUserNames: vi.fn().mockResolvedValue(['Alice', 'Bob']),
}));

// Mock-uim și serviciul de expenses ca să putem controla ramura "remote fetch"
vi.mock('../services/expenses', () => ({
  fetchExpenses: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

// ── Date mock ─────────────────────────────────────────────────────────────
const mockExpenses = [
  { id: 1, lat: 44.4, lng: 26.1, amount: 100, category: 'Food', person: 'Alice', description: 'Pizza', date: '2026-04-20' },
  { id: 2, lat: 44.41, lng: 26.11, amount: 50, category: 'Fun', person: 'Bob', description: 'Cinema', date: '2026-04-21' },
];

const renderComponent = async (state: any = { expenses: mockExpenses, filters: {} }) => {
  render(
      <MemoryRouter initialEntries={[{ pathname: '/all-map', state }]}>
        <Routes>
          <Route path="/all-map" element={<ExpensesMapAll />} />
        </Routes>
      </MemoryRouter>
  );
  await act(async () => { await new Promise(r => setTimeout(r, 0)); });
};

// Render fără state.expenses → forțează ramura remote (fetchExpenses)
const renderRemote = async () => {
  render(
      <MemoryRouter initialEntries={['/all-map']}>
        <Routes>
          <Route path="/all-map" element={<ExpensesMapAll />} />
        </Routes>
      </MemoryRouter>
  );
  await act(async () => { await new Promise(r => setTimeout(r, 0)); });
};

describe('ExpensesMapAll', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', 'fake-api-key');
    lastPolygonCompleteCb = null;
    trackedListenerRemoves.length = 0;
    trackedSetMaps.length = 0;
    // valoarea default a fetchExpenses (suprascrisă în testele care vor altceva)
    (fetchExpenses as any).mockResolvedValue([]);
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
    // ștergem orice google global setat de teste
    delete (window as any).google;
  });

  // ── 1. Render de bază ────────────────────────────────────────────────
  it('1. renders map and markers', async () => {
    await renderComponent();
    expect(screen.getByText('Toate Cheltuielile pe Hartă')).toBeInTheDocument();
    expect(screen.getByTestId('google-map')).toBeInTheDocument();
    expect(screen.getAllByTestId('marker')).toHaveLength(2);
  });

  // ── 2. Reset filtre ──────────────────────────────────────────────────
  it('2. shows filter controls and resets filters', async () => {
    await renderComponent();
    expect(screen.getByText('Resetează Filtre')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Resetează Filtre'));
    expect(screen.getByTestId('google-map')).toBeInTheDocument();
  });

  // ── 3. Filtrare după categorie și persoană ───────────────────────────
  it('3. filters by category and person', async () => {
    await renderComponent();
    const combos = screen.getAllByRole('combobox');
    fireEvent.change(combos[0], { target: { value: 'Food' } });
    fireEvent.change(combos[1], { target: { value: 'Alice' } });
    await waitFor(() => expect(screen.getAllByTestId('marker')).toHaveLength(1));
  });

  // ── 4. Filtrare după dată ────────────────────────────────────────────
  it('4. shows date filters and can set them', async () => {
    await renderComponent();
    expect(screen.getByTitle('Data de început')).toBeInTheDocument();
    expect(screen.getByTitle('Data de final')).toBeInTheDocument();
    fireEvent.change(screen.getByTitle('Data de început'), { target: { value: '2026-04-20' } });
    fireEvent.change(screen.getByTitle('Data de final'), { target: { value: '2026-04-21' } });
    expect(screen.getByTestId('google-map')).toBeInTheDocument();
  });

  // ── 5. Filtrare după startDate exclude cheltuieli ────────────────────
  it('5. filters out expenses before startDate', async () => {
    await renderComponent();
    fireEvent.change(screen.getByTitle('Data de început'), { target: { value: '2026-04-21' } });
    await waitFor(() => expect(screen.getAllByTestId('marker')).toHaveLength(1));
  });

  // ── 6. Lista goală ───────────────────────────────────────────────────
  it('6. renders with no expenses', async () => {
    await renderComponent({ expenses: [], filters: {} });
    expect(screen.getByText('Toate Cheltuielile pe Hartă')).toBeInTheDocument();
    expect(screen.queryAllByTestId('marker')).toHaveLength(0);
  });

  // ── 7. Fără state în location ────────────────────────────────────────
  it('7. renders with no location state', async () => {
    await renderRemote();
    expect(screen.getByText('Toate Cheltuielile pe Hartă')).toBeInTheDocument();
  });

  // ── 8. Butonul back navighează înapoi ────────────────────────────────
  it('8. back button navigates to previous page', async () => {
    await renderComponent();
    const backBtn = screen.getByRole('button', { name: /înapoi/i });
    fireEvent.click(backBtn);
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  // ── 9. Click pe marker afișează panoul lateral ───────────────────────
  it('9. clicking a marker shows the side panel with location info', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({}),
    }) as any;

    await renderComponent();
    const markers = screen.getAllByTestId('marker');
    await act(async () => { fireEvent.click(markers[0]); });

    await waitFor(() => {
      expect(screen.getByText('Locație')).toBeInTheDocument();
    });
  });

  // ── 10. Click pe marker + Places API răspunde cu date ────────────────
  it('10. clicking marker fetches place details from Places API', async () => {
    global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({
            places: [{ id: 'place123', googleMapsUri: 'https://maps.google.com/place123' }],
          }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({
            displayName: { text: 'Pizza Roma' },
            formattedAddress: 'Str. Exemplu 1, București',
            internationalPhoneNumber: '+40 21 000 0000',
            websiteUri: 'https://pizzaroma.ro',
            googleMapsUri: 'https://maps.google.com/place123',
            rating: 4.5,
            userRatingCount: 120,
            regularOpeningHours: { openNow: true, weekdayDescriptions: ['Luni: 10-22', 'Marți: 10-22'] },
            photos: [{ name: 'photos/photo1' }],
          }),
        }) as any;

    await renderComponent();
    const markers = screen.getAllByTestId('marker');
    await act(async () => { fireEvent.click(markers[0]); });

    await waitFor(() => {
      expect(screen.getByText('Pizza Roma')).toBeInTheDocument();
    });

    expect(screen.getByText('Str. Exemplu 1, București')).toBeInTheDocument();
    expect(screen.getByText('+40 21 000 0000')).toBeInTheDocument();
    expect(screen.getByText('Deschis')).toBeInTheDocument();
    expect(screen.getByText('Luni: 10-22')).toBeInTheDocument();
  });

  // ── 11. Click marker + website link vizibil ──────────────────────────
  it('11. shows website and google maps links after marker click', async () => {
    global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ places: [{ id: 'place123', googleMapsUri: 'https://maps.google.com/place123' }] }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({
            displayName: { text: 'Test Place' },
            websiteUri: 'https://testplace.ro',
            googleMapsUri: 'https://maps.google.com/place123',
          }),
        }) as any;

    await renderComponent();
    await act(async () => { fireEvent.click(screen.getAllByTestId('marker')[0]); });

    await waitFor(() => {
      expect(screen.getByText('Website')).toBeInTheDocument();
      expect(screen.getByText('Deschide în Google Maps')).toBeInTheDocument();
    });
  });

  // ── 12. Filtre inițiale din state ────────────────────────────────────
  it('12. initializes filters from location state', async () => {
    await renderComponent({
      expenses: mockExpenses,
      filters: { selectedCategory: 'Food', selectedPerson: 'Alice' },
    });
    const combos = screen.getAllByRole('combobox');
    expect(combos[0]).toHaveValue('Food');
    expect(combos[1]).toHaveValue('Alice');
    await waitFor(() => expect(screen.getAllByTestId('marker')).toHaveLength(1));
  });

  // ── 13. Reset curăță toate câmpurile ────────────────────────────────
  it('13. reset clears all filter fields', async () => {
    await renderComponent();
    const combos = screen.getAllByRole('combobox');
    fireEvent.change(combos[0], { target: { value: 'Food' } });
    fireEvent.change(combos[1], { target: { value: 'Alice' } });
    fireEvent.change(screen.getByTitle('Data de început'), { target: { value: '2026-04-20' } });
    fireEvent.change(screen.getByTitle('Data de final'), { target: { value: '2026-04-21' } });
    fireEvent.click(screen.getByText('Resetează Filtre'));
    expect(combos[0]).toHaveValue('');
    expect(combos[1]).toHaveValue('');
    expect(screen.getByTitle('Data de început')).toHaveValue('');
    expect(screen.getByTitle('Data de final')).toHaveValue('');
  });

  // ── 14. Expenses fără coordonate nu apar pe hartă ────────────────────
  it('14. expenses without coordinates are not rendered as markers', async () => {
    const expensesNoCoords = [
      { id: 3, amount: 30, category: 'Food', person: 'Alice', description: 'Fara coords', date: '2026-04-22' },
    ];
    await renderComponent({ expenses: expensesNoCoords, filters: {} });
    expect(screen.queryAllByTestId('marker')).toHaveLength(0);
  });

  // ── 15. Footer text vizibil ──────────────────────────────────────────
  it('15. shows footer info text', async () => {
    await renderComponent();
    expect(screen.getByText(/Sunt afișate doar cheltuielile cu coordonate/i)).toBeInTheDocument();
  });

  // ── 16. Toggle drawing mode ──────────────────────────────────────────
  it('16. toggles drawing mode on Deseneaza button click', async () => {
    await renderComponent();
    const drawBtn = screen.getByTitle('Desenează poligon');
    fireEvent.click(drawBtn);
    expect(drawBtn).toBeInTheDocument();
  });

  // ── 17. Sterge poligon ───────────────────────────────────────────────
  it('17. sterge poligon button is present and clickable', async () => {
    await renderComponent();
    const stergeBtn = screen.getByTitle('Șterge poligon');
    fireEvent.click(stergeBtn);
    expect(stergeBtn).toBeInTheDocument();
  });

  // ── 18. DrawingManager onPolygonComplete ────────────────────────────
  it('18. completes polygon drawing via DrawingManager', async () => {
    await renderComponent();
    const drawingManager = screen.getByTestId('drawing-manager');
    await act(async () => { fireEvent.click(drawingManager); });
    expect(screen.getByTestId('google-map')).toBeInTheDocument();
  });

  // ── 19. geocodeAddress — expenses fără coords, cu API key ────────────
  it('19. geocodes expenses without coordinates when API key exists', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        results: [{ geometry: { location: { lat: 44.5, lng: 26.2 } } }],
      }),
    }) as any;

    const expenseNoCoords = [
      { id: 5, amount: 75, category: 'Food', person: 'Alice', description: 'Carrefour', location: 'Carrefour Băneasa', date: '2026-04-22' },
    ];

    await renderComponent({ expenses: expenseNoCoords, filters: {} });

    await waitFor(() => {
      expect(screen.getAllByTestId('marker')).toHaveLength(1);
    });
  });

  // ── 20. geocodeAddress — fetch eșuează ──────────────────────────────
  it('20. handles geocode fetch failure gracefully', async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error('Network error')) as any;

    const expenseNoCoords = [
      { id: 6, amount: 20, category: 'Fun', person: 'Bob', description: 'Parc', date: '2026-04-23' },
    ];

    await renderComponent({ expenses: expenseNoCoords, filters: {} });
    expect(screen.queryAllByTestId('marker')).toHaveLength(0);
  });

  // ── 21. Places API — primul fetch eșuează ───────────────────────────
  it('21. handles Places API first fetch failure gracefully', async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: false, json: async () => ({}) }) as any;

    await renderComponent();
    await act(async () => { fireEvent.click(screen.getAllByTestId('marker')[0]); });

    await waitFor(() => {
      expect(screen.getByText('Locație')).toBeInTheDocument();
    });
  });

  // ── 22. Places API — al doilea fetch (detalii) eșuează ──────────────
  it('22. handles Places API details fetch failure gracefully', async () => {
    global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ places: [{ id: 'place123', googleMapsUri: null }] }),
        })
        .mockResolvedValueOnce({ ok: false, json: async () => ({}) }) as any;

    await renderComponent();
    await act(async () => { fireEvent.click(screen.getAllByTestId('marker')[0]); });

    await waitFor(() => {
      expect(screen.getByText('Locație')).toBeInTheDocument();
    });
  });

  // ── 23. Marker cu place închis ───────────────────────────────────────
  it('23. shows Inchis when place is closed', async () => {
    global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ places: [{ id: 'place456', googleMapsUri: 'https://maps.google.com/p456' }] }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({
            displayName: { text: 'Loc Închis' },
            regularOpeningHours: { openNow: false, weekdayDescriptions: [] },
          }),
        }) as any;

    await renderComponent();
    await act(async () => { fireEvent.click(screen.getAllByTestId('marker')[0]); });

    await waitFor(() => {
      expect(screen.getByText('Închis')).toBeInTheDocument();
    });
  });

  // ── 24. Filtrare după endDate ────────────────────────────────────────
  it('24. filters out expenses after endDate', async () => {
    await renderComponent();
    fireEvent.change(screen.getByTitle('Data de final'), { target: { value: '2026-04-20' } });
    await waitFor(() => expect(screen.getAllByTestId('marker')).toHaveLength(1));
  });

  // ── 25. Filtrare Food + Bob = 0 rezultate ───────────────────────────
  it('25. no markers when category and person combination has no match', async () => {
    await renderComponent();
    const combos = screen.getAllByRole('combobox');
    fireEvent.change(combos[0], { target: { value: 'Food' } });
    fireEvent.change(combos[1], { target: { value: 'Bob' } });
    await waitFor(() => expect(screen.queryAllByTestId('marker')).toHaveLength(0));
  });

  // ─────────────────────────────────────────────────────────────────────
  // ── TESTE NOI pentru a urca coverage-ul de la 89% la >95% ────────────
  // ─────────────────────────────────────────────────────────────────────

  // ── 26. Remote fetchExpenses cu succes — acoperă mapApiExpenseToMapExpense (linii 38-56) ──
  it('26. remote fetchExpenses success path maps API DTO via mapApiExpenseToMapExpense', async () => {
    // Răspuns ce forțează folosirea TUTUROR ramurilor de fallback
    // (location null, amount string, expenseDate gol etc.)
    (fetchExpenses as any).mockResolvedValueOnce([
      // expense „bogat" — toate câmpurile prezente
      {
        id: 100,
        amount: '199.50',
        currency: 'RON',
        description: 'Cumpărături supermarket',
        expenseDate: '2026-05-01T12:00:00Z',
        category: 'Mâncare',
        person: 'Ana',
        location: { id: 1, store: 'Mega Image', address: 'Bd. Unirii 1', city: 'București', country: 'RO', lat: 44.43, lng: 26.10 },
      },
      // expense „sărac" — câmpuri lipsă/null pentru a acoperi nullish coalescing-urile
      {
        id: 101,
        amount: 'invalid-number',  // forțează ramura amountNumber non-finite → 0
        currency: null,
        description: null,         // → ''
        expenseDate: null,         // datePart === ''
        category: null,            // → 'Fără categorie'
        person: null,              // → 'N/A'
        location: null,            // → 'Fără locație'
      },
    ]);

    await renderRemote();

    // Avem un marker (cel cu coordonate)
    await waitFor(() => {
      expect(screen.getAllByTestId('marker')).toHaveLength(1);
    });
  });

  // ── 27. Remote fetchExpenses ESUEAZĂ → setează expenseLoadError (linii 117-119) ──
  it('27. remote fetchExpenses failure shows error banner', async () => {
    (fetchExpenses as any).mockRejectedValueOnce(new Error('API down'));

    await renderRemote();

    await waitFor(() => {
      expect(screen.getByText(/Nu am putut încărca cheltuielile/i)).toBeInTheDocument();
    });
  });

  // ── 28. placePhotoSrc returnează null când place.photoName lipsește (linia 172) ──
  // Notă: branch-ul `!mapsApiKey` din linia 171 e practic unreachable —
  // dacă mapsApiKey lipsește, nu se randează harta deloc (linia 343),
  // deci nu există markere de apăsat → IIFE-ul nu mai contează. Acoperim
  // prin urmare doar branch-ul reachable: place.photoName lipsește.
  it('28. no place image rendered when place has no photoName', async () => {
    global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ places: [{ id: 'p-no-photo', googleMapsUri: null }] }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({
            displayName: { text: 'Loc fără poză' },
            // fără photos
          }),
        }) as any;

    await renderComponent();
    await act(async () => { fireEvent.click(screen.getAllByTestId('marker')[0]); });

    await waitFor(() => {
      expect(screen.getByText('Loc fără poză')).toBeInTheDocument();
    });
    const imgs = document.querySelectorAll('img');
    const placeImg = Array.from(imgs).find(img => img.src.includes('places.googleapis.com/v1/'));
    expect(placeImg).toBeUndefined();
  });

  // ── 29. Click pe marker fără id în răspunsul Places (linia 455) ──
  it('29. handles Places API response with no place id', async () => {
    global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ places: [{}] }),  // place fără id
        }) as any;

    await renderComponent();
    await act(async () => { fireEvent.click(screen.getAllByTestId('marker')[0]); });

    await waitFor(() => {
      expect(screen.getByText('Locație')).toBeInTheDocument();
    });
  });

  // ── 30. label-ul markerului este gol când amount=0/undefined (linia 432, branch 1) ──
  it('30. marker label is empty when amount is 0/undefined', async () => {
    const expensesZeroAmount = [
      { id: 50, lat: 44.4, lng: 26.1, amount: 0, category: 'Food', person: 'Alice', description: 'Gratis', date: '2026-04-20' },
    ];
    await renderComponent({ expenses: expensesZeroAmount, filters: {} });
    // Markerul există
    expect(screen.getByTestId('marker')).toBeInTheDocument();
    // Nu apare textul "0 RON" în DOM (e gol prin operatorul ternar)
    expect(screen.queryByText(/0 RON/)).not.toBeInTheDocument();
  });

  // ── 31. geocodeAddress — niciun rezultat în răspuns (linia 15: return null) ──
  it('31. geocodeAddress returns null when no geocoding results', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ results: [] }),
    }) as any;

    const expenseNoCoords = [
      { id: 70, amount: 10, category: 'Food', person: 'Alice', description: 'Cafea', location: 'Adresa inexistentă', date: '2026-04-25' },
    ];

    await renderComponent({ expenses: expenseNoCoords, filters: {} });
    // după geocoding eșuat, expense-ul rămâne fără coords → 0 markers
    await act(async () => { await new Promise(r => setTimeout(r, 0)); });
    expect(screen.queryAllByTestId('marker')).toHaveLength(0);
  });

  // ── 32. geocodeAddress — fetch returnează ok:false (linia 9) ──
  it('32. geocodeAddress returns null when fetch response is not ok', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({}),
    }) as any;

    const expenseNoCoords = [
      { id: 71, amount: 10, category: 'Food', person: 'Alice', description: 'Test', location: 'Server eroare', date: '2026-04-25' },
    ];

    await renderComponent({ expenses: expenseNoCoords, filters: {} });
    await act(async () => { await new Promise(r => setTimeout(r, 0)); });
    expect(screen.queryAllByTestId('marker')).toHaveLength(0);
  });

  // ── 33. geocodeAddress — adresă goală (linia 5: return null prin guard) ──
  it('33. geocodeAddress returns null when address is empty', async () => {
    const fetchSpy = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ results: [{ geometry: { location: { lat: 1, lng: 1 } } }] }),
    });
    global.fetch = fetchSpy as any;

    // expense fără niciun câmp utilizabil pentru adresă → addr === ''
    const expenseEmptyAddr = [
      { id: 72, amount: 5, category: 'Food', person: 'Alice', date: '2026-04-25' },
    ];

    await renderComponent({ expenses: expenseEmptyAddr, filters: {} });
    await act(async () => { await new Promise(r => setTimeout(r, 0)); });

    // niciun fetch de geocoding nu trebuie făcut (guard în prima linie)
    const geoCalls = fetchSpy.mock.calls.filter(([url]: any[]) =>
        typeof url === 'string' && url.includes('maps.googleapis.com/maps/api/geocode')
    );
    expect(geoCalls).toHaveLength(0);
  });

  // ── 34. Sterge poligon DUPĂ ce a fost desenat (linii 316-319 — branch 0) ──
  it('34. clicking Sterge button after drawing a polygon clears it', async () => {
    await renderComponent();

    // 1) Pasul 1: desenăm un poligon
    const drawingManager = screen.getByTestId('drawing-manager');
    await act(async () => { fireEvent.click(drawingManager); });

    // 2) Pasul 2: apăsăm Sterge → branch-ul `if (polygonRef.current)` este TRUE
    const stergeBtn = screen.getByTitle('Șterge poligon');
    await act(async () => { fireEvent.click(stergeBtn); });

    expect(screen.getByTestId('google-map')).toBeInTheDocument();
  });

  // ── 36. onPolygonComplete când există DEJA un poligon (acoperă liniile 406-408) ──
  it('36. drawing a second polygon cleans up the first one', async () => {
    await renderComponent();

    const drawingManager = screen.getByTestId('drawing-manager');

    // 1) Primul polygon-complete: polygonRef.current devine setat,
    //    iar codul atașează 3 listeneri (insert_at, set_at, remove_at).
    await act(async () => { fireEvent.click(drawingManager); });

    const removesAfterFirst = trackedListenerRemoves.length;
    const setMapsAfterFirst = trackedSetMaps.length;
    expect(removesAfterFirst).toBe(3);   // insert_at + set_at + remove_at
    expect(setMapsAfterFirst).toBe(1);

    // 2) Al doilea polygon-complete: codul intră pe ramura `if (polygonRef.current)`,
    //    detașează listenerele primului poly și apelează poly.setMap(null) pe el.
    await act(async () => { fireEvent.click(drawingManager); });

    // Cele 3 listenere ale PRIMULUI poly trebuie să fi fost cleanup-uite
    const firstThreeRemoves = trackedListenerRemoves.slice(0, 3);
    firstThreeRemoves.forEach(rm => expect(rm).toHaveBeenCalled());
    // Primul setMap trebuie să fi fost chemat cu null (linia 408)
    expect(trackedSetMaps[0]).toHaveBeenCalledWith(null);

    expect(screen.getByTestId('google-map')).toBeInTheDocument();
  });

  // ── 37. Sterge poligon DUPĂ desenare (acoperă liniile 316-319) ──
  it('37. clicking Sterge after drawing cleans up listeners and detaches polygon', async () => {
    await renderComponent();

    // 1) Desenăm un poligon
    const drawingManager = screen.getByTestId('drawing-manager');
    await act(async () => { fireEvent.click(drawingManager); });

    expect(trackedListenerRemoves).toHaveLength(3);
    expect(trackedSetMaps).toHaveLength(1);

    // 2) Click pe Sterge → intră pe `if (polygonRef.current)` și pe
    //    sub-branch-ul `if (polygonRef.current.__listeners)`
    const stergeBtn = screen.getByTitle('Șterge poligon');
    await act(async () => { fireEvent.click(stergeBtn); });

    // Cele 3 listenere ale poly-ului desenat trebuie să fi fost cleanup-uite
    trackedListenerRemoves.forEach(rm => expect(rm).toHaveBeenCalled());
    // Și setMap a fost chemat cu null
    expect(trackedSetMaps[0]).toHaveBeenCalledWith(null);
  });

  // ── 38. isPointInPolygon — cu google.maps.geometry mock-uit (liniile 208-211) ──
  it('38. isPointInPolygon uses Google geometry when polygon is drawn', async () => {
    // Mock-uim window.google ÎNAINTE de render
    const containsLocationSpy = vi.fn().mockReturnValue(true);
    (window as any).google = {
      maps: {
        LatLng: function (lat: number, lng: number) { return { lat, lng }; },
        geometry: {
          poly: { containsLocation: containsLocationSpy },
        },
        drawing: { OverlayType: { POLYGON: 'polygon' } },
      },
    };

    await renderComponent();

    // Desenăm un poligon — polygonRef.current devine non-null
    const drawingManager = screen.getByTestId('drawing-manager');
    await act(async () => { fireEvent.click(drawingManager); });

    // Acum modificăm filtrele pentru a re-evalua filteredExpenses → isPointInPolygon e chemat
    fireEvent.change(screen.getByTitle('Data de început'), { target: { value: '2026-04-19' } });

    await waitFor(() => {
      expect(containsLocationSpy).toHaveBeenCalled();
    });
  });

  // ── 39. isPointInPolygon — catch când google.maps aruncă (linia 210-211) ──
  it('39. isPointInPolygon catch branch returns true when google API throws', async () => {
    // google.maps prezent dar containsLocation aruncă
    (window as any).google = {
      maps: {
        LatLng: function () { throw new Error('LatLng boom'); },
        geometry: { poly: { containsLocation: () => true } },
        drawing: { OverlayType: { POLYGON: 'polygon' } },
      },
    };

    await renderComponent();

    const drawingManager = screen.getByTestId('drawing-manager');
    await act(async () => { fireEvent.click(drawingManager); });

    // Schimbăm filtru → re-evaluează filtered, isPointInPolygon aruncă → catch → true
    // → markerele rămân vizibile
    fireEvent.change(screen.getByTitle('Data de început'), { target: { value: '2026-04-19' } });

    await waitFor(() => {
      // ambele markere rămân (nu sunt excluse)
      expect(screen.getAllByTestId('marker').length).toBeGreaterThan(0);
    });
  });

  // ── 40. Panou lateral cu expense MINIMAL (acoperă fallback-uri linii 351, 356, 358, 431) ──
  it('40. side panel falls back to defaults when expense has minimal data', async () => {
    // expense cu description gol, location gol → label='', title=''
    // place rămâne null (fetch eșuează) → 'Locație' (fallback final L356)
    global.fetch = vi.fn().mockResolvedValue({ ok: false, json: async () => ({}) }) as any;

    const minimalExpense = [
      { id: 999, lat: 44.4, lng: 26.1, amount: 25, category: 'Food', person: 'Alice', description: '', location: '', date: '2026-04-20' },
    ];
    await renderComponent({ expenses: minimalExpense, filters: {} });

    await act(async () => { fireEvent.click(screen.getAllByTestId('marker')[0]); });

    // Panou lateral — fallback-ul final 'Locație' este afișat
    await waitFor(() => {
      const headings = screen.getAllByText('Locație');
      // Există atât eticheta de secțiune, cât și fallback-ul de nume → ≥ 2
      expect(headings.length).toBeGreaterThanOrEqual(2);
    });
  });

  // ── 41. Panou lateral cu place RATING fără userRatingCount (acoperă L363/L364 ramurile null) ──
  it('41. shows only rating when userRatingCount is missing (and vice versa)', async () => {
    global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ places: [{ id: 'pX', googleMapsUri: null }] }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({
            displayName: { text: 'Loc cu rating' },
            rating: 4.2,
            // FĂRĂ userRatingCount → ramura null pentru L364
          }),
        }) as any;

    await renderComponent();
    await act(async () => { fireEvent.click(screen.getAllByTestId('marker')[0]); });

    // Doar Rating (fără paranteza cu count)
    await waitFor(() => {
      expect(screen.getByText(/Rating: 4\.2/)).toBeInTheDocument();
    });
  });

  // ── 42. Panou lateral cu userRatingCount fără rating (cealaltă ramură L363) ──
  it('42. shows only userRatingCount when rating is missing', async () => {
    global.fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ places: [{ id: 'pY', googleMapsUri: null }] }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({
            displayName: { text: 'Loc cu count' },
            // FĂRĂ rating
            userRatingCount: 999,
          }),
        }) as any;

    await renderComponent();
    await act(async () => { fireEvent.click(screen.getAllByTestId('marker')[0]); });

    await waitFor(() => {
      expect(screen.getByText(/\(999\)/)).toBeInTheDocument();
    });
  });

  // ── 43. mapApiExpenseToMapExpense cu amount NUMERIC (linia 44 ramura true) ──
  it('43. remote fetchExpenses with numeric amount uses it directly', async () => {
    (fetchExpenses as any).mockResolvedValueOnce([
      {
        id: 200,
        amount: 42,  // ESTE number → ramura `typeof === 'number'` true (linia 44)
        currency: 'RON',
        description: 'Test',
        expenseDate: '2026-05-02T00:00:00Z',
        category: 'Food',
        person: 'Ana',
        location: { id: 1, store: 'X', address: null, city: null, country: null, lat: 44.5, lng: 26.5 },
      },
    ]);

    await renderRemote();

    await waitFor(() => {
      expect(screen.getAllByTestId('marker')).toHaveLength(1);
    });
  });

  // ── 44. drawingEnabled=true și window.google prezent → drawingMode = POLYGON (L403 ramura TRUE) ──
  it('44. drawing mode resolves to POLYGON when google.maps is available', async () => {
    (window as any).google = {
      maps: {
        LatLng: function (lat: number, lng: number) { return { lat, lng }; },
        geometry: { poly: { containsLocation: () => true } },
        drawing: { OverlayType: { POLYGON: 'polygon-overlay' } },
      },
    };

    await renderComponent();

    // Activăm drawing mode → la următorul render, codul evaluează
    // `drawingEnabled && window.google ? ... : null` → ramura TRUE.
    const drawBtn = screen.getByTitle('Desenează poligon');
    await act(async () => { fireEvent.click(drawBtn); });

    expect(screen.getByTestId('google-map')).toBeInTheDocument();
  });
});