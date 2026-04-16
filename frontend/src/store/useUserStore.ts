import { create } from 'zustand';
// Observă cuvântul "type" adăugat aici:
import { getUserData, type UserData } from '../services/api';

interface UserState {
  user: UserData | null;
  isLoading: boolean;
  error: string | null;
  fetchUser: (id: number) => Promise<void>;
}

// Folosim tipul UserState direct în definirea store-ului
export const useUserStore = create<UserState>()((set) => ({
  user: null,
  isLoading: false,
  error: null,

  fetchUser: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const userData = await getUserData(id);
      set({ user: userData, isLoading: false });
    } catch (err: any) {
      set({ error: err.message || 'Ceva nu a mers bine', isLoading: false });
    }
  },
}));