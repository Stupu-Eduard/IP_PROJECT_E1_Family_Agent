
import { useEffect, useState, useRef } from 'react';
// Helper to geocode an address using Google Maps Geocoding API
async function geocodeAddress(address: string, apiKey: string): Promise<{ lat: number, lng: number } | null> {
  if (!address || !apiKey) return null;
  try {
    const url = `https://maps.googleapis.com/maps/api/geocode/json?address=${encodeURIComponent(address)}&key=${apiKey}`;
    const res = await fetch(url);
    if (!res.ok) return null;
    const data = await res.json();
    const loc = data?.results?.[0]?.geometry?.location;
    if (loc && typeof loc.lat === 'number' && typeof loc.lng === 'number') {
      return { lat: loc.lat, lng: loc.lng };
    }
    return null;
  } catch {
    return null;
  }
}
import { useLocation, useNavigate } from 'react-router-dom';
import { GoogleMap, Marker, Polygon, useJsApiLoader, DrawingManager, MarkerClusterer } from '@react-google-maps/api';
import { ArrowLeft, MapPin, Calendar, ChevronDown, Filter, Shield, ShieldOff } from 'lucide-react';
import { fetchExpenses } from '../services/expenses';
import { fetchCategoryNames, fetchUserNames } from '../services/lookups';
import { saveGeofenceZone, getMyGeofenceZone, deleteGeofenceZone, type LatLng } from '../services/geofencing';

type MapExpense = {
  id: number
  lat?: number
  lng?: number
  amount?: number
  category?: string
  person?: string
  description?: string
  location?: string
  rawDate?: string
}

const mapApiExpenseToMapExpense = (expense: any): MapExpense => {
  const isoDate = expense.expenseDate ?? ''
  const datePart = isoDate ? isoDate.slice(0, 10) : ''
  const location = [expense.location?.store, expense.location?.address, expense.location?.city, expense.location?.country]
    .filter(Boolean)
    .join(', ') || 'Fără locație'
  const amountNumber = typeof expense.amount === 'number' ? expense.amount : Number(expense.amount)

  return {
    id: expense.id,
    lat: expense.location?.lat ?? undefined,
    lng: expense.location?.lng ?? undefined,
    amount: Number.isFinite(amountNumber) ? amountNumber : 0,
    category: expense.category ?? 'Fără categorie',
    person: expense.person ?? 'N/A',
    description: expense.description ?? '',
    location,
    rawDate: datePart,
  }
}

import { useAuthStore } from '../store/authStore';

export default function ExpensesMapAll() {
  const navigate = useNavigate();
  const location = useLocation();
  const token = useAuthStore((state) => state.token);

  // --- STATE PENTRU LOCAȚIE LIVE (THE PIPE) ---
  const [liveLocation, setLiveLocation] = useState<{ lat: number, lng: number, isRestricted?: boolean, isOutsideGeofence?: boolean } | null>(null);

  useEffect(() => {
    if (!token) return;
    console.log('📡 HARTA LIVE: Se inițializează fluxul live prin THE PIPE...');

    const host = window.location.hostname === 'localhost' ? 'localhost:8080' : window.location.host;
    const wsUrl = import.meta.env.VITE_WS_BASE_URL || (window.location.protocol === 'https:' ? 'wss://' : 'ws://') + host;

    const socket = new WebSocket(`${wsUrl}/locatie?token=${token}`);

    socket.onopen = () => console.log('🟢 HARTA LIVE: Conectat la fluxul live!');
    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.lat && data.lng) {
          console.log('📍 HARTA LIVE: Poziție nouă primită:', data);
          setLiveLocation({ lat: data.lat, lng: data.lng, isRestricted: data.isRestricted, isOutsideGeofence: data.isOutsideGeofence });
        }
      } catch (e) {
        console.error('❌ HARTA LIVE: Eroare date WebSocket', e);
      }
    };

    return () => socket.close();
  }, [token]);
  // --------------------------------------------

  const locationState = (location.state || {}) as any;
  const hasInjectedExpenses = Object.prototype.hasOwnProperty.call(locationState, 'expenses');
  const injectedExpenses = (hasInjectedExpenses ? locationState.expenses : []) as MapExpense[];
  const { filters = {} } = locationState;
  const [remoteExpenses, setRemoteExpenses] = useState<MapExpense[]>([]);
  const [isLoadingExpenses, setIsLoadingExpenses] = useState(!hasInjectedExpenses);
  const [expenseLoadError, setExpenseLoadError] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState(filters.selectedCategory || '');
  const [selectedPerson, setSelectedPerson] = useState(filters.selectedPerson || '');
  const [startDate, setStartDate] = useState(filters.startDate || '');
  const [endDate, setEndDate] = useState(filters.endDate || '');
  const [availableCategories, setAvailableCategories] = useState<string[]>([]);
  const [availablePeople, setAvailablePeople] = useState<string[]>([]);

  const inputStyle = "w-full bg-white border border-[#EDE9E3] rounded-[10px] px-4 py-2.5 text-[13px] text-[#2D2926] placeholder:text-[#C4B9AC] focus:outline-none focus:border-[#C4B9AC] transition-colors appearance-none";

  useEffect(() => {
    const controller = new AbortController();
    const run = async () => {
      try {
        const [cats, people] = await Promise.all([
          fetchCategoryNames(controller.signal),
          fetchUserNames(controller.signal),
        ]);
        setAvailableCategories((cats ?? []).filter(Boolean));
        setAvailablePeople((people ?? []).filter(Boolean));
      } catch {
        // ignore
      }
    };
    run();
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (hasInjectedExpenses) {
      setRemoteExpenses([]);
      setExpenseLoadError(null);
      setIsLoadingExpenses(false);
      return;
    }

    const controller = new AbortController();
    let cancelled = false;

    const run = async () => {
      setIsLoadingExpenses(true);
      setExpenseLoadError(null);

      try {
        const data = await fetchExpenses({}, controller.signal);
        if (cancelled) return;
        setRemoteExpenses(data.map(mapApiExpenseToMapExpense));
      } catch {
        if (cancelled) return;
        setRemoteExpenses([]);
        setExpenseLoadError('Nu am putut încărca cheltuielile. Deschide pagina Cheltuieli pentru a trimite datele sau verifică backend-ul.');
      } finally {
        if (!cancelled) setIsLoadingExpenses(false);
      }
    };

    run();

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [hasInjectedExpenses]);

  // polygon drawing state (expense filter)
  const polygonRef = useRef<any>(null);
  const [drawingEnabled, setDrawingEnabled] = useState(false);

  // safe zone state
  const drawingModeRef = useRef<'expense' | 'safezone'>('expense');
  const safeZoneDrawnRef = useRef<any>(null);
  const [safeZoneDrawing, setSafeZoneDrawing] = useState(false);
  const [savedSafeZone, setSavedSafeZone] = useState<LatLng[] | null>(null);
  const [pendingSafeZoneCoords, setPendingSafeZoneCoords] = useState<LatLng[] | null>(null);
  const [safeZoneSaving, setSafeZoneSaving] = useState(false);

  // selected expense / place details for left panel
  const [selectedExpense, setSelectedExpense] = useState<any | null>(null);
  const [place, setPlace] = useState<any | null>(null);
  const [placeId, setPlaceId] = useState<string | null>(null);
  const [googleMapsUri, setGoogleMapsUri] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const mapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined;
  const { isLoaded, loadError } = useJsApiLoader({
    id: 'family-agent-google-maps',
    googleMapsApiKey: mapsApiKey ?? '',
    libraries: ['drawing', 'geometry'],
  });

  useEffect(() => {
    getMyGeofenceZone()
      .then((zone) => { if (zone) setSavedSafeZone(zone.coordinates) })
      .catch(() => {})
  }, [])

  useEffect(() => {
    return () => {
      try {
        if (polygonRef.current) {
          const poly: any = polygonRef.current;
          if (poly.__listeners) {
            poly.__listeners.forEach((l: any) => l.remove?.());
            poly.__listeners = null;
          }
          poly.setMap(null);
          polygonRef.current = null;
        }
        if (safeZoneDrawnRef.current) {
          safeZoneDrawnRef.current.setMap(null);
          safeZoneDrawnRef.current = null;
        }
      } catch {
        // ignore
      }
    };
  }, []);

  const placePhotoSrc = (() => {
    if (!mapsApiKey) return null;
    if (!place?.photoName) return null;
    return `https://places.googleapis.com/v1/${place.photoName}/media?maxHeightPx=400&maxWidthPx=800&key=${mapsApiKey}`;
  })();

  const googleMapsLink = (() => {
    if (googleMapsUri) return googleMapsUri;
    if (placeId) return `https://www.google.com/maps/place/?q=place_id:${placeId}`;
    return null;
  })();

  // Geocode missing lat/lng for expenses
  const [geocodedExpenses, setGeocodedExpenses] = useState<any[]>([]);
  const expenses = hasInjectedExpenses ? injectedExpenses : remoteExpenses;

  useEffect(() => {
    if (!mapsApiKey || !expenses.length) return;
    let cancelled = false;
    (async () => {
      const results = await Promise.all(
        expenses.map(async (e: any) => {
          if (typeof e.lat === 'number' && typeof e.lng === 'number') return e;
          // Try to geocode using location/address fields
          const addr = e.location || e.adresa || e.adress || e.nume || e.store || e.description || '';
          const geo = await geocodeAddress(addr, mapsApiKey);
          if (geo) return { ...e, lat: geo.lat, lng: geo.lng, _geocoded: true };
          return e;
        })
      );
      if (!cancelled) setGeocodedExpenses(results);
    })();
    return () => { cancelled = true; };
  }, [expenses, mapsApiKey]);

  const isPointInPolygon = (lat: number, lng: number) => {
    try {
      if (!polygonRef.current || !(window as any).google?.maps?.geometry?.poly) return true;
      const latlng = new (window as any).google.maps.LatLng(lat, lng);
      return (window as any).google.maps.geometry.poly.containsLocation(latlng, polygonRef.current);
    } catch {
      return true;
    }
  };

  const filteredExpenses = (geocodedExpenses.length ? geocodedExpenses : expenses).filter((e: any) => {
    const matchCategory = !selectedCategory || e.category === selectedCategory;
    const matchPerson = !selectedPerson || e.person === selectedPerson;
    const rawDate = e.rawDate || e.date || e.expenseDate || '';
    const matchStart = !startDate || (rawDate && rawDate >= startDate);
    const matchEnd = !endDate || (rawDate && rawDate <= endDate);
    const hasCoords = typeof e.lat === 'number' && typeof e.lng === 'number';
    const insidePolygon = !polygonRef.current || !hasCoords || isPointInPolygon(e.lat, e.lng);
    return matchCategory && matchPerson && matchStart && matchEnd && insidePolygon;
  });

  const firstWithCoords = (expenses || []).find(
    (e: any): e is MapExpense & { lat: number; lng: number } => typeof e.lat === 'number' && typeof e.lng === 'number'
  );
  const center = firstWithCoords ? { lat: firstWithCoords.lat, lng: firstWithCoords.lng } : { lat: 44.4268, lng: 26.1025 };

  return (
    <div className="min-h-screen bg-[#FAF8F5] font-sans flex flex-col">
      <nav className="sticky top-0 z-10 bg-[#FAF8F5] border-b border-[#EDE9E3] px-6 lg:px-10 py-4 flex items-center gap-4">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="w-10 h-10 bg-white border border-[#EDE9E3] rounded-[10px] flex items-center justify-center text-[#2D2926] hover:border-[#C4B9AC] transition-colors shadow-sm"
          aria-label="Înapoi"
        >
          <ArrowLeft size={18} />
        </button>
        <h2 className="text-[18px] sm:text-[20px] font-medium text-[#2D2926] tracking-tight leading-tight">Toate Cheltuielile pe Hartă</h2>
      </nav>

      <div className="px-6 lg:px-10 pt-10 pb-20 max-w-[960px] mx-auto w-full flex-1">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-3 mb-8 fade-in-up" style={{ animationDelay: '0.1s' }}>
          <div className="relative">
            <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C]" size={16} />
            <input
              type="date"
              className={`${inputStyle} pl-10`}
              title="Data de început"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
          </div>

          <div className="relative">
            <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C]" size={16} />
            <input
              type="date"
              className={`${inputStyle} pl-10`}
              title="Data de final"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
          </div>

          <div className="relative">
            <select
              className={inputStyle}
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
            >
              <option value="">Toate Categoriile</option>
              {availableCategories.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
            <ChevronDown className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C] pointer-events-none" size={16} />
          </div>

          <div className="relative">
            <select
              className={inputStyle}
              value={selectedPerson}
              onChange={(e) => setSelectedPerson(e.target.value)}
            >
              <option value="">Orice Persoană</option>
              {availablePeople.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
            <ChevronDown className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C] pointer-events-none" size={16} />
          </div>

          <button
            onClick={() => { setStartDate(''); setEndDate(''); setSelectedCategory(''); setSelectedPerson(''); }}
            className="h-10 px-3 bg-white border border-[#EDE9E3] rounded-[10px] text-[13px] font-medium text-[#2D2926] flex items-center justify-center gap-2 hover:border-[#C4B9AC] transition-colors whitespace-nowrap"
          >
            <Filter size={16} /><span>Resetează Filtre</span>
          </button>

          <div className="flex items-center gap-2 flex-wrap col-span-1 md:col-span-5">
            <button
              onClick={() => {
                drawingModeRef.current = 'expense';
                setDrawingEnabled((s) => !s);
                setSafeZoneDrawing(false);
              }}
              className={`h-10 px-3 bg-white border border-[#EDE9E3] rounded-[10px] text-[13px] font-medium text-[#2D2926] flex items-center justify-center gap-2 hover:border-[#C4B9AC] transition-colors ${drawingEnabled ? 'ring-2 ring-[#C4B9AC]' : ''} whitespace-nowrap`}
              title="Desenează poligon pentru filtrare cheltuieli"
            >
              <span>Deseneaza</span>
            </button>

            <button
              onClick={() => {
                if (polygonRef.current) {
                  try { if (polygonRef.current.__listeners) { polygonRef.current.__listeners.forEach((l: any) => l.remove?.()); } } catch {}
                  polygonRef.current.setMap(null);
                  polygonRef.current = null;
                }
              }}
              className="h-10 px-3 bg-white border border-[#EDE9E3] rounded-[10px] text-[13px] font-medium text-[#2D2926] flex items-center justify-center gap-2 hover:border-[#C4B9AC] transition-colors whitespace-nowrap"
              title="Șterge poligon filtrare"
            >
              Sterge
            </button>

            <button
              onClick={() => {
                drawingModeRef.current = 'safezone';
                setSafeZoneDrawing((s) => !s);
                setDrawingEnabled(false);
                setPendingSafeZoneCoords(null);
                if (safeZoneDrawnRef.current) {
                  safeZoneDrawnRef.current.setMap(null);
                  safeZoneDrawnRef.current = null;
                }
              }}
              className={`h-10 px-3 border rounded-[10px] text-[13px] font-medium flex items-center justify-center gap-2 transition-colors whitespace-nowrap ${
                safeZoneDrawing
                  ? 'bg-green-600 border-green-600 text-white'
                  : 'bg-white border-[#EDE9E3] text-[#2D2926] hover:border-green-400'
              }`}
              title="Desenează zona de siguranță"
            >
              <Shield size={15} />
              <span>Zona Siguranta</span>
            </button>

            {pendingSafeZoneCoords && (
              <button
                disabled={safeZoneSaving}
                onClick={async () => {
                  setSafeZoneSaving(true);
                  try {
                    await saveGeofenceZone(pendingSafeZoneCoords);
                    setSavedSafeZone(pendingSafeZoneCoords);
                    setPendingSafeZoneCoords(null);
                    if (safeZoneDrawnRef.current) {
                      safeZoneDrawnRef.current.setMap(null);
                      safeZoneDrawnRef.current = null;
                    }
                  } catch {
                    // ignore
                  } finally {
                    setSafeZoneSaving(false);
                  }
                }}
                className="h-10 px-3 bg-green-600 border border-green-600 text-white rounded-[10px] text-[13px] font-medium flex items-center justify-center gap-2 hover:bg-green-700 transition-colors whitespace-nowrap disabled:opacity-60"
              >
                {safeZoneSaving ? 'Se salvează...' : 'Salveaza Zona'}
              </button>
            )}

            {savedSafeZone && !pendingSafeZoneCoords && (
              <button
                onClick={async () => {
                  try {
                    await deleteGeofenceZone();
                    setSavedSafeZone(null);
                  } catch {
                    // ignore
                  }
                }}
                className="h-10 px-3 bg-white border border-red-200 text-red-500 rounded-[10px] text-[13px] font-medium flex items-center justify-center gap-2 hover:border-red-400 transition-colors whitespace-nowrap"
                title="Șterge zona de siguranță"
              >
                <ShieldOff size={15} />
                <span>Sterge Zona</span>
              </button>
            )}
          </div>
        </div>

        {(expenseLoadError || (!mapsApiKey || loadError)) && (
          <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 shadow-sm text-[13px] text-[#9A8A7C]">
            {expenseLoadError && <div className="mb-2 text-[#2D2926]">{expenseLoadError}</div>}
            Nu s-a putut încărca Google Maps. Verifică cheia și restricțiile ei în Google Cloud Console.
          </div>
        )}

        {isLoadingExpenses && !hasInjectedExpenses && (
          <div className="mb-4 bg-white border border-[#EDE9E3] rounded-[14px] p-4 shadow-sm text-[13px] text-[#9A8A7C]">
            Se încarcă cheltuielile pentru hartă...
          </div>
        )}

        {liveLocation && savedSafeZone && (
          liveLocation.isOutsideGeofence ? (
            <div className="mb-4 flex items-center gap-3 bg-red-50 border border-red-200 rounded-[14px] px-5 py-3 text-[13px] text-red-700 font-medium">
              <Shield size={16} className="text-red-500 shrink-0" />
              <span>Copilul a ieșit din zona de siguranță!</span>
            </div>
          ) : (
            <div className="mb-4 flex items-center gap-3 bg-green-50 border border-green-200 rounded-[14px] px-5 py-3 text-[13px] text-green-700 font-medium">
              <Shield size={16} className="text-green-600 shrink-0" />
              <span>Copilul se află în zona de siguranță.</span>
            </div>
          )
        )}

        {safeZoneDrawing && (
          <div className="mb-4 flex items-center gap-3 bg-green-50 border border-green-200 rounded-[14px] px-5 py-3 text-[13px] text-green-700">
            <Shield size={16} className="text-green-600 shrink-0" />
            <span>Desenează poligonul pe hartă pentru a defini zona de siguranță, apoi apasă <strong>Salveaza Zona</strong>.</span>
          </div>
        )}

        {mapsApiKey && isLoaded && (
          <div className="w-full bg-white border border-[#EDE9E3] rounded-[14px] overflow-hidden shadow-[0_4px_20px_rgba(0,0,0,0.02)]">
            <div className="flex flex-col md:flex-row">
              {selectedExpense && (
                <aside className="md:w-[340px] md:border-r border-[#EDE9E3] p-5">
                  <div>
                    {placePhotoSrc && (
                      <div className="mb-4 overflow-hidden rounded-[12px] border border-[#EDE9E3] bg-[#FAF8F5]">
                        <img src={placePhotoSrc} alt={place?.name || selectedExpense.description || 'Loc'} className="w-full h-[160px] object-cover" loading="lazy" />
                      </div>
                    )}

                    <div className="text-[12px] text-[#9A8A7C]">Locație</div>
                    <div className="mt-1 text-[16px] font-medium text-[#2D2926] leading-snug">{place?.name || selectedExpense.description || selectedExpense.location || 'Locație'}</div>
                    {(place?.formattedAddress || selectedExpense.location) && (
                      <div className="mt-1 text-[13px] text-[#9A8A7C]">{place?.formattedAddress || selectedExpense.location}</div>
                    )}

                    {(typeof place?.rating === 'number' || typeof place?.userRatingCount === 'number') && (
                      <div className="mt-2 text-[13px] text-[#2D2926]">
                        {typeof place?.rating === 'number' ? `Rating: ${place.rating}` : null}
                        {typeof place?.userRatingCount === 'number' ? ` (${place.userRatingCount})` : null}
                      </div>
                    )}

                    {typeof place?.openNow === 'boolean' && (
                      <div className="mt-2 text-[13px]"><span className="font-medium text-[#2D2926]">{place.openNow ? 'Deschis' : 'Închis'}</span></div>
                    )}

                    {place?.weekdayDescriptions?.length ? (
                      <div className="mt-1 text-[12px] text-[#9A8A7C]">
                        {place.weekdayDescriptions.slice(0, 3).map((line: string) => (
                          <div key={line}>{line}</div>
                        ))}
                      </div>
                    ) : null}

                    {place?.phone && <div className="mt-2 text-[13px] text-[#2D2926]">{place.phone}</div>}

                    {place?.websiteUri && (
                      <a href={place.websiteUri} target="_blank" rel="noreferrer" className="mt-3 inline-flex items-center justify-center h-10 px-3 rounded-[10px] bg-white border border-[#EDE9E3] text-[13px] text-[#2D2926] hover:border-[#C4B9AC] transition-colors">Website</a>
                    )}

                    {googleMapsLink && (
                      <a href={googleMapsLink} target="_blank" rel="noreferrer" className="mt-4 inline-flex items-center justify-center h-10 px-3 rounded-[10px] bg-[#2D2926] text-white text-[13px] hover:opacity-90 transition-opacity">Deschide în Google Maps</a>
                    )}

                    {message && <div className="mt-4 text-[12px] text-[#9A8A7C]">{message}</div>}
                  </div>
                </aside>
              )}

              <div className="flex-1">
                <GoogleMap
                  mapContainerStyle={{ width: '100%', height: '600px' }}
                  center={center}
                  zoom={11}
                  options={{ mapTypeControl: false, streetViewControl: false, fullscreenControl: false }}
                >
                  <DrawingManager
                    drawingMode={(drawingEnabled || safeZoneDrawing) && (window as any).google ? (window as any).google.maps.drawing.OverlayType.POLYGON : null}
                    onPolygonComplete={(poly: any) => {
                      if (drawingModeRef.current === 'safezone') {
                        if (safeZoneDrawnRef.current) {
                          safeZoneDrawnRef.current.setMap(null);
                        }
                        poly.setEditable(true);
                        safeZoneDrawnRef.current = poly;
                        const coords: LatLng[] = poly.getPath().getArray().map((p: any) => ({ lat: p.lat(), lng: p.lng() }));
                        setPendingSafeZoneCoords(coords);
                        setSafeZoneDrawing(false);
                      } else {
                        if (polygonRef.current) {
                          try { if (polygonRef.current.__listeners) { polygonRef.current.__listeners.forEach((l: any) => l.remove?.()); } } catch {}
                          polygonRef.current.setMap(null);
                        }
                        poly.setEditable(true);
                        polygonRef.current = poly;
                        const updatePath = () => poly.getPath().getArray().map((p: any) => ({ lat: p.lat(), lng: p.lng() }));
                        const insertListener = poly.getPath().addListener('insert_at', updatePath);
                        const setListener = poly.getPath().addListener('set_at', updatePath);
                        const removeListener = poly.getPath().addListener('remove_at', updatePath);
                        poly.__listeners = [insertListener, setListener, removeListener];
                        updatePath();
                        setDrawingEnabled(false);
                      }
                    }}
                    options={{
                      drawingControl: false,
                      polygonOptions: drawingModeRef.current === 'safezone'
                        ? { fillColor: '#00C853', fillOpacity: 0.12, strokeColor: '#00C853', strokeWeight: 2 }
                        : { fillColor: '#FF0000', fillOpacity: 0.08, strokeWeight: 2 },
                    }}
                  />

                  {savedSafeZone && savedSafeZone.length > 0 && !pendingSafeZoneCoords && (
                    <Polygon
                      paths={savedSafeZone}
                      options={{ fillColor: '#00C853', fillOpacity: 0.08, strokeColor: '#00C853', strokeWeight: 2 }}
                    />
                  )}

                  {/* --- MARKER LOCAȚIE LIVE (COPIL) --- */}
                  {liveLocation && (
                    <Marker
                      position={{ lat: liveLocation.lat, lng: liveLocation.lng }}
                      icon={{
                        url: 'https://maps.google.com/mapfiles/ms/icons/blue-dot.png',
                        scaledSize: new (window as any).google.maps.Size(40, 40)
                      }}
                      title="Locație Live Copil"
                      zIndex={1000}
                    />
                  )}

                  <MarkerClusterer options={{ imagePath: 'https://developers.google.com/maps/documentation/javascript/examples/markerclusterer/m', gridSize: 30 }}>
                    {(clusterer: any) => (
                      <>
                        {filteredExpenses.filter((e: MapExpense): e is MapExpense & { lat: number; lng: number } => typeof e.lat === 'number' && typeof e.lng === 'number').map((e) => (
                          <Marker
                            key={e.id}
                            position={{ lat: e.lat, lng: e.lng }}
                            clusterer={clusterer}
                            title={e.description || e.location || ''}
                            icon={{ url: 'https://maps.google.com/mapfiles/ms/icons/red-dot.png', scaledSize: new (window as any).google.maps.Size(32, 32) }}
                            label={{ text: e.amount ? `${e.amount} RON` : '', className: 'text-xs' }}
                            onClick={() => {
                              setSelectedExpense(e);
                              setPlace(null);
                              setGoogleMapsUri(null);
                              setMessage(null);
                              // fetch nearby place details (best-effort)
                              (async () => {
                                try {
                                  if (!mapsApiKey) return;
                                  const textQuery = (e.location || e.description || '').toString();
                                  const res = await fetch('https://places.googleapis.com/v1/places:searchText', {
                                    method: 'POST',
                                    headers: {
                                      'Content-Type': 'application/json',
                                      'X-Goog-Api-Key': mapsApiKey,
                                      'X-Goog-FieldMask': 'places.id,places.googleMapsUri',
                                    },
                                    body: JSON.stringify({ textQuery, locationBias: { circle: { center: { latitude: e.lat, longitude: e.lng }, radius: 800 } } }),
                                  });
                                  if (!res.ok) return;
                                  const data: any = await res.json();
                                  const first = data?.places?.[0];
                                  if (!first?.id) return;
                                  setPlaceId(first.id);
                                  setGoogleMapsUri(first.googleMapsUri ?? null);
                                  const detRes = await fetch(`https://places.googleapis.com/v1/places/${encodeURIComponent(first.id)}`, {
                                    method: 'GET',
                                    headers: {
                                      'X-Goog-Api-Key': mapsApiKey,
                                      'X-Goog-FieldMask': 'displayName,formattedAddress,phone,websiteUri,googleMapsUri,photos,rating,userRatingCount,regularOpeningHours.openNow,regularOpeningHours.weekdayDescriptions',
                                    },
                                  });
                                  if (!detRes.ok) return;
                                  const details: any = await detRes.json();
                                  setPlace({
                                    formattedAddress: details.formattedAddress,
                                    phone: details.internationalPhoneNumber ?? details.nationalPhoneNumber,
                                    name: details.displayName?.text,
                                    websiteUri: details.websiteUri,
                                    photoName: details?.photos?.[0]?.name,
                                    googleMapsUri: details.googleMapsUri,
                                    rating: details?.rating,
                                    userRatingCount: details?.userRatingCount,
                                    openNow: details?.regularOpeningHours?.openNow,
                                    weekdayDescriptions: details?.regularOpeningHours?.weekdayDescriptions,
                                  });
                                  if (details?.googleMapsUri) setGoogleMapsUri(details.googleMapsUri);
                                } catch {
                                  // ignore
                                }
                              })();
                            }}
                          />
                        ))}
                      </>
                    )}
                  </MarkerClusterer>
                </GoogleMap>
              </div>
            </div>
          </div>
        )}

        <div className="mt-6 text-[13px] text-[#9A8A7C]">
          <MapPin size={14} className="inline mr-1 text-[#D4C9BC]" /> Sunt afișate doar cheltuielile cu coordonate geografice.
        </div>
      </div>
    </div>
  );
}
