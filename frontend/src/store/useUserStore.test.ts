import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useUserStore } from './useUserStore';
import * as api from '../services/api';

describe('UserStore', () => {
  // AICI folosim beforeEach ca să curățăm terenul înainte de fiecare test
  beforeEach(() => {
    vi.restoreAllMocks();
    // Resetăm manual starea store-ului la valorile inițiale
    useUserStore.setState({ user: null, isLoading: false, error: null });
  });

  it('ar trebui să seteze user-ul și isLoading: false după succes', async () => {
    const mockUser = { id: 1, name: 'Andrei', email: 'a@test.com' };
    const spy = vi.spyOn(api, 'getUserData').mockResolvedValue(mockUser);

    await useUserStore.getState().fetchUser(1);

    expect(useUserStore.getState().user).toEqual(mockUser);
    expect(useUserStore.getState().isLoading).toBe(false);
    expect(spy).toHaveBeenCalledWith(1);
  });
});