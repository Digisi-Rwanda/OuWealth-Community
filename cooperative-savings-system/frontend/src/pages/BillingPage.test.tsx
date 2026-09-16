import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { configureStore } from '@reduxjs/toolkit'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import authReducer from '@/app/store/authSlice'
import uiReducer from '@/app/store/uiSlice'
import {
  fetchBillingPlans,
  fetchSubscriptionPayments,
  startBillingCheckout,
} from '@/shared/api/billing'
import { fetchMyCooperatives } from '@/shared/api/cooperatives'
import { fetchSubscription } from '@/shared/api/subscription'
import {
  ROLE_ACCOUNTANT,
  ROLE_MEMBER,
  ROLE_PRESIDENT,
  ROLE_SUPER_ADMIN,
  type AuthUser,
} from '@/shared/types/auth'
import type { BillingPlansResponse, SubscriptionPaymentRecord } from '@/shared/types/billing'
import type { CooperativeSubscription } from '@/shared/types/cooperative'
import { lightTheme } from '@/theme/theme'
import { BillingPage } from './BillingPage'

vi.mock('@/shared/api/subscription', () => ({
  cooperativeSubscriptionQueryKey: (id: string) => ['cooperatives', id, 'subscription'],
  fetchSubscription: vi.fn(),
}))

vi.mock('@/shared/api/billing', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/shared/api/billing')>()
  return {
    ...actual,
    fetchBillingPlans: vi.fn(),
    fetchSubscriptionPayments: vi.fn(),
    startBillingCheckout: vi.fn(),
  }
})

vi.mock('@/shared/api/cooperatives', () => ({
  fetchMyCooperatives: vi.fn(),
}))

const fetchSubscriptionMock = vi.mocked(fetchSubscription)
const fetchPlansMock = vi.mocked(fetchBillingPlans)
const fetchPaymentsMock = vi.mocked(fetchSubscriptionPayments)
const checkoutMock = vi.mocked(startBillingCheckout)
const fetchCoopsMock = vi.mocked(fetchMyCooperatives)

const plans: BillingPlansResponse = {
  currency: 'RWF',
  trialMonths: 4,
  plans: [
    {
      billingCycle: 'MONTHLY',
      listPrice: '2000.0000',
      amount: '2000.0000',
      discountPercent: '0',
      savings: '0.0000',
      periodMonths: 1,
    },
    {
      billingCycle: 'ANNUAL',
      listPrice: '24000.0000',
      amount: '18000.0000',
      discountPercent: '25',
      savings: '6000.0000',
      periodMonths: 12,
    },
  ],
}

const trial: CooperativeSubscription = {
  id: 's1',
  cooperativeId: 'coop-1',
  status: 'TRIAL',
  storedStatus: 'TRIAL',
  effectiveStatus: 'TRIAL',
  writeAllowed: true,
  usable: true,
  trialStartedAt: '2026-05-16T00:00:00Z',
  trialEndsAt: '2026-12-01T00:00:00Z',
  daysRemaining: 75,
}

function emptyPayments() {
  return {
    content: [] as SubscriptionPaymentRecord[],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  }
}

function integrationUnavailable() {
  return new AxiosError(
    'Not Implemented',
    'ERR_BAD_RESPONSE',
    {} as InternalAxiosRequestConfig,
    undefined,
    {
      status: 501,
      statusText: 'Not Implemented',
      headers: {},
      config: {} as InternalAxiosRequestConfig,
      data: {
        success: false,
        code: 'PAYMENT_INTEGRATION_UNAVAILABLE',
        message: 'Payment processing is not available yet. Your Saving Scheme was not charged.',
      },
    } as AxiosResponse,
  )
}

function renderPage(
  roles: string[],
  subscription: CooperativeSubscription | null = trial,
  payments = emptyPayments(),
) {
  fetchSubscriptionMock.mockResolvedValue(subscription)
  fetchPlansMock.mockResolvedValue(plans)
  fetchPaymentsMock.mockResolvedValue(payments)
  fetchCoopsMock.mockResolvedValue([
    { id: 'coop-1', name: 'Umurenge Scheme', status: 'ACTIVE', currency: 'RWF', logoUrl: null },
  ])
  const user: AuthUser = {
    id: 'u1',
    username: 'pat',
    email: 'pat@test.local',
    firstName: 'Pat',
    lastName: 'Leader',
    fullName: 'Pat Leader',
    roles,
    permissions: [],
    cooperativeIds: ['coop-1'],
  }
  const store = configureStore({
    reducer: { auth: authReducer, ui: uiReducer },
    preloadedState: {
      auth: {
        user,
        accessToken: 'token',
        selectedCooperativeId: 'coop-1',
        status: 'authenticated' as const,
      },
      ui: { sidebarOpen: false, themePreference: 'light' as const },
    },
  })
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <Provider store={store}>
      <QueryClientProvider client={client}>
        <ThemeProvider theme={lightTheme}>
          <MemoryRouter initialEntries={['/billing']}>
            <BillingPage />
          </MemoryRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </Provider>,
  )
}

describe('BillingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    checkoutMock.mockRejectedValue(integrationUnavailable())
  })

  it('renders the billing route for the selected cooperative', async () => {
    renderPage([ROLE_PRESIDENT])
    expect(await screen.findByTestId('billing-page')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Billing & Subscription' })).toBeInTheDocument()
    expect(await screen.findByTestId('billing-scheme-name')).toHaveTextContent('Umurenge Scheme')
  })

  it('shows TRIAL details without charging', async () => {
    renderPage([ROLE_PRESIDENT])
    const card = await screen.findByTestId('current-subscription-card')
    expect(within(card).getAllByText('Free Trial').length).toBeGreaterThan(0)
    expect(within(card).getByText(/75 days remaining/i)).toBeInTheDocument()
    expect(within(card).getByText(/currently using OuWealth free of charge/i)).toBeInTheDocument()
    expect(screen.getByText(/Choosing a plan does not start a paid subscription/i)).toBeInTheDocument()
  })

  it('shows ACTIVE monthly details', async () => {
    renderPage([ROLE_PRESIDENT], {
      ...trial,
      status: 'ACTIVE',
      storedStatus: 'ACTIVE',
      effectiveStatus: 'ACTIVE',
      billingCycle: 'MONTHLY',
      currentPeriodStartedAt: '2026-09-01T00:00:00Z',
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
    })
    const card = await screen.findByTestId('current-subscription-card')
    expect(within(card).getAllByText('Monthly Plan').length).toBeGreaterThan(0)
    expect(within(card).getByText(/2,000/)).toBeInTheDocument()
    expect(within(card).getByText(/Current period ends/i)).toBeInTheDocument()
  })

  it('shows ACTIVE annual details', async () => {
    renderPage([ROLE_PRESIDENT], {
      ...trial,
      status: 'ACTIVE',
      storedStatus: 'ACTIVE',
      effectiveStatus: 'ACTIVE',
      billingCycle: 'ANNUAL',
      currentPeriodStartedAt: '2026-01-01T00:00:00Z',
      currentPeriodEndsAt: '2027-01-01T00:00:00Z',
    })
    const card = await screen.findByTestId('current-subscription-card')
    expect(within(card).getAllByText('Annual Plan').length).toBeGreaterThan(0)
    expect(within(card).getByText(/18,000/)).toBeInTheDocument()
    expect(within(card).getByText(/Save 25%/i)).toBeInTheDocument()
  })

  it('shows PAST_DUE access deadline', async () => {
    renderPage([ROLE_PRESIDENT], {
      ...trial,
      status: 'PAST_DUE',
      storedStatus: 'PAST_DUE',
      effectiveStatus: 'PAST_DUE',
      writeAllowed: true,
      pastDueUntil: '2026-10-15T00:00:00Z',
    })
    const card = await screen.findByTestId('current-subscription-card')
    expect(within(card).getAllByText('Payment overdue').length).toBeGreaterThan(0)
    expect(within(card).getAllByText(/Access remains available until/i).length).toBeGreaterThan(0)
  })

  it('shows EXPIRED read-only copy and still loads billing', async () => {
    renderPage([ROLE_PRESIDENT], {
      ...trial,
      status: 'EXPIRED',
      storedStatus: 'EXPIRED',
      effectiveStatus: 'EXPIRED',
      writeAllowed: false,
      usable: false,
    })
    const card = await screen.findByTestId('current-subscription-card')
    expect(within(card).getAllByText('Subscription expired').length).toBeGreaterThan(0)
    expect(within(card).getByText(/currently read-only/i)).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Choose Monthly' })).toBeInTheDocument()
  })

  it('shows NONE subscription required', async () => {
    renderPage([ROLE_PRESIDENT], {
      ...trial,
      status: 'NONE',
      storedStatus: 'NONE',
      effectiveStatus: 'NONE',
      writeAllowed: false,
      usable: false,
      trialStartedAt: null,
      trialEndsAt: null,
      daysRemaining: null,
    })
    const card = await screen.findByTestId('current-subscription-card')
    expect(within(card).getAllByText('Subscription required').length).toBeGreaterThan(0)
  })

  it('renders monthly and annual catalog amounts from the API', async () => {
    renderPage([ROLE_PRESIDENT])
    const monthly = await screen.findByTestId('plan-card-MONTHLY')
    const annual = await screen.findByTestId('plan-card-ANNUAL')
    expect(monthly).toHaveTextContent('2,000')
    expect(annual).toHaveTextContent('18,000')
    expect(screen.getByTestId('annual-list-price')).toHaveTextContent('24,000')
    expect(annual).toHaveTextContent('25%')
    expect(screen.getByTestId('annual-savings')).toHaveTextContent('6,000')
    expect(annual).toHaveTextContent('1,500')
  })

  it('lets leadership choose monthly, MTN Mobile Money, and review without fake success', async () => {
    const user = userEvent.setup()
    renderPage([ROLE_PRESIDENT])
    await user.click(await screen.findByRole('button', { name: 'Choose Monthly' }))
    expect(screen.getByTestId('plan-card-MONTHLY')).toHaveAttribute('aria-pressed', 'true')
    await user.click(screen.getByLabelText('MTN Mobile Money'))
    const review = screen.getByTestId('billing-review-card')
    expect(review).toHaveTextContent('Monthly')
    expect(review).toHaveTextContent('2,000')
    expect(review).toHaveTextContent('1 month')
    expect(review).toHaveTextContent('MTN Mobile Money')
    expect(screen.queryByLabelText(/PIN/i)).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Continue to Payment' }))
    expect(await screen.findByTestId('checkout-unavailable')).toHaveTextContent(
      'Payment processing is not available yet',
    )
    expect(checkoutMock).toHaveBeenCalledWith('coop-1', {
      billingCycle: 'MONTHLY',
      paymentChannel: 'MTN_MOMO',
    })
    expect(screen.queryByText(/payment successful/i)).not.toBeInTheDocument()
  })

  it('lets leadership choose annual and bank card, then review discount', async () => {
    const user = userEvent.setup()
    renderPage([ROLE_PRESIDENT])
    await user.click(await screen.findByRole('button', { name: 'Choose Annual' }))
    expect(screen.getByTestId('plan-card-ANNUAL')).toHaveAttribute('aria-pressed', 'true')
    await user.click(screen.getByLabelText('Bank Card'))
    const review = screen.getByTestId('billing-review-card')
    expect(review).toHaveTextContent('Annual')
    expect(review).toHaveTextContent('24,000')
    expect(review).toHaveTextContent('25%')
    expect(review).toHaveTextContent('6,000')
    expect(review).toHaveTextContent('18,000')
    expect(review).toHaveTextContent('Bank Card')
    expect(screen.queryByLabelText(/card number/i)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/cvv/i)).not.toBeInTheDocument()
  })

  it('hides payment controls for members', async () => {
    renderPage([ROLE_MEMBER])
    expect(
      await screen.findByText('Subscription payments are managed by your Saving Scheme leadership.'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Choose Monthly' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Choose Annual' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('MTN Mobile Money')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Continue to Payment' })).not.toBeInTheDocument()
    expect(screen.getByTestId('plan-card-MONTHLY')).toHaveTextContent('2,000')
  })

  it('shows management controls for accountant', async () => {
    renderPage([ROLE_ACCOUNTANT])
    expect(await screen.findByRole('button', { name: 'Choose Monthly' })).toBeInTheDocument()
  })

  it('shows management controls for super admin', async () => {
    renderPage([ROLE_SUPER_ADMIN])
    expect(await screen.findByRole('button', { name: 'Choose Monthly' })).toBeInTheDocument()
  })

  it('shows empty payment history during trial', async () => {
    renderPage([ROLE_PRESIDENT])
    expect(await screen.findByText('No subscription payments yet.')).toBeInTheDocument()
  })

  it('renders subscription payment history records', async () => {
    renderPage(
      [ROLE_PRESIDENT],
      {
        ...trial,
        status: 'ACTIVE',
        effectiveStatus: 'ACTIVE',
        billingCycle: 'ANNUAL',
      },
      {
        content: [
          {
            id: 'pay-1',
            billingCycle: 'ANNUAL',
            paymentChannel: 'MTN_MOMO',
            status: 'SUCCESS',
            currency: 'RWF',
            amount: '18000.0000',
            provider: 'placeholder',
            externalReference: 'SAFE-REF-1',
            initiatedAt: '2026-09-01T10:00:00Z',
            paidAt: '2026-09-01T10:01:00Z',
            failedAt: null,
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      },
    )
    expect(await screen.findByText('Successful')).toBeInTheDocument()
    const history = screen.getByTestId('payment-history')
    expect(within(history).getByText('Annual')).toBeInTheDocument()
    expect(within(history).getByText('MTN Mobile Money')).toBeInTheDocument()
    expect(within(history).getByText(/18,000/)).toBeInTheDocument()
    expect(within(history).getByText('Successful')).toBeInTheDocument()
  })
})
