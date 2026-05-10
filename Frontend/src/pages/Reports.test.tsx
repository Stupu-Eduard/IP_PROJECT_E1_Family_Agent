import '@testing-library/jest-dom'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import Reports from './Reports'

// ── 1. Mock Recharts ──
vi.mock('recharts', async () => {
    const actual = await vi.importActual('recharts');
    const MockResponsiveContainer = ({ children }: { children?: ReactNode }) => <div style={{ width: 800, height: 300 }}>{children}</div>;
    return {
        ...actual,
        ResponsiveContainer: MockResponsiveContainer,
    };
});

// ── 2. Mock Navigation ──
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

describe('Reports Component - 100% Coverage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    const renderComponent = () => {
        let result: ReturnType<typeof render>;
        act(() => {
            result = render(
                <MemoryRouter>
                    <Reports />
                </MemoryRouter>
            );
            // Consumăm loading-ul inițial din useEffect imediat după render
            vi.advanceTimersByTime(800);
        });
        return result!;
    };

    it('1. Verifică randarea inițială și navigarea înapoi', () => {
        renderComponent();

        expect(screen.getByRole('heading', { name: /Evoluție cheltuieli/i })).toBeInTheDocument();

        // Butonul ArrowLeft are aria-hidden pe SVG → îl găsim prin clasa CSS
        const backBtn = document.querySelector('button.btn-icon') as HTMLElement;
        expect(backBtn).not.toBeNull();
        fireEvent.click(backBtn);
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });

    it('2. Gestionează schimbarea tab-urilor de timp și starea de loading', () => {
        renderComponent();

        fireEvent.click(screen.getByRole('button', { name: '3M' }));

        // Imediat după click loading trebuie să fie vizibil
        expect(document.querySelector('.lucide-refresh-cw')).toBeInTheDocument();

        // Avansăm timers — loading dispare
        act(() => { vi.advanceTimersByTime(800); });

        expect(document.querySelector('.lucide-refresh-cw')).not.toBeInTheDocument();
    });

    it('3. Deschide și închide panoul de date custom', () => {
        renderComponent();

        fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));
        expect(screen.getByPlaceholderText(/ex: 12\/04\/2026/i)).toBeInTheDocument();

        // Butonul X — găsit prin SVG cu clasa lucide-x
        const closeBtn = document.querySelector('.lucide-x')?.closest('button') as HTMLElement;
        expect(closeBtn).not.toBeNull();
        fireEvent.click(closeBtn);

        expect(screen.queryByPlaceholderText(/ex: 12\/04\/2026/i)).not.toBeInTheDocument();
    });

    it('4. Validare: Format dată invalid (Regex)', () => {
        renderComponent();

        fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));

        const startInput = screen.getByPlaceholderText(/ex: 12\/04\/2026/i);
        fireEvent.change(startInput, { target: { value: '32/13/2026' } });

        // Validarea e sincronă (nu async) — starea se actualizează imediat
        expect(screen.getByText(/Te rugăm să introduci datele conform formatului/i)).toBeInTheDocument();
    });

    it('5. Validare: Eroare cronologică (Start > End)', () => {
        renderComponent();

        fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));

        const inputs = screen.getAllByPlaceholderText(/ex: [0-9/]+/i);
        fireEvent.change(inputs[0], { target: { value: '20/04/2026' } });
        fireEvent.change(inputs[1], { target: { value: '10/04/2026' } });

        expect(screen.getByText(/Data de început trebuie să fie înainte de data de sfârșit/i)).toBeInTheDocument();

        const applyBtn = screen.getByRole('button', { name: 'Aplică' });
        expect(applyBtn).toBeDisabled();
    });

    it('6. Aplică un interval valid (Success Path)', () => {
        renderComponent();

        fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));

        const inputs = screen.getAllByPlaceholderText(/ex: [0-9/]+/i);
        fireEvent.change(inputs[0], { target: { value: '01/04/2026' } });
        fireEvent.change(inputs[1], { target: { value: '10/04/2026' } });

        const applyBtn = screen.getByRole('button', { name: 'Aplică' });
        expect(applyBtn).not.toBeDisabled();

        fireEvent.click(applyBtn);

        // Loading apare imediat după click
        expect(document.querySelector('.lucide-refresh-cw')).toBeInTheDocument();

        // Loading dispare după 800ms
        act(() => { vi.advanceTimersByTime(800); });
        expect(document.querySelector('.lucide-refresh-cw')).not.toBeInTheDocument();
    });

    it('7. Verifică calculul sumei totale', () => {
        renderComponent();
        // 120+450+300+800+200+550+400 = 2820
        expect(screen.getByText('2820.00')).toBeInTheDocument();
    });

    it('8. Testează parseDate pentru date inexistente (ex: 31 Aprilie)', () => {
        renderComponent();

        fireEvent.click(screen.getByRole('button', { name: /Interval Custom/i }));

        const startInput = screen.getByPlaceholderText(/ex: 12\/04\/2026/i);
        // Aprilie are doar 30 zile → parsedDate.getDate() !== Number(day) → parseDate returnează null
        fireEvent.change(startInput, { target: { value: '31/04/2026' } });

        expect(screen.getByText(/Te rugăm să introduci datele conform formatului/i)).toBeInTheDocument();
    });
});