import React from 'react';
import {
    ResponsiveContainer,
    LineChart, Line,
    BarChart, Bar,
    AreaChart, Area,
    PieChart, Pie, Cell,
    XAxis, YAxis, CartesianGrid, Tooltip,
} from 'recharts';
import { GoogleMap, Marker, InfoWindow, useJsApiLoader } from '@react-google-maps/api';
import { AlertCircle } from 'lucide-react';
import type {
    AgentResponse,
    ChartResponse,
    MapResponse,
    ChartDataPoint,
    MapPin,
} from '../types/AgentResponseDTO';

export function isTextResponse(r: unknown): r is { type: 'text'; text: string } {
    return !!r && typeof r === 'object'
        && (r as { type?: unknown }).type === 'text'
        && typeof (r as { text?: unknown }).text === 'string';
}

export function isChartResponse(r: unknown): r is ChartResponse {
    if (!r || typeof r !== 'object') return false;
    const obj = r as { type?: unknown; data?: unknown; chartType?: unknown };
    if (obj.type !== 'chart') return false;
    if (!Array.isArray(obj.data) || obj.data.length === 0) return false;
    if (typeof obj.chartType !== 'string') return false;
    return ['line', 'bar', 'pie', 'area'].includes(obj.chartType);
}

export function isMapResponse(r: unknown): r is MapResponse {
    if (!r || typeof r !== 'object') return false;
    const obj = r as { type?: unknown; pins?: unknown };
    if (obj.type !== 'map') return false;
    return Array.isArray(obj.pins) && obj.pins.length > 0;
}

interface FallbackProps {
    message?: string;
}

export const AgentFallback: React.FC<FallbackProps> = ({ message }) => (
    <div
        data-testid="agent-fallback"
        style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '8px 11px',
            borderRadius: 9,
            background: 'rgba(201, 123, 75, 0.08)',
            border: '1px dashed var(--color-border)',
            color: 'var(--color-muted)',
            fontSize: 12.5,
            lineHeight: 1.5,
        }}
    >
        <AlertCircle size={14} style={{ flexShrink: 0, color: 'var(--color-primary)' }} />
        <span>{message || 'Nu am putut afișa răspunsul vizual. Date lipsă sau format necunoscut.'}</span>
    </div>
);


const CHART_PALETTE = ['#C97B4B', '#2D2926', '#9A8A7C', '#4CAF7D', '#B8A99A', '#EDE9E3'];

interface InlineChartProps {
    response: ChartResponse;
}

export const InlineChart: React.FC<InlineChartProps> = ({ response }) => {
    const { chartType, title, data } = response;

    const tooltipStyle = {
        contentStyle: {
            borderRadius: 8,
            border: '1px solid #EDE9E3',
            boxShadow: '0 4px 12px rgba(0,0,0,0.06)',
            backgroundColor: '#fff',
            fontSize: 11,
            padding: '4px 8px',
        },
        itemStyle: { color: '#C97B4B', fontWeight: 500, fontSize: 11 },
        labelStyle: { color: '#9A8A7C', fontSize: 10, marginBottom: 2 },
    };

    const renderChart = () => {
        switch (chartType) {
            case 'line':
                return (
                    <LineChart data={data as ChartDataPoint[]} margin={{ top: 8, right: 8, left: -22, bottom: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#EDE9E3" />
                        <XAxis dataKey="name" tick={{ fontSize: 10, fill: '#B8A99A' }} axisLine={false} tickLine={false} />
                        <YAxis tick={{ fontSize: 10, fill: '#B8A99A' }} axisLine={false} tickLine={false} />
                        <Tooltip {...tooltipStyle} />
                        <Line type="monotone" dataKey="value" stroke="#C97B4B" strokeWidth={2} dot={{ r: 3, fill: '#C97B4B' }} activeDot={{ r: 5 }} />
                    </LineChart>
                );

            case 'bar':
                return (
                    <BarChart data={data as ChartDataPoint[]} margin={{ top: 8, right: 8, left: -22, bottom: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#EDE9E3" />
                        <XAxis dataKey="name" tick={{ fontSize: 10, fill: '#B8A99A' }} axisLine={false} tickLine={false} />
                        <YAxis tick={{ fontSize: 10, fill: '#B8A99A' }} axisLine={false} tickLine={false} />
                        <Tooltip {...tooltipStyle} />
                        <Bar dataKey="value" fill="#C97B4B" radius={[4, 4, 0, 0]} />
                    </BarChart>
                );

            case 'area':
                return (
                    <AreaChart data={data as ChartDataPoint[]} margin={{ top: 8, right: 8, left: -22, bottom: 0 }}>
                        <defs>
                            <linearGradient id="inlineChartGradient" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#C97B4B" stopOpacity={0.35} />
                                <stop offset="95%" stopColor="#C97B4B" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#EDE9E3" />
                        <XAxis dataKey="name" tick={{ fontSize: 10, fill: '#B8A99A' }} axisLine={false} tickLine={false} />
                        <YAxis tick={{ fontSize: 10, fill: '#B8A99A' }} axisLine={false} tickLine={false} />
                        <Tooltip {...tooltipStyle} />
                        <Area type="monotone" dataKey="value" stroke="#C97B4B" strokeWidth={2} fill="url(#inlineChartGradient)" />
                    </AreaChart>
                );

            case 'pie':
                return (
                    <PieChart>
                        <Tooltip {...tooltipStyle} />
                        <Pie data={data as ChartDataPoint[]} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={55} innerRadius={28}>
                            {(data as ChartDataPoint[]).map((_, idx) => (
                                <Cell key={`cell-${idx}`} fill={CHART_PALETTE[idx % CHART_PALETTE.length]} />
                            ))}
                        </Pie>
                    </PieChart>
                );
        }
    };

    return (
        <div
            data-testid="inline-chart"
            style={{
                width: '100%',
                background: '#fff',
                borderRadius: 9,
                padding: '8px 6px 6px',
                border: '1px solid var(--color-border)',
            }}
        >
            {title && (
                <div
                    style={{
                        fontSize: 11,
                        fontWeight: 500,
                        color: 'var(--color-ink)',
                        padding: '0 6px 6px',
                        borderBottom: '1px solid var(--color-border)',
                        marginBottom: 4,
                    }}
                >
                    {title}
                </div>
            )}
            <div style={{ width: '100%', height: 160 }}>
                <ResponsiveContainer width="100%" height="100%">
                    {renderChart() as React.ReactElement}
                </ResponsiveContainer>
            </div>
        </div>
    );
};


interface InlineMapProps {
    response: MapResponse;
}

export const InlineMap: React.FC<InlineMapProps> = ({ response }) => {
    const { title, pins, center, zoom } = response;
    const [openIdx, setOpenIdx] = React.useState<number | null>(null);

    const mapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined;

    const { isLoaded, loadError } = useJsApiLoader({
        id: 'family-agent-google-maps',
        googleMapsApiKey: mapsApiKey ?? '',
    });

    const resolvedCenter = center ?? { lat: pins[0].lat, lng: pins[0].lng };
    const resolvedZoom = zoom ?? (pins.length === 1 ? 14 : 11);

    if (!mapsApiKey) {
        return <AgentFallback message="Lipsește cheia Google Maps pentru afișarea hărții." />;
    }

    if (loadError) {
        return <AgentFallback message="Nu s-a putut încărca harta. Verifică conexiunea." />;
    }

    if (!isLoaded) {
        return (
            <div
                data-testid="inline-map-loading"
                style={{
                    padding: '8px 11px',
                    fontSize: 11.5,
                    color: 'var(--color-muted)',
                    background: '#fff',
                    border: '1px solid var(--color-border)',
                    borderRadius: 9,
                }}
            >
                Se încarcă harta...
            </div>
        );
    }

    return (
        <div
            data-testid="inline-map"
            style={{
                width: '100%',
                background: '#fff',
                borderRadius: 9,
                padding: 6,
                border: '1px solid var(--color-border)',
            }}
        >
            {title && (
                <div
                    style={{
                        fontSize: 11,
                        fontWeight: 500,
                        color: 'var(--color-ink)',
                        padding: '2px 4px 6px',
                        borderBottom: '1px solid var(--color-border)',
                        marginBottom: 4,
                    }}
                >
                    {title}
                </div>
            )}
            <div style={{ width: '100%', height: 180, borderRadius: 7, overflow: 'hidden' }}>
                <GoogleMap
                    mapContainerStyle={{ width: '100%', height: '100%' }}
                    center={resolvedCenter}
                    zoom={resolvedZoom}
                    options={{
                        mapTypeControl: false,
                        streetViewControl: false,
                        fullscreenControl: false,
                        zoomControl: true,
                    }}
                >
                    {pins.map((p: MapPin, idx: number) => (
                        <Marker
                            key={`pin-${idx}`}
                            position={{ lat: p.lat, lng: p.lng }}
                            onClick={() => setOpenIdx(idx)}
                        />
                    ))}
                    {openIdx !== null && pins[openIdx] && (
                        <InfoWindow
                            position={{ lat: pins[openIdx].lat, lng: pins[openIdx].lng }}
                            onCloseClick={() => setOpenIdx(null)}
                        >
                            <div style={{ maxWidth: 180, fontSize: 12 }}>
                                {pins[openIdx].label && (
                                    <div style={{ fontWeight: 600, marginBottom: 2 }}>{pins[openIdx].label}</div>
                                )}
                                {pins[openIdx].description && (
                                    <div style={{ color: '#6b7280', fontSize: 11 }}>{pins[openIdx].description}</div>
                                )}
                            </div>
                        </InfoWindow>
                    )}
                </GoogleMap>
            </div>
        </div>
    );
};

interface AgentResponseRendererProps {
    response: AgentResponse | null | undefined;
}

const AgentResponseRenderer: React.FC<AgentResponseRendererProps> = ({ response }) => {
    // Lipsă date complet
    if (!response) {
        return <AgentFallback message="Răspunsul agentului este gol sau lipsește." />;
    }

    // Discriminare pe câmpul `type`
    switch ((response as { type?: string }).type) {
        case 'text':
            if (!isTextResponse(response)) {
                return <AgentFallback message="Răspuns text malformat." />;
            }
            return <span data-testid="agent-text">{response.text}</span>;

        case 'chart':
            if (!isChartResponse(response)) {
                return <AgentFallback message="Date insuficiente pentru afișarea graficului." />;
            }
            return <InlineChart response={response} />;

        case 'map':
            if (!isMapResponse(response)) {
                return <AgentFallback message="Date insuficiente pentru afișarea hărții." />;
            }
            return <InlineMap response={response} />;

        default:
            return <AgentFallback message="Tip de răspuns necunoscut." />;
    }
};

export default AgentResponseRenderer;