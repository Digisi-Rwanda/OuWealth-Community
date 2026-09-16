import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from './client'
import {
  billingPlansQueryKey,
  fetchBillingPlans,
  fetchSubscriptionPayments,
  startBillingCheckout,
} from './billing'

vi.mock('./client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const getMock = vi.mocked(apiClient.get)
const postMock = vi.mocked(apiClient.post)

describe('billing API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads plans keyed by cooperative id', async () => {
    getMock.mockResolvedValue({
      data: {
        success: true,
        message: 'ok',
        data: {
          currency: 'RWF',
          trialMonths: 4,
          plans: [
            {
              billingCycle: 'MONTHLY',
              listPrice: '2000.0000',
              amount: '2000.0000',
              discountPercent: 0,
              savings: '0',
              periodMonths: 1,
            },
          ],
        },
      },
    })
    await expect(fetchBillingPlans('coop-1')).resolves.toMatchObject({
      currency: 'RWF',
      trialMonths: 4,
    })
    expect(getMock).toHaveBeenCalledWith('/cooperatives/coop-1/billing/plans')
    expect(billingPlansQueryKey('coop-1')[1]).toBe('coop-1')
  })

  it('loads payment history for the requested cooperative', async () => {
    getMock.mockResolvedValue({
      data: {
        success: true,
        message: 'ok',
        data: {
          content: [
            {
              id: 'pay-1',
              billingCycle: 'MONTHLY',
              paymentChannel: 'CARD',
              status: 'FAILED',
              currency: 'RWF',
              amount: '2000.0000',
              initiatedAt: '2026-09-01T00:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        },
      },
    })
    const page = await fetchSubscriptionPayments('coop-1', { page: 0, size: 20 })
    expect(page.content[0]?.id).toBe('pay-1')
    expect(getMock).toHaveBeenCalledWith('/cooperatives/coop-1/billing/payments', {
      params: { page: 0, size: 20 },
    })
  })

  it('posts checkout without treating the call as completed payment', async () => {
    postMock.mockResolvedValue({ data: { success: true, data: null } })
    await startBillingCheckout('coop-1', { billingCycle: 'ANNUAL', paymentChannel: 'CARD' })
    expect(postMock).toHaveBeenCalledWith('/cooperatives/coop-1/billing/checkout', {
      billingCycle: 'ANNUAL',
      paymentChannel: 'CARD',
    })
  })
})
