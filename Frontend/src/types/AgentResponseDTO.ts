// src/types/AgentResponseDTO.ts

export interface TextResponse {
    type: 'text'
    text?: string      // răspuns text simplu
    message?: string   // fallback — backend-ul pune uneori textul în message
}

export interface ChartDataPoint {
    name: string
    value?: number
    [key: string]: unknown  // pentru multi-series (ex: { name: 'Ian', Teodor: 120, Maria: 80 })
}

export type ChartType = 'bar' | 'line' | 'pie' | 'area'

export interface ChartPayload {
    chartType: ChartType
    title?: string
    data: ChartDataPoint[]
    dataKeys?: string[]   // seriile (ex: ["value"] sau ["Teodor", "Maria"])
    xAxisKey?: string     // cheia pentru axa X (ex: "name")
}

export interface ChartResponse {
    type: 'chart'
    message?: string      // mesajul explicativ generat de backend
    payload: ChartPayload
}

export interface MapPin {
    lat: number
    lng: number
    label?: string
    description?: string
}

export interface MapResponse {
    type: 'map'
    title?: string
    pins: MapPin[]
    center?: { lat: number; lng: number }
    zoom?: number
}

export type AgentResponse = TextResponse | ChartResponse | MapResponse