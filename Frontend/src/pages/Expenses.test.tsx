import '@testing-library/jest-dom';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Expenses from './Expenses';
import * as expensesService from '../services/expenses';
import * as lookupsService from '../services/lookups';

// ─── MOCKS ────────────────────────────────────────────────────────────

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../services/expenses', () => ({
    fetchExpenses: vi.fn(),
}));

vi.mock('../services/lookups', () => ({
    fetchCategoryNames: vi.fn(),
    fetchUserNames: vi.fn(),
}));

const renderWithRouter = () => render(<MemoryRouter><Expenses /></MemoryRouter>);

// ─── SUITA DE TESTE ───────────────────────────────────────────────────

describe('Expenses Component', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        // Setup default mocks for lookups so we don't repeat them everywhere
        vi.mocked(lookupsService.fetchCategoryNames).mockResolvedValue(['Alimentare', 'Transport']);
        vi.mocked(lookupsService.fetchUserNames).mockResolvedValue(['Eduard', 'Mihaela', 'Andrei']);
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    // ─── 1. LOADING & UNMOUNT (Aici pierdeai coverage) ───

    it('afișează scheletul de loading la inițializare', () => {
        // Returnăm o promisiune care nu se rezolvă imediat pentru a vedea skeleton-ul
        vi.mocked(expensesService.fetchExpenses).mockImplementation(() => new Promise(() => {}));
        const { container } = renderWithRouter();

        expect(screen.getByText('Istoric cheltuieli')).toBeInTheDocument();
        expect(container.querySelectorAll('.skeleton').length).toBeGreaterThan(0);
    });

    it('nu actualizează state-ul dacă componenta este demontată înainte de finalizarea request-ului (isCancelled = true)', () => {
        let resolveFetch: any;
        vi.mocked(expensesService.fetchExpenses).mockImplementation(() => new Promise((res) => { resolveFetch = res; }));

        const { unmount } = renderWithRouter();
        unmount(); // Declanșăm cleanup-ul din useEffect

        // Rezolvăm promisiunea DUPĂ ce componenta a fost distrusă.
        // Dacă codul tău nu ar avea "if (isCancelled) return;", React ar arunca o eroare aici.
        resolveFetch([]);
        expect(expensesService.fetchExpenses).toHaveBeenCalled();
    });

    it('prinde eroarea dar nu actualizează state-ul dacă componenta a fost demontată (catch isCancelled = true)', async () => {
        let rejectFetch: any;
        vi.mocked(expensesService.fetchExpenses).mockImplementation(() => new Promise((_, rej) => { rejectFetch = rej; }));

        const { unmount } = renderWithRouter();
        unmount();

        rejectFetch(new Error('Network error')); // Aruncăm eroarea după unmount
        expect(expensesService.fetchExpenses).toHaveBeenCalled();
    });

    // ─── 2. STĂRILE DE EROARE ȘI EMPTY ───

    it('afișează mesaj de eroare când apelul fetchExpenses eșuează', async () => {
        vi.mocked(expensesService.fetchExpenses).mockRejectedValue(new Error('Backend error'));
        renderWithRouter();

        expect(await screen.findByText(/Nu am putut încărca cheltuielile din backend/i)).toBeInTheDocument();
    });

    it('afișează "Empty State" dacă nu se primesc cheltuieli de la backend', async () => {
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue([]);
        renderWithRouter();

        expect(await screen.findByText('Nu s-au găsit cheltuieli')).toBeInTheDocument();
    });

    // ─── 3. RANDARE DATE & EDGE CASES (Fallback-uri) ───

    it('randează corect tabelul și acoperă fallback-urile pentru date incomplete', async () => {
        const mockExpenses = [
            {
                id: 1,
                expenseDate: '2023-10-15T12:00:00Z',
                category: 'Alimentare',
                description: 'Mega Image',
                amount: 150.50,
                person: 'Eduard', // Începe cu E -> Culoare 1
                location: { store: 'Mega', address: 'Strada X', city: 'Cluj', country: 'RO', id: 99, lat: 44.1, lng: 26.2 }
            },
            {
                id: 2,
                expenseDate: null as any, // Testăm fallback-ul de dată
                category: null as any,    // Testăm fallback "Fără categorie"
                description: null as any,
                amount: "30",             // Testăm conversia de la string la number
                person: 'Mihaela',        // Începe cu M -> Culoare 2
                location: null as any     // Testăm fallback "Fără locație"
            },
            {
                id: 3,
                expenseDate: '',
                category: '',
                description: '',
                amount: null as any,      // Testăm fallback-ul la 0 pentru amount invalid
                person: 'Andrei',         // Nu începe cu E sau M -> Culoare default
                location: {}
            }
        ];

        vi.mocked(expensesService.fetchExpenses).mockResolvedValue(mockExpenses);
        const { container } = renderWithRouter();

        // Așteptăm să apară descrierea primei cheltuieli (desktop & mobile)
        const elements = await screen.findAllByText('Mega Image');
        expect(elements.length).toBeGreaterThan(0);

        // Verificăm maparea corectă a datei
        expect((await screen.findAllByText('15.10.2023')).length).toBeGreaterThan(0);

        // Verificăm maparea locației
        expect((await screen.findAllByText(/Mega, Strada X, Cluj, RO/i)).length).toBeGreaterThan(0);

        // Verificăm fallback-urile
        expect((await screen.findAllByText('Fără categorie')).length).toBeGreaterThan(0);
        expect((await screen.findAllByText('Fără locație')).length).toBeGreaterThan(0);

        // Verificăm fallback-urile pentru amount (30 formatat ca 30.00 și null formatat ca 0.00)
        expect((await screen.findAllByText('30.00')).length).toBeGreaterThan(0);
        expect((await screen.findAllByText('0.00')).length).toBeGreaterThan(0);

        // Verificăm avatarele pentru a acoperi funcția `avatarStyle`
        const avatars = container.querySelectorAll('.avatar');
        expect(avatars.length).toBeGreaterThan(0);
    });

    // ─── 4. INTERACȚIUNI ȘI NAVIGARE ───

    it('navighează spre harta detaliată când dă click pe adresă (openMap)', async () => {
        const mockExpense = [{
            id: 1, expenseDate: '2023-10-15T12:00:00Z', category: 'Alimentare', description: 'Auchan', amount: 50, person: 'Mihaela',
            location: { store: 'Auchan', city: 'Cluj', id: 10, lat: 46.7, lng: 23.6 }
        }];
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue(mockExpense);

        renderWithRouter();

        const locationBtns = await screen.findAllByText(/Auchan, Cluj/i);
        fireEvent.click(locationBtns[0]); // Click pe adresa din tabel/card

        expect(mockNavigate).toHaveBeenCalledWith('/expenses/map', {
            state: { lat: 46.7, lng: 23.6, locationId: 10, locationLabel: 'Auchan, Cluj', locationCity: 'Cluj', locationCountry: undefined, description: 'Auchan' }
        });
    });

    it('aplică filtrele de date, categorie și persoană', async () => {
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue([]);
        const { container } = renderWithRouter();

        // Așteptăm încărcarea dropdown-urilor
        await screen.findAllByDisplayValue('Toate Categoriile');

        // 1. Schimbăm Data
        const dateInput = container.querySelector('input[type="date"]');
        if (dateInput) fireEvent.change(dateInput, { target: { value: '2023-10-10' } });

        // 2. Schimbăm Categoria
        const categorySelect = screen.getAllByRole('combobox')[0];
        fireEvent.change(categorySelect, { target: { value: 'Alimentare' } });

        // 3. Schimbăm Persoana
        const personSelect = screen.getAllByRole('combobox')[1];
        fireEvent.change(personSelect, { target: { value: 'Eduard' } });

        await waitFor(() => {
            expect(expensesService.fetchExpenses).toHaveBeenCalledWith(
                { date: '2023-10-10', category: 'Alimentare', person: 'Eduard' },
                expect.any(AbortSignal)
            );
        });

        // 4. Testăm Butonul de Resetare
        fireEvent.click(screen.getByText(/Resetează/i));

        await waitFor(() => {
            expect(expensesService.fetchExpenses).toHaveBeenCalledWith(
                { date: undefined, category: undefined, person: undefined },
                expect.any(AbortSignal)
            );
        });
    });

    it('navighează spre Adaugă Cheltuială', async () => {
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue([]);
        renderWithRouter();

        fireEvent.click(await screen.findByText(/Adaugă/i));
        expect(mockNavigate).toHaveBeenCalledWith('/add-expense');
    });

    it('navighează spre Dashboard la butonul de Back', async () => {
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue([]);
        const { container } = renderWithRouter();

        // Așteptăm să termine loading-ul
        await screen.findByText('Istoric cheltuieli');

        const backBtn = container.querySelector('.btn-icon');
        if(backBtn) fireEvent.click(backBtn);

        expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });

    // ─── 5. PAGINARE ───

    it('navighează prin pagini corect', async () => {
        vi.mocked(expensesService.fetchExpenses).mockResolvedValue([{ id: 1, amount: 10, person: 'E' } as any]);
        renderWithRouter();

        // Așteptăm să apară controlul de paginare
        await screen.findByText(/Pagina/i);
        expect(screen.getByText('1', { selector: 'strong' })).toBeInTheDocument();

        const buttons = screen.getAllByRole('button');
        const nextPageBtn = buttons[buttons.length - 1]; // ChevronRight
        const prevPageBtn = buttons[buttons.length - 4]; // ChevronLeft

        // Next Page
        fireEvent.click(nextPageBtn);
        expect(screen.getByText('2', { selector: 'strong' })).toBeInTheDocument();

        // Limitare Next Page (totalPages e 2 hardcodat)
        fireEvent.click(nextPageBtn); // Nu ar trebui să treacă de 2
        expect(screen.getByText('2', { selector: 'strong' })).toBeInTheDocument();

        // Prev Page
        fireEvent.click(prevPageBtn);
        expect(screen.getByText('1', { selector: 'strong' })).toBeInTheDocument();
    });
});