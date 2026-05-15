import { api } from './api'

export interface LatLng { lat: number; lng: number }
export interface GeofenceZone { id: number; name: string; coordinates: LatLng[] }

export async function saveGeofenceZone(coordinates: LatLng[], name?: string): Promise<void> {
    await api.post('/api/geofencing/zones', { coordinates, name: name ?? 'Zona de Siguranță' })
}

export async function getMyGeofenceZone(): Promise<GeofenceZone | null> {
    const { status, data } = await api.get<GeofenceZone>('/api/geofencing/zones/my', {
        validateStatus: (s) => s === 200 || s === 204,
    })
    return status === 204 ? null : data
}

export async function deleteGeofenceZone(): Promise<void> {
    await api.delete('/api/geofencing/zones/my')
}
