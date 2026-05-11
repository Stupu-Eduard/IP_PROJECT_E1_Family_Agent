
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
import { GoogleMap, Marker, useJsApiLoader, DrawingManager, MarkerClusterer } from '@react-google-maps/api';
import { ArrowLeft, MapPin, Calendar, ChevronDown, Filter } from 'lucide-react';
import { fetchExpenses } from '../services/expenses';
import { updateLocationCoordinates } from '../services/locations';
import { fetchCategoryNames, fetchUserNames } from '../services/lookups';

type MapExpense = {
  id: number
  locationId?: number
  lat?: number
  lng?: number
  amount?: number
  category?: string
  person?: string
  description?: string
  location?: string
  locationStore?: string
  locationAddress?: string
  locationCity?: string
  locationCountry?: string
  rawDate?: string
}

const isNonGeographicLabel = (label: string) => {
  const normalized = label.trim().toLowerCase();
  return normalized === 'online' || normalized === 'fără locație' || normalized === 'fara locatie';
};

const getClusterKey = (cluster: any) => {
  const center = cluster?.getCenter?.();
  if (!center) return '';
  const lat = typeof center.lat === 'function' ? center.lat() : center.lat;
  const lng = typeof center.lng === 'function' ? center.lng() : center.lng;
  if (typeof lat !== 'number' || typeof lng !== 'number') return '';
  return `${lat.toFixed(5)},${lng.toFixed(5)}`;
};

const buildGeocodeAddress = (expense: MapExpense) => {
  const parts = [
    expense.locationStore,
    expense.locationAddress,
    expense.locationCity,
    expense.locationCountry,
  ].filter(Boolean);

  if (parts.length) return parts.join(', ');

  return '';
};

const mapApiExpenseToMapExpense = (expense: any): MapExpense => {
  const isoDate = expense.expenseDate ?? ''
  const datePart = isoDate ? isoDate.slice(0, 10) : ''
  const location = [expense.location?.store, expense.location?.address, expense.location?.city, expense.location?.country]
    .filter(Boolean)
    .join(', ') || 'Fără locație'
  const amountNumber = typeof expense.amount === 'number' ? expense.amount : Number(expense.amount)

  return {
    id: expense.id,
    locationId: expense.location?.id ?? undefined,
    lat: expense.location?.lat ?? undefined,
    lng: expense.location?.lng ?? undefined,
    amount: Number.isFinite(amountNumber) ? amountNumber : 0,
    category: expense.category ?? 'Fără categorie',
    person: expense.person ?? 'N/A',
    description: expense.description ?? '',
    location,
    locationStore: expense.location?.store ?? undefined,
    locationAddress: expense.location?.address ?? undefined,
    locationCity: expense.location?.city ?? undefined,
    locationCountry: expense.location?.country ?? undefined,
    rawDate: datePart,
  }
}

export default function ExpensesMapAll() {
  const navigate = useNavigate();
  const location = useLocation();

  const locationState = (location.state || {}) as any;
  const hasInjectedExpenses = Object.prototype.hasOwnProperty.call(locationState, 'expenses');
  const injectedExpenses = (hasInjectedExpenses ? locationState.expenses : []) as MapExpense[];
  const { filters = {} } = locationState;
  const [remoteExpenses, setRemoteExpenses] = useState<MapExpense[]>(() => (hasInjectedExpenses ? injectedExpenses : []));
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
    const controller = new AbortController();
    let cancelled = false;

    const run = async () => {
      setIsLoadingExpenses(true);
      setExpenseLoadError(null);

      try {
        const data = await fetchExpenses({}, controller.signal);
        if (cancelled) return;
        const mapped = data.map(mapApiExpenseToMapExpense);
        if (mapped.length > 0 || !hasInjectedExpenses) {
          setRemoteExpenses(mapped);
        }
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

  // polygon drawing state
  const polygonRef = useRef<any>(null);
  const [drawingEnabled, setDrawingEnabled] = useState(false);
  const markerExpenseMapRef = useRef<Map<any, MapExpense>>(new Map());
  const lastClusterClickRef = useRef<{ key: string; time: number } | null>(null);

  // selected expense / place details for left panel
  const [selectedExpense, setSelectedExpense] = useState<any | null>(null);
  const [place, setPlace] = useState<any | null>(null);
  const [placeId, setPlaceId] = useState<string | null>(null);
  const [googleMapsUri, setGoogleMapsUri] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const mapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined;
  const geocodingApiKey = (import.meta.env.VITE_GOOGLE_GEOCODING_API_KEY as string | undefined) ?? mapsApiKey;
  const { isLoaded, loadError } = useJsApiLoader({
    id: 'family-agent-google-maps',
    googleMapsApiKey: mapsApiKey ?? '',
    libraries: ['drawing', 'geometry'],
  });

  useEffect(() => {
    return () => {
      try {
        if (polygonRef.current) {
          // detach listeners if any
          const poly: any = polygonRef.current;
          if (poly.__listeners) {
            poly.__listeners.forEach((l: any) => l.remove?.());
            poly.__listeners = null;
          }
          poly.setMap(null);
          polygonRef.current = null;
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

  const getMostCommonLocation = (markers: any[]) => {
    const counts = new Map<string, number>();
    markers.forEach((marker) => {
      const expense = markerExpenseMapRef.current.get(marker);
      const loc = expense?.location;
      if (!loc) return;
      counts.set(loc, (counts.get(loc) ?? 0) + 1);
    });
    let best: string | null = null;
    let bestCount = 0;
    counts.forEach((count, loc) => {
      if (count > bestCount) {
        best = loc;
        bestCount = count;
      }
    });
    return best;
  };

  const handleClusterClick = (cluster: any) => {
    const now = Date.now();
    const key = getClusterKey(cluster);
    if (!key) return;
    const last = lastClusterClickRef.current;
    lastClusterClickRef.current = { key, time: now };

    if (last && last.key === key && now - last.time < 800) {
      const markers = cluster?.getMarkers?.() ?? [];
      const locationLabel = getMostCommonLocation(markers);
      if (!locationLabel || isNonGeographicLabel(locationLabel)) return;
      navigate('/expenses', { state: { filters: { selectedLocation: locationLabel } } });
    }
  };

  // Geocode missing lat/lng for expenses
  const [geocodedExpenses, setGeocodedExpenses] = useState<any[]>([]);
  const expenses = remoteExpenses;

  useEffect(() => {
    if (!geocodingApiKey || !expenses.length) return;
    let cancelled = false;
    (async () => {
      const results = await Promise.all(
        expenses.map(async (e: any) => {
          if (typeof e.lat === 'number' && typeof e.lng === 'number') return e;
          if (!e.locationId) return e;
          const addr = buildGeocodeAddress(e);
          if (!addr || isNonGeographicLabel(addr)) return e;
          const geo = await geocodeAddress(addr, geocodingApiKey);
          if (geo && e.locationId) {
            try {
              await updateLocationCoordinates(e.locationId, geo.lat, geo.lng);
            } catch {
              // ignore save failures
            }
          }
          if (geo) return { ...e, lat: geo.lat, lng: geo.lng, _geocoded: true };
          return e;
        })
      );
      if (!cancelled) setGeocodedExpenses(results);
    })();
    return () => { cancelled = true; };
  }, [expenses, geocodingApiKey]);

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
      <nav className="sticky top-0 z-10 bg-[#FAF8F5] border-b border-[#EDE9E3] py-4">
        <div className="w-full max-w-[1200px] mx-auto px-6 lg:px-10 flex items-center gap-4">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="w-10 h-10 bg-white border border-[#EDE9E3] rounded-[10px] flex items-center justify-center text-[#2D2926] hover:border-[#C4B9AC] transition-colors shadow-sm"
            aria-label="Înapoi"
          >
            <ArrowLeft size={18} />
          </button>
          <h2 className="text-[18px] sm:text-[20px] font-medium text-[#2D2926] tracking-tight leading-tight">Toate Cheltuielile pe Hartă</h2>
        </div>
      </nav>

      <div className="pt-10 pb-20">
        <div className="max-w-[1200px] mx-auto w-full flex-1 px-6 lg:px-10">
        <div className="grid grid-cols-1 md:grid-cols-7 gap-3 mb-8 fade-in-up" style={{ animationDelay: '0.1s' }}>
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
            className="h-10 w-full px-3 bg-white border border-[#EDE9E3] rounded-[10px] text-[13px] font-medium text-[#2D2926] flex items-center justify-center gap-2 hover:border-[#C4B9AC] transition-colors whitespace-nowrap"
            title="Resetează filtrele"
          >
            <Filter size={16} /><span>Resetează Filtre</span>
          </button>

          <button
            onClick={() => setDrawingEnabled((s) => !s)}
            className={`h-10 w-full px-3 bg-white border border-[#EDE9E3] rounded-[10px] text-[13px] font-medium text-[#2D2926] flex items-center justify-center gap-2 hover:border-[#C4B9AC] transition-colors ${drawingEnabled ? 'ring-2 ring-[#C4B9AC]' : ''} whitespace-nowrap`}
            title="Desenează poligon"
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
            className="h-10 w-full px-3 bg-white border border-[#EDE9E3] rounded-[10px] text-[13px] font-medium text-[#2D2926] flex items-center justify-center gap-2 hover:border-[#C4B9AC] transition-colors whitespace-nowrap"
            title="Șterge poligon"
          >
            Sterge
          </button>
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
                  mapContainerStyle={{ width: '100%', height: '700px' }}
                  center={center}
                  zoom={11}
                  options={{ mapTypeControl: false, streetViewControl: false, fullscreenControl: false }}
                >
                  <DrawingManager
                    drawingMode={drawingEnabled && (window as any).google ? (window as any).google.maps.drawing.OverlayType.POLYGON : null}
                    onPolygonComplete={(poly: any) => {
                      // remove previous polygon
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
                    }}
                    options={{ drawingControl: false, polygonOptions: { fillColor: '#FF0000', fillOpacity: 0.08, strokeWeight: 2 } }}
                  />

                  <MarkerClusterer
                    onClick={handleClusterClick}
                    options={{ imagePath: 'https://developers.google.com/maps/documentation/javascript/examples/markerclusterer/m', gridSize: 30 }}
                  >
                    {(clusterer: any) => (
                      <>
                        {filteredExpenses.filter((e: MapExpense): e is MapExpense & { lat: number; lng: number } => typeof e.lat === 'number' && typeof e.lng === 'number').map((e) => (
                          <Marker
                            key={e.id}
                            position={{ lat: e.lat, lng: e.lng }}
                            clusterer={clusterer}
                            title={e.description || e.location || ''}
                            label={{ text: e.amount ? `${e.amount} RON` : '', className: 'text-xs' }}
                            onLoad={(marker) => {
                              markerExpenseMapRef.current.set(marker, e);
                            }}
                            onUnmount={(marker) => {
                              markerExpenseMapRef.current.delete(marker);
                            }}
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
    </div>
  );
}
