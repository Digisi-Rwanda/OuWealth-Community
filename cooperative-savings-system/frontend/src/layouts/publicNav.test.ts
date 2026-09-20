import { describe, expect, it } from 'vitest'
import { isPublicNavActive, publicNavTarget, publicPrimaryLinks } from './publicNav'
import { ROUTES } from '@/shared/constants/routes'

describe('publicNav', () => {
  it('includes Pricing as a home hash link', () => {
    const pricing = publicPrimaryLinks.find((l) => l.hash === 'pricing')
    expect(pricing).toBeTruthy()
    expect(publicNavTarget(pricing!)).toEqual({ pathname: ROUTES.home, hash: 'pricing' })
  })

  it('activates Pricing only when hash matches', () => {
    const pricing = publicPrimaryLinks.find((l) => l.hash === 'pricing')!
    const home = publicPrimaryLinks.find((l) => l.labelKey === 'public.nav.home')!
    expect(isPublicNavActive(pricing, '/', '#pricing')).toBe(true)
    expect(isPublicNavActive(pricing, '/', '')).toBe(false)
    expect(isPublicNavActive(home, '/', '#pricing')).toBe(false)
    expect(isPublicNavActive(home, '/', '')).toBe(true)
  })
})
