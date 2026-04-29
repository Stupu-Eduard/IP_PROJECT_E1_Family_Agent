import '@testing-library/jest-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ExpenseMap from './ExpenseMap';

// Mock Google Maps API Loader
vi.mock('@react-google-maps/api', () => ({
  useJsApiLoader: () => ({ isLoaded: true, loadError: undefined }),
  GoogleMap: ({ children }: any) => <div data-testid="google-map">{children}</div>,
  Marker: () => <div data-testid="marker" />,
  InfoWindow: ({ children }: any) => <div data-testid="info-window">{children}</div>,
}));

describe('ExpenseMap', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });


  // Now supports two date filters: startDate and endDate
  const renderComponent = (state = { locationLabel: 'Test Location', startDate: '', endDate: '' }) =>
    render(
      <MemoryRouter initialEntries={[{ pathname: '/map', state }]}> 
        <Routes>
          <Route path="/map" element={<ExpenseMap />} />
        </Routes>
      </MemoryRouter>
    );


  it('renders map and location label', () => {
    renderComponent();
    expect(screen.getByText('Hartă')).toBeInTheDocument();
    // Sidebar location label should only appear once
    expect(screen.getByTestId('sidebar-location-label')).toBeInTheDocument();
    expect(screen.getByTestId('google-map')).toBeInTheDocument();
  });

  it('shows message for non-geographic location', () => {
    renderComponent({ locationLabel: 'Online' });
    expect(screen.getByText(/non-geografică/i)).toBeInTheDocument();
  });
});
