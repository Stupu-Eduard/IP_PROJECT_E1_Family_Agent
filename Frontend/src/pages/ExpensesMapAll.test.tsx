import '@testing-library/jest-dom';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ExpensesMapAll from './ExpensesMapAll';

// Mock Google Maps API Loader and components
vi.mock('@react-google-maps/api', () => ({
  useJsApiLoader: () => ({ isLoaded: true, loadError: undefined }),
  GoogleMap: ({ children }: any) => <div data-testid="google-map">{children}</div>,
  Marker: () => <div data-testid="marker" />,
  DrawingManager: () => <div data-testid="drawing-manager" />,
  MarkerClusterer: ({ children }: any) => <div data-testid="marker-clusterer">{typeof children === 'function' ? children({}) : children}</div>,
}));

vi.mock('../services/lookups', () => ({
  fetchCategoryNames: vi.fn().mockResolvedValue(['Food', 'Fun']),
  fetchUserNames: vi.fn().mockResolvedValue(['Alice', 'Bob']),
}));

describe('ExpensesMapAll', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Injectăm o cheie falsă ca să nu apară mesajul "Nu s-a putut încărca Google Maps"
    vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', 'fake-api-key');
  });

  afterEach(() => {
    // Curățăm mediul de test la final
    vi.unstubAllEnvs();
  });

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

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 0));
    });
  };

  it('renders map and markers', async () => {
    await renderComponent();
    expect(screen.getByText('Toate Cheltuielile pe Hartă')).toBeInTheDocument();
    // Acum ar trebui să găsească harta pentru că are o cheie API falsă!
    expect(screen.getByTestId('google-map')).toBeInTheDocument();
    expect(screen.getAllByTestId('marker')).toHaveLength(2);
  });

  it('shows filter controls and resets filters', async () => {
    await renderComponent();
    expect(screen.getByText('Resetează Filtre')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Resetează Filtre'));
    expect(screen.getByTestId('google-map')).toBeInTheDocument();
  });

  it('filters by category and person', async () => {
    await renderComponent();
    const combos = screen.getAllByRole('combobox');
    fireEvent.change(combos[0], { target: { value: 'Food' } });
    fireEvent.change(combos[1], { target: { value: 'Alice' } });
    await waitFor(() => expect(screen.getAllByTestId('marker')).toHaveLength(1));
  });

  it('shows date filters and can set them', async () => {
    await renderComponent();
    expect(screen.getByTitle('Data de început')).toBeInTheDocument();
    expect(screen.getByTitle('Data de final')).toBeInTheDocument();
    fireEvent.change(screen.getByTitle('Data de început'), { target: { value: '2026-04-20' } });
    fireEvent.change(screen.getByTitle('Data de final'), { target: { value: '2026-04-21' } });
    expect(screen.getByTestId('google-map')).toBeInTheDocument();
  });
});