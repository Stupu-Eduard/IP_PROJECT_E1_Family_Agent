import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const api = axios.create({
    baseURL: 'http://localhost:8081',
    withCredentials: true, // Permite trimiterea cookie-urilor de sesiune
});

//  Interceptorul
api.interceptors.request.use(
    (config) => {

        const token = useAuthStore.getState().token;
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default api;