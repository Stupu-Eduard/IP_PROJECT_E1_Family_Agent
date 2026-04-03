import { create } from 'zustand';

interface AuthState {
    token: string | null;
    isAuthenticated: boolean;
    login: (token: string) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    token: localStorage.getItem('auth_token'),
    isAuthenticated: !!localStorage.getItem('auth_token'),
    login: (token) => {
        localStorage.setItem('auth_token', token);
        set({ token, isAuthenticated: true });
    },
    logout: () => {
        localStorage.removeItem('auth_token');
        set({ token: null, isAuthenticated: false });
    },
}));