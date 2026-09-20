import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { configureStore } from '@reduxjs/toolkit'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import authReducer from '@/app/store/authSlice'
import uiReducer from '@/app/store/uiSlice'
import { BRAND_LOGO_ALT } from '@/shared/components/BrandLogo'
import {
  ROLE_ACCOUNTANT,
  ROLE_LOAN_OFFICER,
  ROLE_MEMBER,
  ROLE_PRESIDENT,
  ROLE_SECRETARY,
  ROLE_SUPER_ADMIN,
  ROLE_VICE_PRESIDENT,
} from '@/shared/types/auth'
import { darkTheme, lightTheme } from '@/theme/theme'
import { AppLayout } from './AppLayout'

vi.mock('@/shared/api/notifications', () => ({
  fetchUnreadCount: vi.fn().mockResolvedValue(0),
  fetchNotifications: vi.fn().mockResolvedValue([]),
  fetchPendingApprovals: vi.fn().mockResolvedValue([]),
  markNotificationRead: vi.fn(),
}))

vi.mock('@/shared/api/cooperatives', () => ({
  fetchMyCooperatives: vi.fn().mockResolvedValue([
    { id: 'coop-1', name: 'Alpha Scheme', code: 'A1' },
    { id: 'coop-2', name: 'Beta Scheme', code: 'B2' },
  ]),
}))

vi.mock('@/shared/api/subscription', () => ({
  cooperativeSubscriptionQueryKey: (id: string) => ['cooperatives', id, 'subscription'],
  fetchSubscription: vi.fn().mockResolvedValue({
    id: 'sub-1',
    cooperativeId: 'coop-1',
    status: 'TRIAL',
    effectiveStatus: 'TRIAL',
    writeAllowed: true,
    usable: true,
    daysRemaining: 10,
    trialEndsAt: '2099-12-31T00:00:00Z',
  }),
}))

vi.mock('@/shared/api/auth', () => ({
  logout: vi.fn().mockResolvedValue(undefined),
}))

function stubMatchMedia(mdUp: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: (query: string) => ({
      matches: query.includes('900') ? mdUp : false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }),
  })
}

function renderLayout({
  roles,
  mdUp = true,
  path = '/dashboard',
  mode = 'light' as 'light' | 'dark',
  selectedCooperativeId = 'coop-1',
  isSuperAdmin = false,
}: {
  roles: string[]
  mdUp?: boolean
  path?: string
  mode?: 'light' | 'dark'
  selectedCooperativeId?: string | null
  isSuperAdmin?: boolean
}) {
  stubMatchMedia(mdUp)
  const store = configureStore({
    reducer: { auth: authReducer, ui: uiReducer },
    preloadedState: {
      auth: {
        user: {
          id: 'u1',
          username: 'tester',
          email: 't@example.com',
          firstName: 'Test',
          lastName: 'User',
          fullName: 'Test User',
          roles,
          permissions: [],
          cooperativeIds: ['coop-1', 'coop-2'],
        },
        accessToken: 'test-token',
        selectedCooperativeId,
        status: 'authenticated' as const,
      },
      ui: { sidebarOpen: false, themePreference: mode },
    },
  })
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })

  const view = render(
    <Provider store={store}>
      <QueryClientProvider client={client}>
        <ThemeProvider theme={mode === 'dark' ? darkTheme : lightTheme}>
          <MemoryRouter initialEntries={[path]}>
            <Routes>
              <Route element={<AppLayout />}>
                <Route path="/dashboard" element={<div data-testid="page-dashboard">Dashboard</div>} />
                <Route path="/loans" element={<div data-testid="page-loans">Loans</div>} />
                <Route path="/loans/:id" element={<div data-testid="page-loan-detail">Loan detail</div>} />
                <Route path="/members" element={<div data-testid="page-members">Members</div>} />
                <Route path="/members/:id" element={<div data-testid="page-member-detail">Member detail</div>} />
                <Route path="/cooperatives" element={<div data-testid="page-coops">Coops</div>} />
                <Route path="/cooperatives/:id" element={<div data-testid="page-coop-detail">Coop detail</div>} />
                <Route path="/billing" element={<div data-testid="page-billing">Billing</div>} />
                <Route path="/settings" element={<div data-testid="page-settings">Settings</div>} />
                <Route path="/contributions" element={<div data-testid="page-contrib">Contributions</div>} />
                <Route path="/ledger" element={<div data-testid="page-ledger">Ledger</div>} />
                <Route path="/profile" element={<div data-testid="page-profile">Profile</div>} />
                <Route path="*" element={<div data-testid="page-other">Other</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </Provider>,
  )

  return { ...view, store, isSuperAdmin }
}

function desktopNav() {
  return within(screen.getByTestId('app-sidebar-desktop')).getByRole('navigation', {
    name: /main navigation/i,
  })
}

describe('AppLayout sidebar (Phase B)', () => {
  beforeEach(() => {
    stubMatchMedia(true)
  })

  it('renders a permanent desktop sidebar and slim top bar', () => {
    renderLayout({ roles: [ROLE_PRESIDENT], mdUp: true })
    expect(screen.getByTestId('app-sidebar-desktop')).toBeInTheDocument()
    expect(screen.getByTestId('app-top-bar')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Menu$/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /My account/i })).not.toBeInTheDocument()
  })

  it('marks Dashboard as the active nav item on /dashboard', () => {
    renderLayout({ roles: [ROLE_PRESIDENT], path: '/dashboard' })
    const link = within(desktopNav()).getByRole('link', { name: /^Dashboard$/i })
    expect(link).toHaveAttribute('aria-current', 'page')
  })

  it('keeps Loans parent active on loan detail routes', () => {
    renderLayout({ roles: [ROLE_PRESIDENT], path: '/loans/loan-99' })
    const loans = within(desktopNav()).getByRole('link', { name: /^Loans$/i })
    expect(loans).toHaveAttribute('aria-current', 'page')
    expect(screen.getByTestId('page-loan-detail')).toBeInTheDocument()
  })

  it('shows MEMBER-only navigation modules', () => {
    renderLayout({ roles: [ROLE_MEMBER] })
    const nav = desktopNav()
    expect(within(nav).getByRole('link', { name: /^Dashboard$/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /My contributions/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /My loans/i })).toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Members$/i })).not.toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Billing$/i })).not.toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Saving Schemes$/i })).not.toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Ledger$/i })).not.toBeInTheDocument()
  })

  it('shows PRESIDENT leadership and operations modules', () => {
    renderLayout({ roles: [ROLE_PRESIDENT] })
    const nav = desktopNav()
    expect(within(nav).getByRole('link', { name: /^Members$/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /^Settings$/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /Historical Data Import/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /^Billing$/i })).toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Saving Schemes$/i })).not.toBeInTheDocument()
  })

  it('shows ACCOUNTANT finance modules without membership admin', () => {
    renderLayout({ roles: [ROLE_ACCOUNTANT] })
    const nav = desktopNav()
    expect(within(nav).getByRole('link', { name: /^Contributions$/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /^Ledger$/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /^Billing$/i })).toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Members$/i })).not.toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Settings$/i })).not.toBeInTheDocument()
  })

  it('shows LOAN_OFFICER loan-related modules', () => {
    renderLayout({ roles: [ROLE_LOAN_OFFICER] })
    const nav = desktopNav()
    expect(within(nav).getByRole('link', { name: /^Loans$/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /Loan Approvals/i })).toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Billing$/i })).not.toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Members$/i })).not.toBeInTheDocument()
  })

  it('keeps SECRETARY records access unchanged', () => {
    renderLayout({ roles: [ROLE_SECRETARY] })
    const nav = desktopNav()
    expect(within(nav).getByRole('link', { name: /^Members$/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /Audit logs/i })).toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Contributions$/i })).not.toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: /^Billing$/i })).not.toBeInTheDocument()
  })

  it('shows SUPER_ADMIN platform navigation', () => {
    renderLayout({ roles: [ROLE_SUPER_ADMIN] })
    const nav = desktopNav()
    expect(within(nav).getByRole('link', { name: /^Saving Schemes$/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /^System$/i })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /^Members$/i })).toBeInTheDocument()
  })

  it('restricts Billing to billing-manager roles', () => {
    const member = renderLayout({ roles: [ROLE_MEMBER] })
    expect(within(desktopNav()).queryByRole('link', { name: /^Billing$/i })).not.toBeInTheDocument()
    member.unmount()

    renderLayout({ roles: [ROLE_VICE_PRESIDENT] })
    expect(within(desktopNav()).getByRole('link', { name: /^Billing$/i })).toBeInTheDocument()
  })

  it('shows Settings only for leadership roles', () => {
    const officer = renderLayout({ roles: [ROLE_LOAN_OFFICER] })
    expect(within(desktopNav()).queryByRole('link', { name: /^Settings$/i })).not.toBeInTheDocument()
    officer.unmount()

    renderLayout({ roles: [ROLE_PRESIDENT] })
    expect(within(desktopNav()).getByRole('link', { name: /^Settings$/i })).toBeInTheDocument()
  })

  it('opens the mobile drawer from the hamburger with the same nav source', async () => {
    const user = userEvent.setup()
    renderLayout({ roles: [ROLE_PRESIDENT], mdUp: false })
    expect(screen.queryByTestId('app-sidebar-desktop')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /open menu/i }))
    const paper = document.querySelector('.MuiDrawer-paper') as HTMLElement
    expect(paper).toBeTruthy()
    expect(within(paper).getByRole('link', { name: /^Members$/i })).toBeInTheDocument()
    expect(within(paper).getByRole('link', { name: /^Dashboard$/i })).toBeInTheDocument()
  })

  it('closes the mobile drawer after selecting a route', async () => {
    const user = userEvent.setup()
    renderLayout({ roles: [ROLE_PRESIDENT], mdUp: false, path: '/dashboard' })
    await user.click(screen.getByRole('button', { name: /open menu/i }))
    const paper = document.querySelector('.MuiDrawer-paper') as HTMLElement
    await user.click(within(paper).getByRole('link', { name: /^Members$/i }))
    expect(screen.getByTestId('page-members')).toBeInTheDocument()
    await waitFor(
      () => {
        expect(document.querySelector('.MuiModal-root')).toHaveAttribute('aria-hidden', 'true')
      },
      { timeout: 15_000 },
    )
  }, 20_000)

  it('keeps CooperativeSelector, notifications, user menu, language and theme controls', async () => {
    renderLayout({ roles: [ROLE_PRESIDENT], mdUp: true })
    const top = screen.getByTestId('app-top-bar')
    expect(await within(top).findByLabelText(/Select Saving Scheme/i)).toBeInTheDocument()
    expect(within(top).getByRole('button', { name: /notifications/i })).toBeInTheDocument()
    expect(within(top).getByRole('button', { name: /profile/i })).toBeInTheDocument()
    expect(within(top).getByLabelText(/language/i)).toBeInTheDocument()
    expect(within(top).getByLabelText(/theme/i)).toBeInTheDocument()
  })

  it('renders OfflineBanner and SubscriptionBanner above main content', async () => {
    Object.defineProperty(window.navigator, 'onLine', { configurable: true, get: () => false })
    renderLayout({ roles: [ROLE_PRESIDENT] })
    expect(screen.getByText(/you're offline/i)).toBeInTheDocument()
    expect(await screen.findByTestId('subscription-banner')).toBeInTheDocument()
    Object.defineProperty(window.navigator, 'onLine', { configurable: true, get: () => true })
  })

  it('continues to render deep-linked detail pages inside AppLayout', () => {
    renderLayout({ roles: [ROLE_PRESIDENT], path: '/members/m-1' })
    expect(screen.getByTestId('page-member-detail')).toBeInTheDocument()
    expect(within(desktopNav()).getByRole('link', { name: /^Members$/i })).toHaveAttribute(
      'aria-current',
      'page',
    )
  })

  it('places the brand lockup in the desktop sidebar navigation', () => {
    renderLayout({ roles: [ROLE_PRESIDENT], mdUp: true })
    const sidebar = screen.getByTestId('app-sidebar-desktop')
    expect(within(sidebar).getByRole('img', { name: BRAND_LOGO_ALT })).toBeInTheDocument()
    expect(within(sidebar).getByText('Wealth')).toBeInTheDocument()
    expect(within(screen.getByRole('banner')).queryByText('Wealth')).not.toBeInTheDocument()
  })
})
