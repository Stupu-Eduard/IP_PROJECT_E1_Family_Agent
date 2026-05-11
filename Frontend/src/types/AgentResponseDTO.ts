export interface TextResponse {
    type: 'text';
    text: string;
}

export type ChartType = 'line' | 'bar' | 'pie' | 'area';

export interface ChartDataPoint {
    name: string;
    value: number;
}

export interface ChartResponse {
    type: 'chart';
    chartType: ChartType;
    title?: string;
    data: ChartDataPoint[];
}

export interface MapPin {
    lat: number;
    lng: number;
    label?: string;
    description?: string;
}

export interface MapResponse {
    type: 'map';
    title?: string;
    pins: MapPin[];
    center?: { lat: number; lng: number };
    zoom?: number;
}

export type AgentResponse = TextResponse | ChartResponse | MapResponse;