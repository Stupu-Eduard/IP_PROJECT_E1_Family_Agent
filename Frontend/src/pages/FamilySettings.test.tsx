import '@testing-library/jest-dom';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import FamilySettings from './FamilySettings';
import { useAuthStore } from '../store/authStore';
import { decodeJwtPayload } from '../utils/jwt';

// ─── MOCKS ────────────────────────────────────────────────────────────

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../store/authStore', () => ({
    useAuthStore: vi.fn(),
}));

vi.mock('../utils/jwt', () => ({
    decodeJwtPayload: vi.fn(),
}));

// ─── SUITA DE TESTE ───────────────────────────────────────────────────

describe('FamilySettings Component', () => {
    let confirmSpy: any;

    beforeEach(() => {
        vi.clearAllMocks();
        vi.useFakeTimers();
        confirmSpy = vi.spyOn(window, 'confirm');
    });

    afterEach(() => {
        vi.useRealTimers();
        confirmSpy.mockRestore();
    });

    const renderComponent = () => render(<MemoryRouter><FamilySettings /></MemoryRouter>);

    // ─── 1. ROLURI ȘI RANDARE CONDIȚIONATĂ ───

    it('randează vizualizarea limitată pentru rolul Child (fără formular de invitare)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Child' }as any);

        renderComponent();

        expect(screen.getByText('Membrii Familiei Mele')).toBeInTheDocument();
        expect(screen.queryByText('Adaugă un membru nou')).not.toBeInTheDocument();
        expect(screen.queryByTitle('Elimină membru')).not.toBeInTheDocument();
    });

    it('randează vizualizarea completă pentru rolul Parent', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent' }as any);

        renderComponent();

        expect(screen.getByText('Gestionare Familie')).toBeInTheDocument();
        expect(screen.getByText('Adaugă un membru nou')).toBeInTheDocument();

        const deleteButtons = screen.getAllByTitle('Elimină membru');
        expect(deleteButtons.length).toBeGreaterThan(0);
    });

    it('randează vizualizarea completă pentru rolul Co-Parent', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Co-Parent' }as any);

        renderComponent();
        expect(screen.getByText('Gestionare Familie')).toBeInTheDocument();
    });

    it('funcționează corect dacă token-ul lipsește', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: null }));

        renderComponent();

        expect(screen.getByText('Membrii Familiei Mele')).toBeInTheDocument();
        expect(decodeJwtPayload).not.toHaveBeenCalled();
    });

    // ─── 2. INTERACȚIUNI ȘI FORMULAR INVITARE ───

    it('navighează spre dashboard la click pe back', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent' } as any);

        const { container } = renderComponent();

        const backBtn = container.querySelector('.btn-alive-secondary');
        if (backBtn) fireEvent.click(backBtn);

        expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });

    it('nu trimite invitația dacă emailul este gol sau conține doar spații (early return)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent' }as any);

        renderComponent();

        const emailInput = screen.getByPlaceholderText('email@familie.com');
        const submitBtn = screen.getByText('Trimite Invitație');

        fireEvent.change(emailInput, { target: { value: '   ' } });
        fireEvent.click(submitBtn);

        expect(screen.queryByText('Se trimite...')).not.toBeInTheDocument();
    });

    it('trimite invitația și adaugă un nou membru în listă după delay (setTimeout)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent' }as any);

        renderComponent();

        const emailInput = screen.getByPlaceholderText('email@familie.com');
        const roleSelect = screen.getByRole('combobox');
        const submitBtn = screen.getByText('Trimite Invitație');

        fireEvent.change(emailInput, { target: { value: 'test@familie.com' } });
        fireEvent.change(roleSelect, { target: { value: 'Co-Parent' } });

        fireEvent.click(submitBtn);

        expect(screen.getByText('Se trimite...')).toBeInTheDocument();

        act(() => {
            vi.advanceTimersByTime(1000);
        });

        expect(screen.getByText('Trimite Invitație')).toBeInTheDocument();

        expect(screen.getAllByText('test@familie.com').length).toBeGreaterThan(0);
        expect((emailInput as HTMLInputElement).value).toBe('');
    });

    // ─── 3. ȘTERGEREA MEMBRILOR (WINDOW.CONFIRM) ───

    it('șterge un membru cu nume valid dacă se acceptă confirmarea', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent' }as any);
        confirmSpy.mockReturnValue(true);

        renderComponent();

        // Folosim getAllByText pentru a evita conflictul dintre "Mihaela" și "mihaela@partner.com"
        expect(screen.getAllByText(/Mihaela/i).length).toBeGreaterThan(0);

        const deleteButtons = screen.getAllByTitle('Elimină membru');
        fireEvent.click(deleteButtons[0]);

        expect(confirmSpy).toHaveBeenCalledWith('Ești sigur că dorești să elimini membrul Mihaela ?');

        // După ștergere, lungimea array-ului returnat de queryAllByText ar trebui să fie 0
        expect(screen.queryAllByText(/Mihaela/i).length).toBe(0);
    });

    it('șterge un membru fără nume (invitat) dacă se acceptă confirmarea', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent' }as any);
        confirmSpy.mockReturnValue(true);

        renderComponent();

        expect(screen.getAllByText('invitat@exemplu.com').length).toBeGreaterThan(0);

        const deleteButtons = screen.getAllByTitle('Elimină membru');
        fireEvent.click(deleteButtons[2]);

        expect(confirmSpy).toHaveBeenCalledWith('Ești sigur că dorești să elimini membrul invitat?');

        expect(screen.queryAllByText('invitat@exemplu.com').length).toBe(0);
    });

    it('NU șterge membrul dacă se apasă Cancel pe confirmare', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent' }as any);
        confirmSpy.mockReturnValue(false); // Simulăm click pe "Cancel"

        renderComponent();

        const deleteButtons = screen.getAllByTitle('Elimină membru');
        fireEvent.click(deleteButtons[1]); // Dăm click pe delete la Andrei

        // Andrei (nume) și andrei@kid.com (email) ar trebui să fie în continuare în DOM
        expect(screen.getAllByText(/Andrei/i).length).toBeGreaterThan(0);
    });
});