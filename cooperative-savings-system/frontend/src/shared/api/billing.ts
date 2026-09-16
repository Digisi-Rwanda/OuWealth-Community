import axios from 'axios'
import { apiClient } from './client'
import { unwrapApiData } from './auth'
import type { ApiErrorBody, ApiResponse, PageQuery, PageResponse } from '@/shared/types/api'
import type {
  BillingCheckoutRequest,
  BillingPlanQuote,
  BillingPlansResponse,
  SubscriptionPaymentRecord,
} from '@/shared/types/billing'
import { mapBillingPlan, mapSubscriptionPayment } from '@/shared/types/billing'

export const billingPlansQueryKey = (cooperativeId: string) =>
  ['cooperatives', cooperativeId, 'billing', 'plans'] as const

export const billingPaymentsQueryKey = (cooperativeId: string, query: PageQuery = {}) =>
  ['cooperatives', cooperativeId, 'billing', 'payments', query] as const

export async function fetchBillingPlans(cooperativeId: string): Promise<BillingPlansResponse> {
  const response = await apiClient.get<ApiResponse<BillingPlansResponse>>(
    `/cooperatives/${cooperativeId}/billing/plans`,
  )
  const data = unwrapApiData(response.data)
  return {
    currency: data.currency || 'RWF',
    trialMonths: data.trialMonths,
    plans: (data.plans ?? []).map(mapBillingPlan),
  }
}

export async function fetchSubscriptionPayments(
  cooperativeId: string,
  query: PageQuery = {},
): Promise<PageResponse<SubscriptionPaymentRecord>> {
  const params: Record<string, string | number> = {}
  if (query.page != null) params.page = query.page
  if (query.size != null) params.size = query.size
  if (query.sort) params.sort = query.sort
  const response = await apiClient.get<ApiResponse<PageResponse<SubscriptionPaymentRecord>>>(
    `/cooperatives/${cooperativeId}/billing/payments`,
    { params },
  )
  const page = unwrapApiData(response.data)
  return {
    ...page,
    content: (page.content ?? []).map(mapSubscriptionPayment),
  }
}

/**
 * Phase 6 will start provider checkout. Phase 5 validates the payload and returns
 * PAYMENT_INTEGRATION_UNAVAILABLE without charging or activating a subscription.
 */
export async function startBillingCheckout(
  cooperativeId: string,
  payload: BillingCheckoutRequest,
): Promise<void> {
  await apiClient.post(`/cooperatives/${cooperativeId}/billing/checkout`, payload)
}

export function isPaymentIntegrationUnavailableError(error: unknown): boolean {
  if (!axios.isAxiosError(error)) return false
  const data = error.response?.data as ApiErrorBody | undefined
  return error.response?.status === 501 || data?.code === 'PAYMENT_INTEGRATION_UNAVAILABLE'
}

export function planByCycle(
  plans: BillingPlanQuote[] | undefined,
  cycle: BillingPlanQuote['billingCycle'],
): BillingPlanQuote | undefined {
  return plans?.find((plan) => plan.billingCycle === cycle)
}
