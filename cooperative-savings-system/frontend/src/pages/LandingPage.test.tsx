import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { LandingPage } from '@/pages/LandingPage'
import { AppProviders } from '@/app/providers/AppProviders'
import { ROUTES } from '@/shared/constants/routes'

function renderLanding(initial = '/') {
  return render(
    <MemoryRouter initialEntries={[initial]}>
      <AppProviders>
        <LandingPage />
      </AppProviders>
    </MemoryRouter>,
  )
}

describe('LandingPage Phase E', () => {
  it('shows public subscription pricing amounts and trial messaging', () => {
    renderLanding()
    const pricing = screen.getByTestId('landing-pricing')
    expect(within(pricing).getByTestId('pricing-trial-lead')).toHaveTextContent(/4 months free/i)
    expect(within(pricing).getByTestId('pricing-trial-months')).toHaveTextContent(/4 months/i)
    expect(within(pricing).getByTestId('pricing-monthly-amount')).toHaveTextContent(/RWF\s*2[\s,]?000/)
    expect(within(pricing).getByTestId('pricing-annual-amount')).toHaveTextContent(/RWF\s*18[\s,]?000/)
    expect(within(pricing).getByTestId('pricing-annual-list')).toHaveTextContent(/RWF\s*24[\s,]?000/)
    expect(within(pricing).getByTestId('pricing-annual-discount')).toHaveTextContent(/25%/)
    expect(within(pricing).getByTestId('pricing-annual-savings')).toHaveTextContent(/RWF\s*6[\s,]?000/)
    expect(within(pricing).getByTestId('pricing-annual-equivalent')).toHaveTextContent(/RWF\s*1[\s,]?500/)
    expect(within(pricing).getByText(/No subscription payment is required/i)).toBeInTheDocument()
  })

  it('routes pricing CTAs to signup and login', () => {
    renderLanding()
    expect(screen.getByTestId('pricing-cta-signup')).toHaveAttribute('href', ROUTES.signup)
    expect(screen.getByTestId('pricing-cta-login')).toHaveAttribute('href', ROUTES.login)
  })

  it('renders journey steps for contributions, loans, investments, and visibility', async () => {
    const user = userEvent.setup({ delay: null })
    renderLanding()
    const journey = screen.getByTestId('landing-journey')
    expect(within(journey).getByRole('heading', { name: /From contributions to shared progress/i })).toBeInTheDocument()

    await user.click(screen.getByTestId('journey-step-tab-contributions'))
    expect(screen.getByTestId('journey-panel-contributions')).toHaveTextContent(/expected to contribute/i)

    await user.click(screen.getByTestId('journey-step-tab-loans'))
    expect(screen.getByTestId('journey-panel-loans')).toHaveTextContent(/loan requests/i)

    await user.click(screen.getByTestId('journey-step-tab-investments'))
    expect(screen.getByTestId('journey-panel-investments')).toHaveTextContent(/investments/i)

    await user.click(screen.getByTestId('journey-step-tab-visibility'))
    expect(screen.getByTestId('journey-panel-visibility')).toHaveTextContent(/financial position/i)

    await user.click(screen.getByTestId('journey-step-tab-progress'))
    expect(screen.getByTestId('journey-panel-progress')).toHaveTextContent(/transparent records/i)

    await user.click(screen.getByTestId('journey-step-tab-community'))
    expect(screen.getByTestId('journey-panel-community')).toHaveTextContent(/organize your Saving Scheme/i)
  })

  it('exposes keyboard-accessible journey controls without auto-rotation', async () => {
    const user = userEvent.setup({ delay: null })
    renderLanding()
    expect(screen.getByTestId('journey-prev')).toHaveAccessibleName(/previous journey step/i)
    expect(screen.getByTestId('journey-next')).toHaveAccessibleName(/next journey step/i)
    await user.click(screen.getByTestId('journey-next'))
    expect(screen.getByTestId('journey-panel-contributions')).toBeInTheDocument()
    await user.click(screen.getByTestId('journey-prev'))
    expect(screen.getByTestId('journey-panel-community')).toBeInTheDocument()
  })

  it('does not invent fake customer statistics', () => {
    renderLanding()
    const landing = screen.getByTestId('landing-page')
    const text = landing.textContent ?? ''
    expect(text).not.toMatch(/happy customers/i)
    expect(text).not.toMatch(/98%\s*repayment/i)
    expect(text).not.toMatch(/12\s*M\s*invested/i)
    expect(text).not.toMatch(/500\s+members/i)
  })

  it('contains no member-named advanced analytics', () => {
    renderLanding()
    const landing = screen.getByTestId('landing-page')
    const text = landing.textContent ?? ''
    expect(screen.queryByTestId('advanced-insights')).not.toBeInTheDocument()
    expect(screen.queryByTestId('frequent-borrowers-card')).not.toBeInTheDocument()
    expect(screen.queryByTestId('member-insights')).not.toBeInTheDocument()
    expect(text).not.toMatch(/frequent borrowers/i)
    expect(text).not.toMatch(/largest active investments/i)
  })

  it('stacks pricing cards for mobile layout structure', () => {
    renderLanding()
    const cards = screen.getByTestId('landing-pricing-cards')
    expect(cards).toHaveStyle({ display: 'grid' })
    expect(screen.getByTestId('pricing-card-trial')).toBeInTheDocument()
    expect(screen.getByTestId('pricing-card-monthly')).toBeInTheDocument()
    expect(screen.getByTestId('pricing-card-annual')).toBeInTheDocument()
  })
})
