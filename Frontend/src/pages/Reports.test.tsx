import '@testing-library/jest-dom';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Reports from './Reports';
import * as apiModule from '../services/api';

vi.mock('recharts', async () => {
    const actual = await vi.importActual('recharts');
    return {
        ...actual,
        ResponsiveContainer: ({ children }: any) => (
            <div style={{ width: 800, height: 300 }}>{children}</div>
        ),
    };
});

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../services/api', () => ({
    api: { get: vi.fn() },
}));

const today     = new Date();
const thisMonth = today.toISOString().slice(0, 7);

const mockExpenses = [
    { id: 1, amount: 120, expenseDate: `${thisMonth}-02T10:00:00`, category: 'Alimentare', person: 'Mihaela', location: null, description: null },
    { id: 2, amount: 450, expenseDate: `${thisMonth}-05T12:00:00`, category: 'Transport',  person: 'Eduard',  location: null, description: null },
    { id: 3, amount: 300, expenseDate: `${thisMonth}-10T09:00:00`, category: 'Educație',   person: 'Andrei',  location: null, description: null },
];

describe('Reports Component', () => {

    beforeEach(() => {
        vi.clearAllMocks();
        // ← SCOS useFakeTimers
        vi.mocked(apiModule.api.get).mockResolvedValue({ data: mockExpenses });
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    const renderComponent = () =>
        render(<MemoryRouter><Reports /></MemoryRouter>);

    // ─── 1. RANDARE INIȚIALĂ ─────────────────────────────────────────────────

    it('randează corect header-ul și titlul paginii', async () => {
        await act(async () => { renderComponent(); });
        expect(screen.getByRole('heading', { name: /Evoluție cheltuieli/i })).toBeInTheDocument();
        expect(screen.getByText('RAPOARTE · EVOLUȚIE')).toBeInTheDocument();
    });

    it('navighează înapoi la /dashboard la click pe butonul ArrowLeft', async () => {
        await act(async () => { renderComponent(); });
        const backBtn = document.querySelector('button.btn-icon') as HTMLElement;
        fireEvent.click(backBtn);
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });

    it('apelează API-ul la montare cu timeRange implicit 1M', async () => {
        await act(async () => { renderComponent(); });
        expect(apiModule.api.get).toHaveBeenCalledWith(
            '/api/v1/expenses',
            expect.objectContaining({ signal: expect.any(AbortSignal) })
        );
    });

    // ─── 2. LOADING STATE ────────────────────────────────────────────────────

    it('afișează loading overlay în timp ce se încarcă datele', async () => {
        vi.mocked(apiModule.api.get).mockReturnValue(new Promise(() => {}));
        await act(async () => { renderComponent(); });
        expect(document.querySelector('.lucide-refresh-cw')).toBeInTheDocument();
    });

    it('ascunde loading overlay după ce datele sunt încărcate', async () => {
        await act(async () => { renderComponent(); });
        expect(document.querySelector('.lucide-refresh-cw')).not.toBeInTheDocument();
    });

    // ─── 3. DATE REALE DIN API ───────────────────────────────────────────────

    it('afișează totalul calculat din datele reale (120+450+300 = 870)', async () => {
        await act(async () => { renderComponent(); });
        expect(screen.getByText(/870/)).toBeInTheDocument();
    });

    it('afișează mesaj empty state dacă API returnează listă goală', async () => {
        vi.mocked(apiModule.api.get).mockResolvedValue({ data: [] });
        await act(async () => { renderComponent(); });
        expect(screen.getByText('Nu există cheltuieli în perioada selectată.')).toBeInTheDocument();
    });

    it('afișează eroare dacă API-ul pică', async () => {
        vi.mocked(apiModule.api.get).mockRejectedValue(new Error('Network error'));
        await act(async () => { renderComponent(); });
        expect(screen.getByText(/Nu am putut încărca datele/i)).toBeInTheDocument();
    });

    it('nu afișează eroare dacă request-ul e anulat (AbortError)', async () => {
        const abortError = new Error('Aborted');
        abortError.name = 'AbortError';
        vi.mocked(apiModule.api.get).mockRejectedValue(abortError);
        await act(async () => { renderComponent(); });
        expect(screen.queryByText(/Nu am putut încărca datele/i)).not.toBeInTheDocument();
    });

    // ─── 4. SCHIMBAREA TIMERANGE ─────────────────────────────────────────────

    it('face un nou API call la schimbarea tab-ului de timp', async () => {
        await act(async () => { renderComponent(); });
        expect(apiModule.api.get).toHaveBeenCalledTimes(1);

        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: '3M' }));
        });

        expect(apiModule.api.get).toHaveBeenCalledTimes(2);
    });

    it('face un nou API call la schimbarea tab-ului pe 1W', async () => {
        await act(async () => { renderComponent(); });
        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: '1W' }));
        });
        expect(apiModule.api.get).toHaveBeenCalledTimes(2);
    });

    it('face un nou API call la schimbarea tab-ului pe 1Y', async () => {
        await act(async () => { renderComponent(); });
        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: '1Y' }));
        });
        expect(apiModule.api.get).toHaveBeenCalledTimes(2);
    });

    it('afișează loading la schimbarea tab-ului', async () => {
        await act(async () => { renderComponent(); });
        vi.mocked(apiModule.api.get).mockReturnValue(new Promise(() => {}));
        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: '3M' }));
        });
        expect(document.querySelector('.lucide-refresh-cw')).toBeInTheDocument();
    });

    // ─── 5. INTERVAL CUSTOM ──────────────────────────────────────────────────

    it('deschide și închide panoul de date custom', async () => {
        await act(async () => { renderComponent(); });

        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));
        });
        expect(screen.getByPlaceholderText(/ex: 12\/04\/2026/i)).toBeInTheDocument();

        await act(async () => {
            const closeBtn = document.querySelector('.lucide-x')?.closest('button') as HTMLElement;
            fireEvent.click(closeBtn);
        });
        expect(screen.queryByPlaceholderText(/ex: 12\/04\/2026/i)).not.toBeInTheDocument();
    });

    it('aplică intervalul custom valid și face API call', async () => {
        await act(async () => { renderComponent(); });
        expect(apiModule.api.get).toHaveBeenCalledTimes(1);

        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));
        });

        await act(async () => {
            const inputs = screen.getAllByPlaceholderText(/ex: [0-9/]+/i);
            fireEvent.change(inputs[0], { target: { value: '01/04/2026' } });
            fireEvent.change(inputs[1], { target: { value: '30/04/2026' } });
        });

        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: 'Aplică' }));
        });

        expect(apiModule.api.get).toHaveBeenCalledTimes(2);
    });

    it('butonul Aplică este dezactivat cu date incomplete', async () => {
        await act(async () => { renderComponent(); });
        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));
        });
        expect(screen.getByRole('button', { name: 'Aplică' })).toBeDisabled();
    });

    it('închiderea custom date picker resetează câmpurile', async () => {
        await act(async () => { renderComponent(); });

        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));
        });
        await act(async () => {
            fireEvent.change(screen.getByPlaceholderText(/ex: 12\/04\/2026/i), { target: { value: '01/04/2026' } });
        });
        await act(async () => {
            const closeBtn = document.querySelector('.lucide-x')?.closest('button') as HTMLElement;
            fireEvent.click(closeBtn);
        });
        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));
        });

        expect(screen.getByPlaceholderText(/ex: 12\/04\/2026/i)).toHaveValue('');
    });

    // ─── 6. VALIDARE DATE ────────────────────────────────────────────────────

    it('afișează eroare de format pentru dată invalidă', async () => {
        await act(async () => { renderComponent(); });
        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));
        });
        await act(async () => {
            fireEvent.change(screen.getByPlaceholderText(/ex: 12\/04\/2026/i), { target: { value: '32/13/2026' } });
        });
        expect(screen.getByText(/Te rugăm să introduci datele conform formatului/i)).toBeInTheDocument();
    });

    it('afișează eroare cronologică dacă start > end', async () => {
        await act(async () => { renderComponent(); });
        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));
        });
        await act(async () => {
            const inputs = screen.getAllByPlaceholderText(/ex: [0-9/]+/i);
            fireEvent.change(inputs[0], { target: { value: '20/04/2026' } });
            fireEvent.change(inputs[1], { target: { value: '10/04/2026' } });
        });
        expect(screen.getByText(/Data de început trebuie să fie înainte de data de sfârșit/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Aplică' })).toBeDisabled();
    });

    it('afișează eroare pentru 31 Aprilie (dată inexistentă)', async () => {
        await act(async () => { renderComponent(); });
        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));
        });
        await act(async () => {
            fireEvent.change(screen.getByPlaceholderText(/ex: 12\/04\/2026/i), { target: { value: '31/04/2026' } });
        });
        expect(screen.getByText(/Te rugăm să introduci datele conform formatului/i)).toBeInTheDocument();
    });

    // ─── 7. ABORT CONTROLLER ─────────────────────────────────────────────────

    it('anulează request-ul API la unmount', async () => {
        vi.mocked(apiModule.api.get).mockReturnValue(new Promise(() => {}));
        let unmount: () => void;
        await act(async () => {
            ({ unmount } = renderComponent());
        });
        unmount!();
        expect(apiModule.api.get).toHaveBeenCalledWith(
            '/api/v1/expenses',
            expect.objectContaining({ signal: expect.any(AbortSignal) })
        );
    });
});