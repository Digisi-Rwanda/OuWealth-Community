import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { configureStore } from '@reduxjs/toolkit'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import authReducer from '@/app/store/authSlice'
import uiReducer from '@/app/store/uiSlice'
import { ForgotPasswordPage } from '@/pages/ForgotPasswordPage'
import { LoginPage } from '@/pages/LoginPage'
import { SignupPage } from '@/pages/SignupPage'
import { ROLE_PRESIDENT } from '@/shared/types/auth'
import { lightTheme } from '@/theme/theme'
import { AppLayout } from './AppLayout'
import { AuthLayout } from './AuthLayout'
import { PublicLayout } from './PublicLayout'

vi.mock('@/shared/api/notifications', () => ({
  fetchUnreadCount: vi.fn().mockResolvedValue(0),
  fetchNotifications: vi.fn().mockResolvedValue([]),
  fetchPendingApprovals: vi.fn().mockResolvedValue([]),
  markNotificationRead: vi.fn(),
}))
vi.mock('@/shared/api/cooperatives', () => ({ fetchMyCooperatives: vi.fn().mockResolvedValue([]) }))
vi.mock('@/shared/api/subscription', () => ({
  cooperativeSubscriptionQueryKey: (id: string) => ['cooperatives', id, 'subscription'],
  fetchSubscription: vi.fn().mockResolvedValue({
    id: 'sub-1',
    cooperativeId: 'coop-1',
    status: 'TRIAL',
    effectiveStatus: 'TRIAL',
    writeAllowed: true,
    usable: true,
  }),
}))
vi.mock('@/shared/api/onboarding', () => ({ onboardCooperative: vi.fn() }))

function stubMedia(reducedMotion = false) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: (query: string) => ({
      matches: reducedMotion && query.includes('prefers-reduced-motion'),
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

function Where() {
  return <div data-testid="where">{useLocation().pathname}</div>
}

function renderAt(path: string, { signedIn = false }: { signedIn?: boolean } = {}) {
  const store = configureStore({
    reducer: { auth: authReducer, ui: uiReducer },
    preloadedState: {
      auth: signedIn
        ? {
            user: {
              id: 'u1',
              username: 'pat',
              email: 'pat@example.com',
              firstName: 'Pat',
              lastName: 'Leader',
              fullName: 'Pat Leader',
              roles: [ROLE_PRESIDENT],
              permissions: [],
              cooperativeIds: ['coop-1'],
            },
            accessToken: 't',
            selectedCooperativeId: 'coop-1',
            status: 'authenticated' as const,
          }
        : { user: null, accessToken: null, selectedCooperativeId: null, status: 'anonymous' as const },
      ui: { sidebarOpen: false, themePreference: 'light' as const },
    },
  })
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <Provider store={store}>
      <QueryClientProvider client={client}>
        <ThemeProvider theme={lightTheme}>
          <MemoryRouter initialEntries={[path]}>
            <Where />
            <Routes>
              <Route element={<PublicLayout />}>
                <Route path="/" element={<div data-testid="landing-stub" />} />
                <Route path="/about" element={<div data-testid="about-stub" />} />
              </Route>
              <Route element={<AuthLayout />}>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/signup" element={<SignupPage />} />
                <Route path="/forgot-password" element={<ForgotPasswordPage />} />
              </Route>
              <Route element={<AppLayout />}>
                <Route path="/dashboard" element={<div data-testid="dashboard-stub" />} />
              </Route>
            </Routes>
          </MemoryRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </Provider>,
  )
}

const homeLinks = () => screen.getAllByRole('link', { name: 'Go to OuWealth home' })

beforeEach(() => {
  vi.clearAllMocks()
  stubMedia()
})
afterEach(() => stubMedia())

describe('auth layout', () => {
  it('login: the OuWealth logo links to the landing page', () => {
    renderAt('/login')
    const brandPanel = screen.getByTestId('auth-brand-panel')
    const link = within(brandPanel).getByRole('link', { name: 'Go to OuWealth home' })
    expect(link).toHaveAttribute('href', '/')
    expect(within(link).getByRole('img', { name: 'OuWealth Community' })).toBeInTheDocument()
  })

  it('signup: the OuWealth logo links to the landing page', () => {
    renderAt('/signup')
    const link = within(screen.getByTestId('auth-brand-panel')).getByRole('link', { name: 'Go to OuWealth home' })
    expect(link).toHaveAttribute('href', '/')
  })

  it('forgot password: the (raster) auth logo links to the landing page too', () => {
    renderAt('/forgot-password')
    expect(homeLinks().every((link) => link.getAttribute('href') === '/')).toBe(true)
    expect(homeLinks().length).toBeGreaterThanOrEqual(1)
  })

  it('the logo link is a real, keyboard-focusable link and navigates home', async () => {
    const user = userEvent.setup()
    renderAt('/login')
    const link = within(screen.getByTestId('auth-brand-panel')).getByRole('link', { name: 'Go to OuWealth home' })
    await user.tab()
    // the brand panel logo is the first focusable element after the language/theme switchers
    link.focus()
    expect(link).toHaveFocus()
    await user.keyboard('{Enter}')
    expect(screen.getByTestId('where')).toHaveTextContent('/')
    expect(await screen.findByTestId('landing-stub')).toBeInTheDocument()
  })

  it('login renders a split shell with a brand panel and the form panel', () => {
    renderAt('/login')
    const shell = screen.getByTestId('auth-split-shell')
    const brand = within(shell).getByTestId('auth-brand-panel')
    const form = within(shell).getByTestId('auth-form-panel')
    expect(within(brand).getByText('Accumulate your wealth in an instant.')).toBeInTheDocument()
    expect(within(form).getByRole('heading', { name: /Sign in/i })).toBeInTheDocument()
    expect(within(form).getByLabelText(/username/i)).toBeInTheDocument()
    expect(screen.getByTestId('auth-canvas-split')).toBeInTheDocument()
    // no invented statistics in the brand panel
    expect(within(brand).queryByText(/\d{2,}/)).not.toBeInTheDocument()
  })

  it('signup renders the wider split shell around the wizard', () => {
    renderAt('/signup')
    const shell = screen.getByTestId('auth-split-shell')
    expect(within(shell).getByTestId('auth-brand-panel')).toBeInTheDocument()
    const form = within(shell).getByTestId('auth-form-panel')
    expect(within(form).getByRole('heading', { name: /Create your Saving Scheme/i })).toBeInTheDocument()
    expect(within(form).getByText('Saving Scheme Details')).toBeInTheDocument() // the stepper
    expect(within(form).getByRole('button', { name: 'Next' })).toBeInTheDocument()
  })

  it('signup uses the brand tagline, not the trial offer, and keeps the registration number removed', () => {
    renderAt('/signup')
    const brand = screen.getByTestId('auth-brand-panel')
    expect(within(brand).getByText('Accumulate your wealth in an instant.')).toBeInTheDocument()
    expect(screen.queryByText(/Start with 4 months free/i)).not.toBeInTheDocument()
    expect(screen.queryByTestId('trial-pricing-note')).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/registration number/i)).not.toBeInTheDocument()
    // wizard buttons stay medium sized (no "large" variant)
    expect(screen.getByRole('button', { name: 'Next' }).className).not.toMatch(/sizeLarge/)
  })

  it('signup validation still blocks Next on empty required fields', async () => {
    const user = userEvent.setup()
    renderAt('/signup')
    await user.click(screen.getByRole('button', { name: 'Next' }))
    expect(await screen.findAllByText(/required/i)).not.toHaveLength(0)
    expect(screen.getByText('Saving Scheme Details')).toBeInTheDocument()
    expect(screen.queryByLabelText(/^Username/i)).not.toBeInTheDocument() // still on step 1
  })
})

describe('support dock', () => {
  const dock = () => screen.getByTestId('support-dock')

  it.each([
    ['public layout', '/about', false],
    ['auth layout', '/login', false],
    ['app layout', '/dashboard', true],
  ])('appears once in the %s', (_label, path, signedIn) => {
    renderAt(path, { signedIn })
    expect(screen.getAllByTestId('support-dock')).toHaveLength(1)
  })

  it('has the correct, accessible email and WhatsApp links', () => {
    renderAt('/about')
    const email = within(dock()).getByRole('link', { name: 'Email support' })
    const whatsapp = within(dock()).getByRole('link', { name: 'Chat on WhatsApp' })
    expect(email).toHaveAttribute('href', 'mailto:support@ozufy.com')
    expect(whatsapp).toHaveAttribute('href', 'https://wa.me/250793634217')
    expect(whatsapp).toHaveAttribute('target', '_blank')
    expect(whatsapp.getAttribute('rel')).toMatch(/noopener/)
    expect(whatsapp.getAttribute('rel')).toMatch(/noreferrer/)
    expect(dock()).toHaveAttribute('aria-label', 'Get help')
  })

  it('does not print raw phone numbers in the dock', () => {
    renderAt('/login')
    expect(dock()).not.toHaveTextContent(/0782102154|0793634217/)
  })
})

describe('footers', () => {
  it('the landing page keeps the full footer with the new tagline', () => {
    renderAt('/')
    const footer = screen.getByTestId('public-footer')
    expect(screen.queryByTestId('compact-footer')).not.toBeInTheDocument()
    expect(within(footer).getByText('Accumulate your wealth in an instant.')).toBeInTheDocument()
    expect(footer).not.toHaveTextContent(/Software for Saving Schemes/)
    expect(footer).not.toHaveTextContent(/—|–/)
    expect(within(footer).getByRole('navigation', { name: 'Footer links' })).toBeInTheDocument()
  })

  it('other public pages use the compact footer', () => {
    renderAt('/about')
    const footer = screen.getByTestId('compact-footer')
    expect(screen.queryByTestId('public-footer')).not.toBeInTheDocument()
    expect(within(footer).queryByRole('navigation')).not.toBeInTheDocument()
    expect(footer).toHaveTextContent(/© \d{4} OuWealth Community/)
  })

  it('auth pages use the compact footer', () => {
    renderAt('/login')
    expect(screen.getByTestId('compact-footer')).toBeInTheDocument()
    expect(screen.queryByTestId('public-footer')).not.toBeInTheDocument()
  })

  it('the authenticated app uses the compact footer, without marketing links', () => {
    renderAt('/dashboard', { signedIn: true })
    const footer = screen.getByTestId('compact-footer')
    expect(screen.queryByTestId('public-footer')).not.toBeInTheDocument()
    expect(within(footer).queryByRole('link', { name: /pricing|privacy|terms/i })).not.toBeInTheDocument()
  })

  it('the compact footer logo links to the landing page, also for a signed-in user, without signing out', async () => {
    const user = userEvent.setup()
    renderAt('/dashboard', { signedIn: true })
    const link = within(screen.getByTestId('compact-footer')).getByRole('link', { name: 'Go to OuWealth home' })
    expect(link).toHaveAttribute('href', '/')
    await user.click(link)
    expect(screen.getByTestId('where')).toHaveTextContent('/')
    expect(await screen.findByTestId('landing-stub')).toBeInTheDocument()
  })

  it('landing footer logo smoothly scrolls to the top instead of navigating', async () => {
    const user = userEvent.setup()
    renderAt('/')
    vi.mocked(window.scrollTo).mockClear()
    await user.click(screen.getByRole('link', { name: 'OuWealth Community, back to top' }))
    expect(window.scrollTo).toHaveBeenCalledWith({ top: 0, left: 0, behavior: 'smooth' })
    expect(screen.getByTestId('where')).toHaveTextContent('/')
  })

  it('landing footer logo jumps to the top when the visitor prefers reduced motion', async () => {
    stubMedia(true)
    const user = userEvent.setup()
    renderAt('/')
    vi.mocked(window.scrollTo).mockClear()
    await user.click(screen.getByRole('link', { name: 'OuWealth Community, back to top' }))
    expect(window.scrollTo).toHaveBeenCalledWith({ top: 0, left: 0, behavior: 'auto' })
  })

  it('the footer logo is keyboard accessible', async () => {
    const user = userEvent.setup()
    renderAt('/')
    vi.mocked(window.scrollTo).mockClear()
    screen.getByRole('link', { name: 'OuWealth Community, back to top' }).focus()
    await user.keyboard('{Enter}')
    expect(window.scrollTo).toHaveBeenCalled()
  })

  it('from another public page the logo navigates to the landing page, which starts at the top', async () => {
    const user = userEvent.setup()
    renderAt('/about')
    vi.mocked(window.scrollTo).mockClear()
    await user.click(
      within(screen.getByTestId('compact-footer')).getByRole('link', { name: 'Go to OuWealth home' }),
    )
    expect(await screen.findByTestId('landing-stub')).toBeInTheDocument()
    expect(screen.getByTestId('public-footer')).toBeInTheDocument()
    expect(window.scrollTo).toHaveBeenCalledWith(0, 0)
  })
})
