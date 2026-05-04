import { create } from 'zustand'
import { calculateDistanceInMeters } from '../utils/geofencing'

interface MapState {
    childLocation: { lat: number; lng: number; isRestricted: boolean } | null;
    isOptimisticallyDanger: boolean; // Feedback instantaneu

    // Metoda care deschide "țeava" (SSE)
    connectToLiveStream: (parentId: number, token: string) => void;
}

let sse: EventSource | null = null;
export const useMapStore = create<MapState>((set,) => ({
    childLocation: null,
    isOptimisticallyDanger: false,

    connectToLiveStream: (parentId, token) => {
        if (sse) return; // Nu deschidem două țevi deodată

        // Adresa de backend (Pipe-ul creat în Spring Boot)
        const url = `http://localhost:8080/api/v1/parent/location-stream?parentId=${parentId}&token=${token}`;

        sse = new EventSource(url);

        // Când "țeava" primește date noi de la DB
        sse.addEventListener('location-update', (event) => {
            const data = JSON.parse(event.data);

            // Ignorăm locațiile eronate (0,0) - protecție senzor picat
            if (data.lat === 0 && data.lng === 0) return;

            set({ childLocation: { lat: data.lat, lng: data.lng, isRestricted: data.isRestricted } });

            // Verificăm instant perimetrul (Optimistic Check)
            // Presupunem o zonă sigură la aceste coordonate pentru test
            const dist = calculateDistanceInMeters(data.lat, data.lng, 47.15, 27.59);
            set({ isOptimisticallyDanger: dist > 500 }); // Alerta dacă a ieșit din raza de 500m
        });
    }
}));