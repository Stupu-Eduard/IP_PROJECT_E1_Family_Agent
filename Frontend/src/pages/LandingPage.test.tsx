import '@testing-library/jest-dom'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import LandingPage from './LandingPage'

// ── Mock REPARAT pentru IntersectionObserver ──
let observerCallback: any = null;
const mockObserve = vi.fn();
const mockDisconnect = vi.fn();

// Fix: Folosim o funcție clasică, nu arrow function, pentru a permite folosirea 'new'
global.IntersectionObserver = vi.fn().mockImplementation(function(callback) {
    observerCallback = callback;
    return {
        observe: mockObserve,
        disconnect: mockDisconnect,
        unobserve: vi.fn(),
    };
}) as any;

describe('LandingPage Component', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        window.scrollY = 0;
        observerCallback = null;
    })

    const renderComponent = () => render(
        <MemoryRouter>
            <LandingPage />
        </MemoryRouter>
    )

    it('1. Randează corect elementele principale de branding și Hero', () => {
        renderComponent();

        // Verificăm prezența logo-ului în Nav și Footer
        expect(screen.getAllByText(/FamilyAgent/i).length).toBeGreaterThan(0);

        // Verificăm titlul principal
        expect(screen.getByText(/Finanțele familiei,/i)).toBeInTheDocument();

        // Verificăm butoanele duplicate (Hero și Bottom CTA)[cite: 3]
        const ctaButtons = screen.getAllByText(/Creează cont gratuit/i);
        expect(ctaButtons.length).toBe(2);
    })

    it('2. Gestionează corect efectul de scroll pentru Navbar', async () => {
        renderComponent();
        const navbar = screen.getByRole('navigation');

        // Inițial e transparent[cite: 3]
        expect(navbar).not.toHaveStyle('background: rgba(250,248,245,0.92)');

        // Simulăm scroll-ul peste pragul de 40px[cite: 3]
        await act(async () => {
            Object.defineProperty(window, 'scrollY', { value: 50, writable: true });
            fireEvent.scroll(window);
        });

        // Verificăm aplicarea stilului de scroll[cite: 3]
        expect(navbar).toHaveStyle('background: rgba(250,248,245,0.92)');
    });

    it('3. Simulează declanșarea animațiilor via IntersectionObserver', () => {
        renderComponent();

        // Simulăm intrarea în viewport a primului feature (index 0)[cite: 3]
        act(() => {
            if (observerCallback) {
                observerCallback([
                    { isIntersecting: true, target: { dataset: { idx: '0' } } }
                ]);
            }
        });

        // Verificăm dacă opacity a devenit 1 pentru cardul de feature[cite: 3]
        const featureTitle = screen.getByText('Rapoarte Inteligente');
        const featureCard = featureTitle.closest('.group');
        expect(featureCard).toHaveStyle('opacity: 1');
    });

    it('4. Verifică secțiunea de statistici și cardul de previzualizare', () => {
        renderComponent();

        // Verificăm valorile din array-ul stats[cite: 3]
        expect(screen.getByText('100%')).toBeInTheDocument();
        expect(screen.getByText('24/7')).toBeInTheDocument();

        // Verificăm datele din Mock UI (3.240 RON și Sfat AI)[cite: 3]
        expect(screen.getByText('3.240 RON')).toBeInTheDocument();
        expect(screen.getByText(/Sfat AI/i)).toBeInTheDocument();
    });

    it('5. Verifică destinațiile corecte ale link-urilor de navigare', () => {
        renderComponent();

        // Verificăm butoanele care trebuie să ducă la Register[cite: 3]
        const registerLink = screen.getByText(/Începe gratuit/i);
        expect(registerLink.closest('a')).toHaveAttribute('href', '/register');

        // Verificăm link-ul de Login[cite: 3]
        const loginLink = screen.getByText(/Am deja cont/i);
        expect(loginLink).toHaveAttribute('href', '/login');
    });

    it('6. Verifică demontarea event listener-ilor (Cleanup)', () => {
        const removeSpy = vi.spyOn(window, 'removeEventListener');
        const { unmount } = renderComponent();

        unmount();

        // Verificăm dacă s-a făcut cleanup pe scroll și pe observer[cite: 3]
        expect(removeSpy).toHaveBeenCalledWith('scroll', expect.any(Function));
        expect(mockDisconnect).toHaveBeenCalled();
    });
})