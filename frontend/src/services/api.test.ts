import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getUserData } from './api';

describe('getUserData', () => {
  // Definim mockUser aici, în interiorul describe, dacă vrem să-l folosim în mai multe teste
  const mockUser = { id: 1, name: 'Test User', email: 'test@example.com' };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('ar trebui să returneze datele utilizatorului la succes', async () => {
    // Simulăm fetch-ul pentru succes
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => mockUser,
    }) as any;

    const data = await getUserData(1);

    expect(data).toEqual(mockUser);
    expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/users/1'));
  });

  it('ar trebui să arunce eroare dacă răspunsul nu este ok', async () => {
    // MODIFICARE: Simulăm un eșec (ok: false)
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false, // Aici trebuie să fie false pentru a testa eroarea
      status: 404,
      json: async () => ({ message: "Not Found" }),
    }) as any;

    // Acum verificăm dacă funcția aruncă eroarea pe care am definit-o în api.ts
    await expect(getUserData(1)).rejects.toThrow('Eroare la preluarea datelor de la server');
  });
});