export interface SummaryPeriodDTO {
  from: string
  to: string
}

export interface CategorySummaryDTO {
  category: string
  totalAmount: number
  percentage: number
}

export interface MemberSummaryDTO {
  memberName: string
  totalAmount: number
  transactionCount: number
}

export interface SummaryQueryParams {
  from?: string
  to?: string
  category?: string
}

