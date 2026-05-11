import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'https://api.family-agent.me',
    withCredentials: true, 
});

// Interceptorul
api.interceptors.request.use(
    (config) => {

        // 1. Token-ul JWT (logica existentă a lui Dimir)
        const token = useAuthStore.getState().token;
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        // 2. Token-ul Anti-CSRF (NOU - pus de noi pentru securitate)
        const csrfToken = localStorage.getItem('csrfToken');
        // Îl trimitem doar la cererile care pot modifica date (nu și la GET)
        if (csrfToken && config.method !== 'get') {
            config.headers['X-XSRF-TOKEN'] = csrfToken;
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default api;