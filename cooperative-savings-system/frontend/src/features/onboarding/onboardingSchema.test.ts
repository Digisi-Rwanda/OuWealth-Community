import { describe, expect, it } from 'vitest'
import {
  onboardingDefaults,
  onboardingSchema,
  toOnboardingPayload,
} from './onboardingSchema'

const valid: typeof onboardingDefaults = {
  ...onboardingDefaults,
  name: '  Public Scheme  ',
  registrationNumber: ' rca / 2024 / 0123 ',
  contactEmail: 'Scheme@Example.COM',
  contactPhone: '+250 781 234 567',
  address: '  Kigali  ',
  financialYearStartMonth: 3,
  monthlyContributionAmount: '5000',
  contributionDueDay: 10,
  registrationDate: '2024-01-15',
  firstName: 'Pat',
  lastName: 'President',
  username: 'pat.president',
  email: 'Pat@Example.COM',
  phone: '0781112233',
  password: 'SignupPass1!',
  confirmPassword: 'SignupPass1!',
}

describe('onboardingSchema', () => {
  it('accepts a valid public onboarding form', async () => {
    await expect(onboardingSchema.validate(valid)).resolves.toMatchObject({
      name: 'Public Scheme',
      firstName: 'Pat',
    })
  })

  it('requires saving scheme name', async () => {
    await expect(onboardingSchema.validate({ ...valid, name: '' })).rejects.toThrow(/required/i)
  })

  it('rejects invalid registration numbers', async () => {
    await expect(
      onboardingSchema.validate({ ...valid, registrationNumber: 'ab' }),
    ).rejects.toThrow(/registration number/i)
  })

  it('rejects non-Rwandan cooperative phones', async () => {
    await expect(
      onboardingSchema.validate({ ...valid, contactPhone: '12345' }),
    ).rejects.toThrow(/Rwandan mobile/i)
  })

  it('requires creator first name', async () => {
    await expect(onboardingSchema.validate({ ...valid, firstName: '' })).rejects.toThrow(/required/i)
  })

  it('rejects a short password', async () => {
    await expect(
      onboardingSchema.validate({ ...valid, password: 'short', confirmPassword: 'short' }),
    ).rejects.toThrow(/8 characters/i)
  })

  it('requires matching passwords', async () => {
    await expect(
      onboardingSchema.validate({ ...valid, confirmPassword: 'OtherPass1!' }),
    ).rejects.toThrow(/match/i)
  })
})

describe('toOnboardingPayload', () => {
  it('builds cooperative and creator groups without role or subscription overrides', () => {
    const payload = toOnboardingPayload(valid)

    expect(payload).toEqual({
      cooperative: {
        name: 'Public Scheme',
        registrationNumber: 'RCA/2024/0123',
        contactEmail: 'scheme@example.com',
        contactPhone: '0781234567',
        address: 'Kigali',
        currency: 'RWF',
        financialYearStartMonth: 3,
        monthlyContributionAmount: '5000',
        contributionDueDay: 10,
        registrationDate: '2024-01-15',
      },
      creator: {
        firstName: 'Pat',
        lastName: 'President',
        username: 'pat.president',
        email: 'pat@example.com',
        phone: '0781112233',
        password: 'SignupPass1!',
      },
    })
    expect(payload).not.toHaveProperty('role')
    expect(payload.cooperative).not.toHaveProperty('subscriptionInitialization')
    expect(payload.cooperative).not.toHaveProperty('status')
    expect(payload.creator).not.toHaveProperty('role')
    expect(payload.creator).not.toHaveProperty('confirmPassword')
  })

  it('omits blank optional creator phone', () => {
    const payload = toOnboardingPayload({ ...valid, phone: '   ' })
    expect(payload.creator.phone).toBeUndefined()
  })
})
