export interface OcrTransactionDTO {
    date: string;
    description: string;
    amount: number;
    currency: string;
    type: string;
}

export interface OcrResponseDTO {
    transactions: OcrTransactionDTO[];
    count: number;
}
