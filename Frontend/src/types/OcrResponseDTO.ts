export interface OcrItemDTO {
    name: string;
    quantity?: number;
    unitPrice?: number;
}

export interface OcrResponseDTO {
    amount: number;
    category: string;
    date: string;
    locationName?: string;
    confidence: number;
    receiptUrl?: string;
    items?: OcrItemDTO[];
}