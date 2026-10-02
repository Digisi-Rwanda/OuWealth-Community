import type { ContributionStatus } from '@/shared/types/contribution'
import { normalizeDecimalString } from '@/shared/utils/formatMoney'

export type ChipColor =
  | 'default'
  | 'primary'
  | 'secondary'
  | 'error'
  | 'info'
  | 'success'
  | 'warning'

export function contributionStatusColor(status: string): ChipColor {
  switch (status) {
    case 'PAID':
    case 'APPROVED':
      return 'success'
    case 'PARTIALLY_PAID':
      return 'warning'
    case 'PENDING':
      return 'info'
    case 'WAIVED':
      return 'default'
    case 'CANCELLED':
    case 'REJECTED':
      return 'error'
    case 'ACTIVE':
      return 'success'
    case 'DRAFT':
      return 'default'
    case 'CLOSED':
      return 'secondary'
    default:
      return 'default'
  }
}

export function contributionStatusLabelKey(status: string): string {
  return `contributions.status.${status}`
}

/** Compare decimal money strings/numbers without relying on binary float equality. */
export function moneyToScaledInt(value: string | number, scale = 4): bigint {
  const normalized = normalizeDecimalString(value)
  const sign = normalized.startsWith('-') ? -1n : 1n
  const raw = sign === -1n ? normalized.slice(1) : normalized
  const [whole = '0', fraction = ''] = raw.split('.')
  const padded = fraction.padEnd(scale, '0').slice(0, scale)
  return sign * (BigInt(whole || '0') * 10n ** BigInt(scale) + BigInt(padded || '0'))
}

export function computeOutstandingAmount(
  expected: string | number,
  paid: string | number,
): string {
  const scale = 4
  const outstanding = moneyToScaledInt(expected, scale) - moneyToScaledInt(paid || 0, scale)
  if (outstanding <= 0n) return '0'
  const negative = outstanding < 0n
  const abs = negative ? -outstanding : outstanding
  const divisor = 10n ** BigInt(scale)
  const whole = abs / divisor
  const fraction = abs % divisor
  const fractionStr = fraction.toString().padStart(scale, '0').replace(/0+$/, '')
  const body = fractionStr ? `${whole}.${fractionStr}` : `${whole}`
  return negative ? `-${body}` : body
}

export function deriveContributionStatus(
  expected: string | number,
  paid: string | number,
  current?: string,
): ContributionStatus {
  if (current === 'WAIVED' || current === 'CANCELLED') {
    return current
  }
  const paidScaled = moneyToScaledInt(paid || 0)
  if (paidScaled <= 0n) return 'PENDING'
  if (paidScaled >= moneyToScaledInt(expected || 0)) return 'PAID'
  return 'PARTIALLY_PAID'
}

export type ContributionTab =
  | 'monthly'
  | 'submit'
  | 'approvals'
  | 'share-approvals'
  | 'history'
  | 'special'

/**
 * Tabs of the Contributions page. `share-approvals` (Share Purchase Approvals) is its own direct view and is
 * only offered to users who may review share purchases (`canReviewShares`, i.e. the existing
 * selectCanReviewSharePurchases rule); it is never added for users who cannot record contributions.
 */
export function contributionTabsForUser(
  canRecord: boolean,
  isSuperAdmin: boolean,
  canReviewShares = false,
): ContributionTab[] {
  const shareApprovals: ContributionTab[] = canRecord && canReviewShares ? ['share-approvals'] : []
  if (isSuperAdmin) {
    return ['monthly', 'approvals', ...shareApprovals, 'history', 'special']
  }
  if (canRecord) {
    return ['monthly', 'submit', 'approvals', ...shareApprovals, 'history', 'special']
  }
  return ['submit', 'history', 'special']
}

export function isNonNegativeMoney(value: string): boolean {
  const trimmed = value.trim()
  if (!trimmed) return false
  if (!/^\d+(\.\d+)?$/.test(trimmed)) return false
  try {
    return moneyToScaledInt(trimmed) >= 0n
  } catch {
    return false
  }
}
