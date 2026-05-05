import { api } from './api'
import type { CategorySummaryDTO, MemberSummaryDTO, SummaryQueryParams } from '../types/SummaryDTO'

type SummaryApiItem = {
  category?: string | null
  memberName?: string | null
  totalAmount?: number | string | null
  percentage?: number | string | null
  transactionCount?: number | string | null
}

type SummaryApiResponse = {
  data?: SummaryApiItem[]
  items?: SummaryApiItem[]
}

const toNumber = (value: number | string | null | undefined) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const pickItems = (payload: SummaryApiItem[] | SummaryApiResponse): SummaryApiItem[] => {
  if (Array.isArray(payload)) {
    return payload
  }

  if (Array.isArray(payload.data)) {
    return payload.data
  }

  if (Array.isArray(payload.items)) {
    return payload.items
  }

  return []
}

const buildQuery = (groupBy: 'category' | 'member', params?: SummaryQueryParams) => ({
  groupBy,
  from: params?.from,
  to: params?.to,
  category: params?.category,
})

export async function fetchCategorySummary(params?: SummaryQueryParams, signal?: AbortSignal): Promise<CategorySummaryDTO[]> {
  const response = await api.get<SummaryApiItem[] | SummaryApiResponse>('/api/v1/expenses/summary', {
    params: buildQuery('category', params),
    signal,
  })

  return pickItems(response.data).map((item) => ({
    category: item.category?.trim() || 'Fara categorie',
    totalAmount: toNumber(item.totalAmount),
    percentage: toNumber(item.percentage),
  }))
}

export async function fetchMemberSummary(params?: SummaryQueryParams, signal?: AbortSignal): Promise<MemberSummaryDTO[]> {
  const response = await api.get<SummaryApiItem[] | SummaryApiResponse>('/api/v1/expenses/summary', {
    params: buildQuery('member', params),
    signal,
  })

  return pickItems(response.data).map((item) => ({
    memberName: item.memberName?.trim() || 'Necunoscut',
    totalAmount: toNumber(item.totalAmount),
    transactionCount: toNumber(item.transactionCount),
  }))
}

