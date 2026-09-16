import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { configureStore } from '@reduxjs/toolkit'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { SnackbarProvider } from 'notistack'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import authReducer from '@/app/store/authSlice'
import uiReducer from '@/app/store/uiSlice'
import { ROLE_SUPER_ADMIN } from '@/shared/types/auth'
import { lightTheme } from '@/theme/theme'
import { CooperativeDetailPage } from './CooperativeDetailPage'
import {
  assignCooperativePresident,
  fetchCooperative,
  fetchCooperativeSubscription,
} from '@/shared/api/cooperatives'

vi.mock('@/shared/api/cooperatives', () => ({
  fetchCooperative: vi.fn(),
  fetchCooperativeSubscription: vi.fn(),
  assignCooperativePresident: vi.fn(),
  updateCooperative: vi.fn(),
  updateCooperativeStatus: vi.fn(),
  uploadCooperativeLogo: vi.fn(),
}))

const fetchCooperativeMock = vi.mocked(fetchCooperative)
const fetchSubscriptionMock = vi.mocked(fetchCooperativeSubscription)
const assignPresidentMock = vi.mocked(assignCooperativePresident)

const incomplete = {
  id: 'coop-1',
  name: 'Need President',
  currency: 'RWF',
  status: 'ACTIVE' as const,
  onboardingState: 'AWAITING_PRESIDENT' as const,
  financialYearStartMonth: 1,
  monthlyContributionAmount: '1000',
  contributionDueDay: 1,
  registrationNumber: 'RCA/2024/0123',
}

function renderPage() {
  const store = configureStore({
    reducer: { auth: authReducer, ui: uiReducer },
    preloadedState: {
      auth: {
        user: {
          id: 'admin-1',
          username: 'superadmin',
          email: 'admin@test.local',
          firstName: 'Super',
          lastName: 'Admin',
          fullName: 'Super Admin',
          roles: [ROLE_SUPER_ADMIN],
          permissions: ['COOPERATIVE_WRITE'],
          cooperativeIds: [],
        },
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
          <SnackbarProvider>
            <MemoryRouter initialEntries={['/cooperatives/coop-1']}>
              <Routes>
                <Route path="/cooperatives/:id" element={<CooperativeDetailPage />} />
              </Routes>
            </MemoryRouter>
          </SnackbarProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </Provider>,
  )
}

describe('CooperativeDetailPage onboarding', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchCooperativeMock.mockResolvedValue(incomplete)
    fetchSubscriptionMock.mockResolvedValue({
      id: 'sub-1',
      cooperativeId: 'coop-1',
      status: 'TRIAL',
      trialStartedAt: '2026-01-15T00:00:00Z',
      trialEndsAt: '2026-05-15T00:00:00Z',
    })
  })

  it('shows the incomplete warning, Assign President action, and trial status', async () => {
    renderPage()
    expect(await screen.findByText('Need President')).toBeInTheDocument()
    expect(screen.getAllByText('President required').length).toBeGreaterThan(0)
    expect(
      screen.getByText(
        'This Saving Scheme will remain in incomplete onboarding until a President is assigned.',
      ),
    ).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Assign President' }).length).toBeGreaterThan(0)
    expect(screen.getByText(/Free trial started/i)).toBeInTheDocument()
  })

  it('assigns a President through the administrators wrapper and clears the warning', async () => {
    const user = userEvent.setup()
    assignPresidentMock.mockResolvedValue({
      userId: 'user-1',
      firstName: 'Pat',
      lastName: 'President',
      username: 'pat.president',
      email: 'pat@test.local',
      membershipStatus: 'ACTIVE',
      accountStatus: 'ACTIVE',
      roleInCooperative: 'PRESIDENT',
    })
    fetchCooperativeMock.mockResolvedValue({ ...incomplete, onboardingState: 'COMPLETE' })
    fetchCooperativeMock.mockResolvedValueOnce(incomplete)

    renderPage()
    await user.click((await screen.findAllByRole('button', { name: 'Assign President' }))[0])
    const firstName = await screen.findByLabelText(/^First name/)
    fireEvent.change(firstName, { target: { value: 'Pat' } })
    fireEvent.change(screen.getByLabelText(/^Last name/), { target: { value: 'President' } })
    fireEvent.change(screen.getByLabelText(/^Username/), { target: { value: 'pat.president' } })
    fireEvent.change(screen.getByLabelText(/^Email/), { target: { value: 'pat@test.local' } })
    await user.click(screen.getAllByRole('button', { name: 'Assign President' }).at(-1)!)

    await waitFor(() => {
      expect(assignPresidentMock).toHaveBeenCalledWith('coop-1', {
        username: 'pat.president',
        email: 'pat@test.local',
        firstName: 'Pat',
        lastName: 'President',
        phone: undefined,
        temporaryPassword: undefined,
      })
    })
    await waitFor(() => {
      expect(
        screen.queryByText(
          'This Saving Scheme will remain in incomplete onboarding until a President is assigned.',
        ),
      ).not.toBeInTheDocument()
    })
  }, 15000)
})
