import '@testing-library/jest-dom';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ExpensesMapAll from './ExpensesMapAll';

// ── Mock Google Maps ───────────────────────────────────────────────────────
vi.mock('@react-google-maps/api', () => ({
  useJsApiLoader: () => ({ isLoaded: true, loadError: undefined }),
  GoogleMap: ({ children }: any) => <div data-testid="google-map">{children}</div>,
  Marker: ({ onClick }: any) => <div data-testid="marker" onClick={onClick} />,
  DrawingManager: ({ onPolygonComplete }: any) => (
      <div data-testid="drawing-manager" onClick={() => {
        if (onPolygonComplete) {
          // Simulăm un polygon complet
          const fakePoly = {
            setEditable: vi.fn(),
            setMap: vi.fn(),
            getPath: () => ({
              getArray: () => [
                { lat: () => 44.4, lng: () => 26.1 },
                { lat: () => 44.5, lng: () => 26.2 },
              ],
              addListener: (_: string, _cb: unknown) => ({ remove: vi.fn() }),
            }),
            __listeners: null,
          };
          onPolygonComplete(fakePoly);
        }
      }} />
  ),
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

describe('ExpensesMapAll', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', 'fake-api-key');
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
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
    render(
        <MemoryRouter initialEntries={['/all-map']}>
          <Routes><Route path="/all-map" element={<ExpensesMapAll />} /></Routes>
        </MemoryRouter>
    );
    await act(async () => { await new Promise(r => setTimeout(r, 0)); });
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
    globalThis.fetch = vi.fn().mockResolvedValue({
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
    globalThis.fetch = vi.fn()
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
    expect(screen.getByText('Rating: 4.5 (120)')).toBeInTheDocument();
    expect(screen.getByText('Deschis')).toBeInTheDocument();
    expect(screen.getByText('Luni: 10-22')).toBeInTheDocument();
  });

  // ── 11. Click marker + website link vizibil ──────────────────────────
  it('11. shows website and google maps links after marker click', async () => {
    globalThis.fetch = vi.fn()
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
    globalThis.fetch = vi.fn().mockResolvedValue({
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
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('Network error')) as any;

    const expenseNoCoords = [
      { id: 6, amount: 20, category: 'Fun', person: 'Bob', description: 'Parc', date: '2026-04-23' },
    ];

    await renderComponent({ expenses: expenseNoCoords, filters: {} });
    expect(screen.queryAllByTestId('marker')).toHaveLength(0);
  });

  // ── 21. Places API — primul fetch eșuează ───────────────────────────
  it('21. handles Places API first fetch failure gracefully', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: false, json: async () => ({}) }) as any;

    await renderComponent();
    await act(async () => { fireEvent.click(screen.getAllByTestId('marker')[0]); });

    await waitFor(() => {
      expect(screen.getByText('Locație')).toBeInTheDocument();
    });
  });

  // ── 22. Places API — al doilea fetch (detalii) eșuează ──────────────
  it('22. handles Places API details fetch failure gracefully', async () => {
    globalThis.fetch = vi.fn()
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
    globalThis.fetch = vi.fn()
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
});