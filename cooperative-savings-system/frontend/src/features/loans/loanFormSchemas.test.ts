import { describe, expect, it } from 'vitest'
import {
  loanRequestDefaults,
  loanSettingsDefaults,
  toLoanCreatePayload,
  toLoanSettingsPayload,
} from './loanFormSchemas'

describe('toLoanCreatePayload', () => {
  it('sends term without borrower prorata or client-calculated totals', () => {
    const payload = toLoanCreatePayload(
      {
        ...loanRequestDefaults,
        amount: '300000',
        termMonths: '5',
        purpose: 'Business',
      },
      false,
    )
    expect(payload).toEqual({
      amount: '300000',
      purpose: 'Business',
      notes: undefined,
      guaranteeMode: 'SELF',
      termMonths: 5,
    })
    expect(payload).not.toHaveProperty('prorataEnabled')
    expect(payload).not.toHaveProperty('totalInterest')
    expect(payload).not.toHaveProperty('equalInstallmentAmount')
  })
})

describe('toLoanSettingsPayload', () => {
  it('sends cooperative repayment and penalty policy', () => {
    const payload = toLoanSettingsPayload(
      {
        ...loanSettingsDefaults,
        interestRatePercent: '2',
        repaymentDateModel: 'MONTH_END',
        loanPenaltyEnabled: true,
        penaltyType: 'PERCENTAGE_OF_OVERDUE_INSTALLMENT',
        penaltyRateOrAmount: '5',
        penaltyFrequency: 'DAILY',
        gracePeriodDays: '3',
        allocationOrder: ['INTEREST', 'PENALTY', 'PRINCIPAL'],
      },
      false,
    )
    expect(payload.repaymentDateModel).toBe('MONTH_END')
    expect(payload.loanPenaltyEnabled).toBe(true)
    expect(payload.allocationOrder).toEqual(['INTEREST', 'PENALTY', 'PRINCIPAL'])
    expect(payload).not.toHaveProperty('shareTiers')
  })
})
