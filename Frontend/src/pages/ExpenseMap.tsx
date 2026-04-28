import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, MapPin } from 'lucide-react'
import { GoogleMap, InfoWindow, Marker, useJsApiLoader } from '@react-google-maps/api'
import { updateLocationCoordinates } from '../services/locations'

type MapState = {
  lat?: number
  lng?: number
  locationId?: number
  locationLabel?: string
  description?: string
}

type LatLng = { lat: number; lng: number }

//  Geofencing Engine (Client-Side)
// Algoritm matematic Ray-Casting pentru a verifica daca un punct este in poligon
function isInsideGeofence(point: LatLng, polygon: LatLng[]) {
  let isInside = false;
  const x = point.lng, y = point.lat;

  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const xi = polygon[i].lng, yi = polygon[i].lat;
    const xj = polygon[j].lng, yj = polygon[j].lat;

    const intersect = ((yi > y) !== (yj > y)) &&
        (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
    if (intersect) isInside = !isInside;
  }

  return isInside;
}
// -----------------------------------------------------

export default function ExpenseMap() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = (location.state ?? {}) as MapState

  const mapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined
  const geocodingApiKey = (import.meta.env.VITE_GOOGLE_GEOCODING_API_KEY as string | undefined) ?? mapsApiKey

  const { isLoaded, loadError } = useJsApiLoader({
    id: 'family-agent-google-maps',
    googleMapsApiKey: mapsApiKey ?? '',
  })

  const [center, setCenter] = useState<LatLng>({ lat: 44.4268, lng: 26.1025 })
  const [marker, setMarker] = useState<LatLng | null>(null)
  const [infoOpen, setInfoOpen] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const [resolvedAddress, setResolvedAddress] = useState<string | null>(null)
  const [placeId, setPlaceId] = useState<string | null>(null)
  const [googleMapsUri, setGoogleMapsUri] = useState<string | null>(null)

  //  Adaugat pentru Geofencing
  const [isOutsideZone, setIsOutsideZone] = useState(false);

  // Vom folosi acest patrat (centrul Bucurestiului) ca zona sigura de test
  const safeZone = [
    { lat: 44.4300, lng: 26.0900 },
    { lat: 44.4300, lng: 26.1100 },
    { lat: 44.4200, lng: 26.1100 },
    { lat: 44.4200, lng: 26.0900 },
  ];
  // -----------------------------------------------------

  const [place, setPlace] = useState<{
    name?: string
    formattedAddress?: string
    rating?: number
    userRatingCount?: number
    openNow?: boolean
    weekdayDescriptions?: string[]
    phone?: string
    websiteUri?: string
    photoName?: string
  } | null>(null)

  // --- THE PIPE: Conexiunea pentru actualizare LIVE ---
  useEffect(() => {
    console.log('⏳ HARTA: Se inițializează fluxul live prin THE PIPE...');
    const socket = new WebSocket('ws://localhost:8081/locatie');

    socket.onopen = () => console.log('🟢 HARTA: Conectat la fluxul live!');

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log('📍 HARTA: Locație nouă primită:', data);

        // Actualizăm markerul și centrul hărții cu datele de la adaptorul tău
        if (data.lat && data.lng) {
          const newPos = { lat: data.lat, lng: data.lng };
          setMarker(newPos);
          setCenter(newPos);
          
          // Dacă adaptorul tău zice că e zonă restricționată, activăm alerta
          if (data.isRestricted) {
            setIsOutsideZone(true);
          }
        }
      } catch (e) {
        console.error('❌ HARTA: Eroare la procesarea datelor live', e);
      }
    };

    return () => socket.close();
  }, []);
  // ----------------------------------------------------

  const label = (state.locationLabel ?? '').trim()

  async function geocodeAddress(address: string, signal: AbortSignal) {
    if (!geocodingApiKey) return null

    const url = `https://maps.googleapis.com/maps/api/geocode/json?address=${encodeURIComponent(address)}&key=${encodeURIComponent(geocodingApiKey)}`
    const res = await fetch(url, { signal })
    const data: any = await res.json()

    const first = data?.results?.[0]
    const loc = first?.geometry?.location
    if (!loc || typeof loc.lat !== 'number' || typeof loc.lng !== 'number') {
      return { error: data?.status ?? 'UNKNOWN', errorMessage: data?.error_message ?? null }
    }

    return {
      coords: { lat: loc.lat as number, lng: loc.lng as number },
      formattedAddress: first?.formatted_address ?? null,
      geocodePlaceId: first?.place_id ?? null,
    }
  }

  async function searchPlaceNear(textQuery: string, coords: LatLng, signal: AbortSignal) {
    if (!mapsApiKey) return null

    const res = await fetch('https://places.googleapis.com/v1/places:searchText', {
      method: 'POST',
      signal,
      headers: {
        'Content-Type': 'application/json',
        'X-Goog-Api-Key': mapsApiKey,
        'X-Goog-FieldMask': 'places.id,places.googleMapsUri',
      },
      body: JSON.stringify({
        textQuery,
        locationBias: {
          circle: {
            center: { latitude: coords.lat, longitude: coords.lng },
            radius: 800,
          },
        },
      }),
    })

    if (!res.ok) return null
    const data: any = await res.json()
    const first = data?.places?.[0]
    if (!first?.id) return null

    return {
      id: first.id as string,
      googleMapsUri: (first.googleMapsUri as string | undefined) ?? null,
    }
  }

  async function fetchPlaceDetails(id: string, signal: AbortSignal) {
    if (!mapsApiKey) return null

    const res = await fetch(`https://places.googleapis.com/v1/places/${encodeURIComponent(id)}`, {
      method: 'GET',
      signal,
      headers: {
        'X-Goog-Api-Key': mapsApiKey,
        'X-Goog-FieldMask':
            'displayName,formattedAddress,rating,userRatingCount,websiteUri,regularOpeningHours.openNow,regularOpeningHours.weekdayDescriptions,nationalPhoneNumber,internationalPhoneNumber,photos,googleMapsUri',
      },
    })

    if (!res.ok) return null
    const data: any = await res.json()

    return {
      name: data?.displayName?.text as string | undefined,
      formattedAddress: data?.formattedAddress as string | undefined,
      rating: data?.rating as number | undefined,
      userRatingCount: data?.userRatingCount as number | undefined,
      openNow: data?.regularOpeningHours?.openNow as boolean | undefined,
      weekdayDescriptions: (data?.regularOpeningHours?.weekdayDescriptions as string[] | undefined) ?? undefined,
      phone: (data?.internationalPhoneNumber as string | undefined) ?? (data?.nationalPhoneNumber as string | undefined),
      websiteUri: data?.websiteUri as string | undefined,
      photoName: data?.photos?.[0]?.name as string | undefined,
      googleMapsUri: (data?.googleMapsUri as string | undefined) ?? null,
    }
  }

  useEffect(() => {
    setMessage(null)
    setResolvedAddress(null)
    setPlaceId(null)
    setGoogleMapsUri(null)
    setPlace(null)
    setInfoOpen(false)
    setIsOutsideZone(false) // Resetam alerta la schimbarea locatiei

    if (!mapsApiKey || !isLoaded || loadError) return

    const normalized = label.toLowerCase()
    if (!label) {
      setMarker(null)
      setMessage('Nu există text de locație pentru geocoding.')
      return
    }
    if (normalized === 'online' || normalized === 'fără locație' || normalized === 'fara locatie') {
      setMarker(null)
      setMessage('Locația este non-geografică (ex: Online) și nu poate fi geocodată.')
      return
    }

    const controller = new AbortController()

    async function run() {
      // 1) Get coordinates (from DB or Geocoding)
      let coords: LatLng | null = null
      let addressFromGeocode: string | null = null
      let geocodePlaceId: string | null = null
      const hadDbCoords = typeof state.lat === 'number' && typeof state.lng === 'number'

      if (hadDbCoords) {
        coords = { lat: state.lat as number, lng: state.lng as number }
        setMessage('Folosesc coordonatele salvate.')
      } else {
        if (!geocodingApiKey) {
          setMessage('Lipsește cheia pentru Geocoding API. Setează VITE_GOOGLE_GEOCODING_API_KEY.')
          setMarker(null)
          return
        }

        setMessage(`Se caută locația pe hartă… ("${label}")`)
        const geo = await geocodeAddress(label, controller.signal)
        if (!geo || (geo as any).error) {
          const err = (geo as any)?.error ?? 'UNKNOWN'
          const extra = (geo as any)?.errorMessage ? ` - ${(geo as any).errorMessage}` : ''
          setMarker(null)
          setMessage(`${err}${extra}`)
          return
        }

        coords = (geo as any).coords as LatLng
        addressFromGeocode = (geo as any).formattedAddress as string | null
        geocodePlaceId = (geo as any).geocodePlaceId as string | null
      }

      setCenter(coords)
      setMarker(coords)
      setResolvedAddress(addressFromGeocode)

      // Verificam matematic INSTANT daca punctul e in afara zonei
      if (coords) {
        const isSafe = isInsideGeofence(coords, safeZone);
        setIsOutsideZone(!isSafe);
      }
      // ------------------------------------------------

      // 2) Save coords to DB if they were missing
      if (!hadDbCoords && state.locationId) {
        try {
          await updateLocationCoordinates(state.locationId, coords.lat, coords.lng)
          setMessage('Coordonatele au fost salvate în baza de date.')
        } catch {
          setMessage('Am găsit coordonatele, dar nu le-am putut salva în baza de date.')
        }
      }

      // 3) Places: find the place near these coords and load details
      try {
        const found = await searchPlaceNear(label, coords, controller.signal)

        if (found?.id) {
          setPlaceId(found.id)
          setGoogleMapsUri(found.googleMapsUri)

          const details = await fetchPlaceDetails(found.id, controller.signal)
          if (details) {
            setPlace(details)
            if (details.googleMapsUri) setGoogleMapsUri(details.googleMapsUri)
          }
        } else {
          // fallback: at least keep the geocoding place id
          if (geocodePlaceId) setPlaceId(geocodePlaceId)
        }
      } catch {
        // ignore
      }
    }

    void run()
    return () => controller.abort()
  }, [geocodingApiKey, isLoaded, label, loadError, mapsApiKey, state.lat, state.lng, state.locationId])

  const googleMapsLink = (() => {
    if (googleMapsUri) return googleMapsUri
    if (placeId) return `https://www.google.com/maps/place/?q=place_id:${placeId}`
    if (marker) return `https://www.google.com/maps/search/?api=1&query=${marker.lat},${marker.lng}`
    return null
  })()

  const placePhotoSrc = (() => {
    if (!mapsApiKey) return null
    if (!place?.photoName) return null
    return `https://places.googleapis.com/v1/${place.photoName}/media?maxHeightPx=400&maxWidthPx=800&key=${mapsApiKey}`
  })()

  return (
      <div className="min-h-screen bg-[#FAF8F5] font-sans flex flex-col">
        <nav className="sticky top-0 z-10 bg-[#FAF8F5] border-b border-[#EDE9E3] px-6 lg:px-10 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
                type="button"
                onClick={() => navigate(-1)}
                className="w-10 h-10 bg-white border border-[#EDE9E3] rounded-[10px] flex items-center justify-center text-[#2D2926] hover:border-[#C4B9AC] transition-colors shadow-sm"
                aria-label="Înapoi"
            >
              <ArrowLeft size={18} />
            </button>
            <div className="flex flex-col">
              <h2 className="text-[18px] sm:text-[20px] font-medium text-[#2D2926] tracking-tight leading-tight">Hartă</h2>
              <div className="text-[12px] text-[#B8A99A] flex items-center gap-1.5 mt-0.5">
                <MapPin size={12} />
                <span>{label || 'Locație'}</span>
              </div>
            </div>
          </div>
        </nav>

        <div className="px-6 lg:px-10 pt-6 pb-10 max-w-[1200px] mx-auto w-full flex-1">

          {/* --- BANNER DE ALERTA GEOFENCING (Task Miron Andrei) --- */}
          {isOutsideZone && marker && (
              <div className="mb-4 bg-red-100 border-l-4 border-red-500 text-red-700 p-4 rounded shadow-sm animate-pulse">
                <div className="flex items-center">
                  <span className="font-bold text-lg mr-2">⚠️ ALERTĂ DE SECURITATE:</span>
                  <span>Locația este în afara perimetrului de siguranță!</span>
                </div>
              </div>
          )}
          {/* ------------------------------------------------------- */}

          {state.description && <div className="mb-3 text-[14px] text-[#2D2926] font-medium">{state.description}</div>}

          {!mapsApiKey && (
              <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 shadow-sm text-[13px] text-[#9A8A7C]">
                Lipsește cheia Google Maps. Setează <span className="font-medium text-[#2D2926]">VITE_GOOGLE_MAPS_API_KEY</span> în <span className="font-medium text-[#2D2926]">Frontend/.env.local</span>.
              </div>
          )}

          {mapsApiKey && loadError && (
              <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 shadow-sm text-[13px] text-[#9A8A7C]">
                Nu s-a putut încărca Google Maps. Verifică cheia și restricțiile ei în Google Cloud Console.
              </div>
          )}

          {mapsApiKey && !loadError && !isLoaded && (
              <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 shadow-sm text-[13px] text-[#9A8A7C]">Se încarcă harta…</div>
          )}

          {mapsApiKey && !loadError && isLoaded && (
              <div className="bg-white border border-[#EDE9E3] rounded-[14px] overflow-hidden shadow-[0_4px_20px_rgba(0,0,0,0.02)]">
                <div className="flex flex-col md:flex-row">
                  <aside className="md:w-[340px] md:border-r border-[#EDE9E3] p-5">
                    {placePhotoSrc && (
                        <div className="mb-4 overflow-hidden rounded-[12px] border border-[#EDE9E3] bg-[#FAF8F5]">
                          <img
                              src={placePhotoSrc}
                              alt={place?.name || label}
                              className="w-full h-[160px] object-cover"
                              loading="lazy"
                          />
                        </div>
                    )}

                    <div className="text-[12px] text-[#9A8A7C]">Locație</div>
                    <div className="mt-1 text-[16px] font-medium text-[#2D2926] leading-snug">{place?.name || label || 'Locație'}</div>

                    {(place?.formattedAddress || resolvedAddress) && (
                        <div className="mt-1 text-[13px] text-[#9A8A7C]">{place?.formattedAddress || resolvedAddress}</div>
                    )}

                    {(typeof place?.rating === 'number' || typeof place?.userRatingCount === 'number') && (
                        <div className="mt-2 text-[13px] text-[#2D2926]">
                          {typeof place?.rating === 'number' ? `Rating: ${place.rating}` : null}
                          {typeof place?.userRatingCount === 'number' ? ` (${place.userRatingCount})` : null}
                        </div>
                    )}

                    {typeof place?.openNow === 'boolean' && (
                        <div className="mt-2 text-[13px]">
                          <span className="font-medium text-[#2D2926]">{place.openNow ? 'Deschis' : 'Închis'}</span>
                        </div>
                    )}

                    {place?.weekdayDescriptions?.length ? (
                        <div className="mt-1 text-[12px] text-[#9A8A7C]">
                          {place.weekdayDescriptions.slice(0, 3).map((line) => (
                              <div key={line}>{line}</div>
                          ))}
                        </div>
                    ) : null}

                    {place?.phone && <div className="mt-3 text-[13px] text-[#2D2926]">{place.phone}</div>}

                    {place?.websiteUri && (
                        <a
                            href={place.websiteUri}
                            target="_blank"
                            rel="noreferrer"
                            className="mt-3 inline-flex items-center justify-center h-10 px-3 rounded-[10px] bg-white border border-[#EDE9E3] text-[13px] text-[#2D2926] hover:border-[#C4B9AC] transition-colors"
                        >
                          Website
                        </a>
                    )}

                    {googleMapsLink && (
                        <a
                            href={googleMapsLink}
                            target="_blank"
                            rel="noreferrer"
                            className="mt-4 inline-flex items-center justify-center h-10 px-3 rounded-[10px] bg-[#2D2926] text-white text-[13px] hover:opacity-90 transition-opacity"
                        >
                          Deschide în Google Maps
                        </a>
                    )}

                    {message && <div className="mt-4 text-[12px] text-[#9A8A7C]">{message}</div>}
                  </aside>

                  <div className="flex-1">
                    <GoogleMap
                        mapContainerStyle={{ width: '100%', height: '520px' }}
                        center={center}
                        zoom={marker ? 17 : 12}
                        options={{ mapTypeControl: false, streetViewControl: false, fullscreenControl: false }}
                    >
                      {marker && <Marker position={marker} onClick={() => setInfoOpen(true)} />}

                      {marker && infoOpen && (
                          <InfoWindow position={marker} onCloseClick={() => setInfoOpen(false)}>
                            <div style={{ maxWidth: 220 }}>
                              {placePhotoSrc && (
                                  <div style={{ marginBottom: 8 }}>
                                    <img
                                        src={placePhotoSrc}
                                        alt={place?.name || label}
                                        style={{ width: '100%', height: 110, objectFit: 'cover', borderRadius: 8 }}
                                        loading="lazy"
                                    />
                                  </div>
                              )}

                              <div style={{ fontWeight: 600, marginBottom: 4 }}>{place?.name || label}</div>
                              <div style={{ fontSize: 12, color: '#6b7280' }}>{place?.formattedAddress || resolvedAddress || ''}</div>

                              {googleMapsLink && (
                                  <a
                                      href={googleMapsLink}
                                      target="_blank"
                                      rel="noreferrer"
                                      style={{ display: 'inline-block', marginTop: 8, fontSize: 12, color: '#111827' }}
                                  >
                                    Deschide în Google Maps
                                  </a>
                              )}
                            </div>
                          </InfoWindow>
                      )}
                    </GoogleMap>
                  </div>
                </div>
              </div>
          )}
        </div>
      </div>
  )
}