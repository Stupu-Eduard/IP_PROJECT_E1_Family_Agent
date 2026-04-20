import { api } from './api'

export async function updateLocationCoordinates(locationId: number, lat: number, lng: number): Promise<void> {
  await api.post(`/api/v1/locations/${locationId}/coordinates`, { lat, lng })
}
