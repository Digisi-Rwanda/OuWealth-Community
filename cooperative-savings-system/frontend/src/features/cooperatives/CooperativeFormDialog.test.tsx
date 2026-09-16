import { ThemeProvider } from '@mui/material'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { lightTheme } from '@/theme/theme'
import { CooperativeFormDialog } from './CooperativeFormDialog'

function renderCreate(onSubmit = vi.fn()) {
  return {
    onSubmit,
    ...render(
      <ThemeProvider theme={lightTheme}>
        <CooperativeFormDialog open mode="create" onClose={vi.fn()} onSubmit={onSubmit} />
      </ThemeProvider>,
    ),
  }
}

function fillRequiredFields() {
  fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Ubumwe Savings' } })
  fireEvent.change(screen.getByLabelText(/^Registration number/), {
    target: { value: 'RCA/2024/0123' },
  })
  fireEvent.change(screen.getByLabelText(/^Contact email/), { target: { value: 'info@ubumwe.rw' } })
  fireEvent.change(screen.getByLabelText(/^Contact phone/), { target: { value: '0781234567' } })
  fireEvent.change(screen.getByLabelText(/^Registration date/), { target: { value: '2024-01-15' } })
}

describe('CooperativeFormDialog onboarding', () => {
  it('defaults Start 4-month free trial to checked and shows the assign-later warning', () => {
    renderCreate()
    expect(screen.getByRole('checkbox', { name: /Start 4-month free trial/i })).toBeChecked()
    expect(
      screen.getByText(
        'This Saving Scheme will remain in incomplete onboarding until a President is assigned.',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText(/Existing user ID/i)).not.toBeInTheDocument()
  })

  it('shows president fields when Assign President now is selected', async () => {
    const user = userEvent.setup()
    renderCreate()
    await user.click(screen.getByRole('radio', { name: /Assign President now/i }))
    expect(
      screen.queryByText(
        'This Saving Scheme will remain in incomplete onboarding until a President is assigned.',
      ),
    ).not.toBeInTheDocument()
    expect(screen.getByLabelText(/President source/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Username/i)).toBeInTheDocument()
  })

  it('submits START_TRIAL without president by default', async () => {
    const user = userEvent.setup({ delay: null })
    const { onSubmit } = renderCreate()
    fillRequiredFields()
    await user.click(screen.getByRole('button', { name: /save/i }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0][0]).toMatchObject({
      name: 'Ubumwe Savings',
      subscriptionInitialization: 'START_TRIAL',
    })
    expect(onSubmit.mock.calls[0][0].president).toBeUndefined()
  })

  it('maps an unchecked trial to NONE', async () => {
    const user = userEvent.setup({ delay: null })
    const { onSubmit } = renderCreate()
    fillRequiredFields()
    await user.click(screen.getByRole('checkbox', { name: /Start 4-month free trial/i }))
    expect(screen.getByRole('checkbox', { name: /Start 4-month free trial/i })).not.toBeChecked()
    await user.click(screen.getByRole('button', { name: /save/i }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0][0].subscriptionInitialization).toBe('NONE')
  })
})
