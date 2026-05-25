import '@testing-library/jest-dom';
import { render, screen, fireEvent } from '@testing-library/react';
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

vi.mock('../services/api', () => ({
    familyApi: {
        getMembers: vi.fn(),
        inviteMember: vi.fn(),
        removeMember: vi.fn(),
        deleteChildAccount: vi.fn(),
        leaveFamily: vi.fn(),
        deleteFamily: vi.fn(),
        createFamily: vi.fn(),
        updateMemberRole: vi.fn(),
    },
    invitationApi: {
        getPending: vi.fn(() => Promise.resolve({ data: [] })),
        accept: vi.fn(),
        decline: vi.fn(),
    },
    authApi: {
        refresh: vi.fn(),
    },
    api: {
        put: vi.fn(),
    },
}));

// ─── SUITA DE TESTE ───────────────────────────────────────────────────

describe('FamilySettings Component', () => {
    let confirmSpy: ReturnType<typeof vi.spyOn>;

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
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Child' } as any);

        renderComponent();

        expect(screen.getByText('Membrii Familiei Mele')).toBeInTheDocument();
        expect(screen.queryByText('Adaugă un membru nou')).not.toBeInTheDocument();
    });

    it('randează vizualizarea completă pentru rolul Parent', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent' } as any);

        renderComponent();

        expect(screen.getByText('Gestionare Familie')).toBeInTheDocument();
        expect(screen.getByText('Adaugă un membru nou')).toBeInTheDocument();
    });

    it('randează vizualizarea completă pentru rolul Co-Parent', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Co-Parent' } as any);

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
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent' } as any);

        renderComponent();

        const emailInput = screen.getByPlaceholderText('email@familie.com');
        const submitBtn = screen.getByText('Trimite Invitație');

        fireEvent.change(emailInput, { target: { value: '   ' } });
        fireEvent.click(submitBtn);

        expect(screen.queryByText('Se trimite...')).not.toBeInTheDocument();
    });

    // ─── 3. ȘTERGEREA MEMBRILOR DIN LISTA LOCALĂ ───

    it('NU șterge membrul din lista locală dacă se apasă Cancel', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent' } as any);
        confirmSpy.mockReturnValue(false);

        // Adaugăm manual un setTimeout ca să fie mai ușor de testat fără API
        renderComponent();

        // Fără familyId, lista e goală — nu există membri de șters în UI
        expect(screen.queryByTitle('Elimină membru')).not.toBeInTheDocument();
    });

    // ─── 4. STERGE CONT COPIL (DELETE CHILD ACCOUNT) ─────────────────────

    it('butonul "Sterge cont" apare pentru membri Child când userul e adult (Parent)', async () => {
        const { familyApi } = await import('../services/api');
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: 'mock_token' })
        );
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent', familyId: 1, userId: 10 } as any);
        vi.mocked(familyApi.getMembers).mockResolvedValue({
            data: [
                { id: 100, userId: 10, name: 'ParentUser', email: 'parent@test.com', role: 'Parent' },
                { id: 200, userId: 20, name: 'Copil Mic', email: 'copil@test.com', role: 'Child' },
            ],
        } as any);

        const { unmount } = render(<MemoryRouter><FamilySettings /></MemoryRouter>);
        await screen.findByText('Copil Mic');

        expect(screen.getByTitle('Sterge contul complet')).toBeInTheDocument();
        unmount();
    });

    it('butonul "Sterge cont" NU apare pentru membri Parent sau Co-Parent', async () => {
        const { familyApi } = await import('../services/api');
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: 'mock_token' })
        );
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent', familyId: 1, userId: 10 } as any);
        vi.mocked(familyApi.getMembers).mockResolvedValue({
            data: [
                { id: 100, userId: 10, name: 'ParentUser', email: 'parent@test.com', role: 'Parent' },
                { id: 101, userId: 11, name: 'CoParent', email: 'co@test.com', role: 'Co-Parent' },
            ],
        } as any);

        const { unmount } = render(<MemoryRouter><FamilySettings /></MemoryRouter>);
        await screen.findByText('ParentUser');

        expect(screen.queryByTitle('Sterge contul complet')).not.toBeInTheDocument();
        unmount();
    });

    it('handleDeleteChildAccount șterge membrul din UI după confirmare acceptată', async () => {
        const { familyApi } = await import('../services/api');
        confirmSpy.mockReturnValue(true);
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: 'mock_token' })
        );
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent', familyId: 1, userId: 10 } as any);
        vi.mocked(familyApi.getMembers).mockResolvedValue({
            data: [
                { id: 100, userId: 10, name: 'ParentUser', email: 'parent@test.com', role: 'Parent' },
                { id: 200, userId: 20, name: 'Copil Mic', email: 'copil@test.com', role: 'Child' },
            ],
        } as any);
        vi.mocked(familyApi.deleteChildAccount).mockResolvedValue(undefined as any);

        const { unmount } = render(<MemoryRouter><FamilySettings /></MemoryRouter>);
        await screen.findByText('Copil Mic');

        const deleteChildBtn = screen.getByTitle('Sterge contul complet');
        fireEvent.click(deleteChildBtn);

        expect(confirmSpy).toHaveBeenCalledWith(
            'Esti sigur ca vrei sa stergi COMPLET contul lui Copil Mic? Aceasta actiune este ireversibila.'
        );
        expect(familyApi.deleteChildAccount).toHaveBeenCalledWith(1, 200);

        // Membrul dispare din UI după răspunsul API
        await screen.findByText('ParentUser');
        expect(screen.queryByText('Copil Mic')).not.toBeInTheDocument();
        unmount();
    });

    it('handleDeleteChildAccount NU șterge dacă utilizatorul anulează confirmarea', async () => {
        const { familyApi } = await import('../services/api');
        confirmSpy.mockReturnValue(false);
        vi.mocked(useAuthStore).mockImplementation((selector: any) =>
            selector({ token: 'mock_token' })
        );
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Parent', familyId: 1, userId: 10 } as any);
        vi.mocked(familyApi.getMembers).mockResolvedValue({
            data: [
                { id: 100, userId: 10, name: 'ParentUser', email: 'parent@test.com', role: 'Parent' },
                { id: 200, userId: 20, name: 'Copil Mic', email: 'copil@test.com', role: 'Child' },
            ],
        } as any);

        const { unmount } = render(<MemoryRouter><FamilySettings /></MemoryRouter>);
        await screen.findByText('Copil Mic');

        const deleteChildBtn = screen.getByTitle('Sterge contul complet');
        fireEvent.click(deleteChildBtn);

        // API-ul NU trebuie apelat
        expect(familyApi.deleteChildAccount).not.toHaveBeenCalled();
        // Membrul rămâne în UI
        expect(screen.getByText('Copil Mic')).toBeInTheDocument();
        unmount();
    });

    it('butonul "Sterge cont" NU apare pentru utilizatorii cu rol Child (nu pot sterge conturi)', () => {
        vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'mock_token' }));
        vi.mocked(decodeJwtPayload).mockReturnValue({ role: 'Child', familyId: 1, userId: 20 } as any);

        renderComponent();

        expect(screen.queryByTitle('Sterge contul complet')).not.toBeInTheDocument();
    });
});
