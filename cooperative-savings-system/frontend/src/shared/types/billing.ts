export type BillingCycle = 'MONTHLY' | 'ANNUAL'
export type BillingPaymentChannel = 'MTN_MOMO' | 'CARD'
export type SubscriptionPaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELED'

export interface BillingPlanQuote {
  billingCycle: BillingCycle
  listPrice: string | number
  amount: string | number
  discountPercent: string | number
  savings: string | number
  periodMonths: number
}

export interface BillingPlansResponse {
  currency: string
  trialMonths: number
  plans: BillingPlanQuote[]
}

export interface BillingCheckoutRequest {
  billingCycle: BillingCycle
  paymentChannel: BillingPaymentChannel
}

export interface SubscriptionPaymentRecord {
  id: string
  billingCycle: BillingCycle
  paymentChannel: BillingPaymentChannel
  status: SubscriptionPaymentStatus
  currency: string
  amount: string | number
  provider?: string | null
  externalReference?: string | null
  initiatedAt: string
  paidAt?: string | null
  failedAt?: string | null
}

export function mapBillingPlan(raw: BillingPlanQuote): BillingPlanQuote {
  return {
    billingCycle: raw.billingCycle,
    listPrice: raw.listPrice,
    amount: raw.amount,
    discountPercent: raw.discountPercent,
    savings: raw.savings,
    periodMonths: raw.periodMonths,
  }
}

export function mapSubscriptionPayment(raw: SubscriptionPaymentRecord): SubscriptionPaymentRecord {
  return {
    id: String(raw.id),
    billingCycle: raw.billingCycle,
    paymentChannel: raw.paymentChannel,
    status: raw.status,
    currency: raw.currency || 'RWF',
    amount: raw.amount,
    provider: raw.provider ?? null,
    externalReference: raw.externalReference ?? null,
    initiatedAt: raw.initiatedAt,
    paidAt: raw.paidAt ?? null,
    failedAt: raw.failedAt ?? null,
  }
}
