import { describe, expect, it } from 'vitest'
import {
  canAccessNavItem,
  getMobileNavItems,
  isAdminUser,
  isCooperativeAdminUser,
  isSuperAdminUser,
  type NavItem,
} from '@/layouts/navItems'
import { ROLE_MEMBER, ROLE_PRESIDENT, ROLE_SUPER_ADMIN } from '@/shared/types/auth'
import DashboardIcon from '@mui/icons-material/Dashboard'

const memberItem: NavItem = {
  labelKey: 'nav.dashboard',
  path: '/dashboard',
  icon: DashboardIcon,
}

const adminItem: NavItem = {
  labelKey: 'nav.members',
  path: '/members',
  icon: DashboardIcon,
  roles: [ROLE_PRESIDENT, ROLE_SUPER_ADMIN],
}

const superItem: NavItem = {
  labelKey: 'nav.cooperatives',
  path: '/cooperatives',
  icon: DashboardIcon,
  roles: [ROLE_SUPER_ADMIN],
}

describe('canAccessNavItem', () => {
  it('allows unrestricted items for any authenticated role set', () => {
    expect(canAccessNavItem(memberItem, [])).toBe(true)
    expect(canAccessNavItem(memberItem, [ROLE_MEMBER])).toBe(true)
  })

  it('hides cooperative-admin menus from members', () => {
    expect(canAccessNavItem(adminItem, [ROLE_MEMBER])).toBe(false)
    expect(canAccessNavItem(adminItem, [ROLE_PRESIDENT])).toBe(true)
    expect(canAccessNavItem(adminItem, [ROLE_SUPER_ADMIN])).toBe(true)
  })

  it('restricts super-admin-only items to SUPER_ADMIN', () => {
    expect(canAccessNavItem(superItem, [ROLE_PRESIDENT])).toBe(false)
    expect(canAccessNavItem(superItem, [ROLE_MEMBER])).toBe(false)
    expect(canAccessNavItem(superItem, [ROLE_SUPER_ADMIN])).toBe(true)
  })
})

describe('getMobileNavItems', () => {
  it('returns simplified member navigation', () => {
    const items = getMobileNavItems([ROLE_MEMBER])
    expect(items.some((i) => i.path === '/dashboard')).toBe(true)
    expect(items.some((i) => i.path === '/reports')).toBe(true)
    expect(items.some((i) => i.path === '/ledger')).toBe(false)
    expect(items.some((i) => i.path === '/billing')).toBe(false)
    expect(items.some((i) => i.path === '/audit-logs')).toBe(false)
    expect(items.some((i) => i.path === '/cooperatives')).toBe(false)
    expect(isAdminUser([ROLE_MEMBER])).toBe(false)
  })

  it('includes admin modules for cooperative admins', () => {
    const items = getMobileNavItems([ROLE_PRESIDENT])
    expect(items.some((i) => i.path === '/members')).toBe(true)
    expect(items.some((i) => i.path === '/historical-import')).toBe(true)
    expect(items.some((i) => i.path === '/billing')).toBe(true)
    expect(items.some((i) => i.path === '/fine-payments')).toBe(true)
    expect(items.some((i) => i.path === '/cooperatives')).toBe(false)
    expect(isCooperativeAdminUser([ROLE_PRESIDENT])).toBe(true)
    expect(isSuperAdminUser([ROLE_PRESIDENT])).toBe(false)
  })

  it('gives super admins full operational access plus cooperatives', () => {
    const items = getMobileNavItems([ROLE_SUPER_ADMIN])
    expect(items.some((i) => i.path === '/cooperatives')).toBe(true)
    expect(items.some((i) => i.path === '/members')).toBe(true)
    expect(items.some((i) => i.path === '/ledger')).toBe(true)
    expect(items.some((i) => i.path === '/loans')).toBe(true)
    expect(isSuperAdminUser([ROLE_SUPER_ADMIN])).toBe(true)
    expect(isAdminUser([ROLE_SUPER_ADMIN])).toBe(true)
  })

  it('scopes secretary to records and accountant to the ledger', () => {
    const secretary = getMobileNavItems(['SECRETARY'])
    expect(secretary.some((i) => i.path === '/members')).toBe(true)
    expect(secretary.some((i) => i.path === '/audit-logs')).toBe(true)
    expect(secretary.some((i) => i.path === '/ledger')).toBe(false)
    expect(secretary.some((i) => i.path === '/contributions')).toBe(false)
    expect(secretary.some((i) => i.path === '/billing')).toBe(false)

    const accountant = getMobileNavItems(['ACCOUNTANT'])
    expect(accountant.some((i) => i.path === '/contributions')).toBe(true)
    expect(accountant.some((i) => i.path === '/ledger')).toBe(true)
    expect(accountant.some((i) => i.path === '/billing')).toBe(true)
    expect(accountant.some((i) => i.path === '/historical-import')).toBe(false)
    expect(accountant.some((i) => i.path === '/members')).toBe(false)
    expect(accountant.some((i) => i.path === '/audit-logs')).toBe(false)
  })
})
