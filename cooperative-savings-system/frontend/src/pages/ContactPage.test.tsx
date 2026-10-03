import { ThemeProvider } from '@mui/material'
import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { openMailClient } from '@/features/contact/openMailClient'
import { SUPPORT_CONTACTS } from '@/shared/constants/supportContacts'
import { cssFor } from '@/test/cssHelpers'
import { lightTheme } from '@/theme/theme'
import { ContactPage } from './ContactPage'

vi.mock('@/features/contact/openMailClient', () => ({ openMailClient: vi.fn() }))

function renderPage() {
  return render(
    <ThemeProvider theme={lightTheme}>
      <MemoryRouter>
        <ContactPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

const field = (label: RegExp) => screen.getByLabelText(label)
const send = () => screen.getByRole('button', { name: 'Send message' })

function fill(values: Partial<Record<'first' | 'last' | 'email' | 'phone' | 'message', string>> = {}) {
  const v = {
    first: 'Alice',
    last: 'Uwase',
    email: 'alice@example.com',
    phone: '',
    message: 'I need help setting up my scheme.',
    ...values,
  }
  fireEvent.change(field(/^First name/i), { target: { value: v.first } })
  fireEvent.change(field(/^Last name/i), { target: { value: v.last } })
  fireEvent.change(field(/^Email/i), { target: { value: v.email } })
  if (v.phone) fireEvent.change(field(/Phone number/i), { target: { value: v.phone } })
  fireEvent.change(field(/^Message/i), { target: { value: v.message } })
}

function mailtoParts() {
  const url = vi.mocked(openMailClient).mock.calls[0][0]
  const [base, query] = url.split('?')
  const params = new URLSearchParams(query)
  return { url, base, subject: params.get('subject') ?? '', body: params.get('body') ?? '' }
}

describe('support channels', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows the exact call number with a tel link', () => {
    renderPage()
    const call = screen.getByTestId('support-call')
    expect(call).toHaveTextContent('Call us')
    expect(call).toHaveTextContent('0782102154')
    expect(call).toHaveAttribute('href', 'tel:+250782102154')
  })

  it('shows the exact WhatsApp number with a wa.me link opening in a new tab', () => {
    renderPage()
    const wa = screen.getByTestId('support-whatsapp')
    expect(wa).toHaveTextContent('WhatsApp')
    expect(wa).toHaveTextContent('0793634217')
    expect(wa).toHaveAttribute('href', 'https://wa.me/250793634217')
    expect(wa).toHaveAttribute('target', '_blank')
    expect(wa.getAttribute('rel')).toMatch(/noopener/)
  })

  it('shows the exact support email with a mailto link', () => {
    renderPage()
    const email = screen.getByTestId('support-email')
    expect(email).toHaveTextContent('support@ozufy.com')
    expect(email).toHaveAttribute('href', 'mailto:support@ozufy.com')
  })

  it('reads every value from the single support-contact source', () => {
    renderPage()
    expect(screen.getByTestId('support-call')).toHaveAttribute('href', SUPPORT_CONTACTS.phoneHref)
    expect(screen.getByTestId('support-whatsapp')).toHaveAttribute('href', SUPPORT_CONTACTS.whatsappHref)
    expect(screen.getByTestId('support-email')).toHaveAttribute('href', SUPPORT_CONTACTS.emailHref)
  })
})

describe('contact form', () => {
  beforeEach(() => vi.clearAllMocks())

  it('has first/last name, email, country code (Rwanda by default), phone and message', () => {
    renderPage()
    for (const label of [/^First name/i, /^Last name/i, /^Email/i, /Phone number/i, /^Message/i]) {
      expect(field(label)).toBeInTheDocument()
    }
    expect(screen.getByRole('combobox', { name: /Country code/i })).toHaveTextContent('RW (+250)')
  })

  it('requires first and last name', async () => {
    const user = userEvent.setup()
    renderPage()
    fill({ first: '   ', last: '' })
    await user.click(send())
    expect(await screen.findByText('First name is required')).toBeInTheDocument()
    expect(screen.getByText('Last name is required')).toBeInTheDocument()
    expect(openMailClient).not.toHaveBeenCalled()
  })

  it('requires an email and rejects an invalid one', async () => {
    const user = userEvent.setup()
    renderPage()
    fill({ email: '' })
    await user.click(send())
    expect(await screen.findByText('Email is required')).toBeInTheDocument()

    fill({ email: 'not-an-email' })
    await user.click(send())
    expect(await screen.findByText('Enter a valid email address')).toBeInTheDocument()
    expect(openMailClient).not.toHaveBeenCalled()
  })

  it('requires a message', async () => {
    const user = userEvent.setup()
    renderPage()
    fill({ message: '  ' })
    await user.click(send())
    expect(await screen.findByText('Message is required')).toBeInTheDocument()
    expect(openMailClient).not.toHaveBeenCalled()
  })

  it('rejects an invalid Rwandan phone but treats the phone as optional', async () => {
    const user = userEvent.setup()
    renderPage()
    fill({ phone: '12345' })
    await user.click(send())
    expect(await screen.findByText('Enter a Rwandan mobile number (07XXXXXXXX)')).toBeInTheDocument()
    expect(openMailClient).not.toHaveBeenCalled()

    fill({ phone: '' })
    fireEvent.change(field(/Phone number/i), { target: { value: '' } })
    await user.click(send())
    expect(openMailClient).toHaveBeenCalledTimes(1)
  })

  it('rejects over-long fields and an over-long message', async () => {
    const user = userEvent.setup()
    renderPage()
    fill({ message: 'm'.repeat(1001) })
    await user.click(send())
    expect(await screen.findByText('This is too long')).toBeInTheDocument()
    expect(openMailClient).not.toHaveBeenCalled()
  })

  it('an invalid form does not launch any mail action and keeps what was typed', async () => {
    const user = userEvent.setup()
    renderPage()
    fill({ first: 'Alice', last: '', email: 'bad' })
    await user.click(send())
    expect(openMailClient).not.toHaveBeenCalled()
    expect(field(/^First name/i)).toHaveValue('Alice')
    expect(field(/^Email/i)).toHaveValue('bad')
    expect(screen.queryByTestId('contact-mail-opened')).not.toBeInTheDocument()
  })

  it('a valid form opens one correctly encoded email to support, and does not claim it was sent', async () => {
    const user = userEvent.setup()
    renderPage()
    fill({
      first: ' Alice ',
      last: 'Uwase',
      email: 'alice@example.com',
      phone: '0781234567',
      message: 'Muraho & hello?\nNeed help: 100% sure = yes.',
    })
    await user.click(send())

    expect(openMailClient).toHaveBeenCalledTimes(1)
    const { url, base, subject, body } = mailtoParts()
    expect(base).toBe('mailto:support@ozufy.com')
    expect(subject).toBe('OuWealth support request from Alice Uwase')
    expect(body).toBe(
      [
        'Name: Alice Uwase',
        'Email: alice@example.com',
        'Phone: +250 781234567',
        '',
        'Message:',
        'Muraho & hello?\nNeed help: 100% sure = yes.',
      ].join('\n'),
    )
    // properly percent-encoded: no raw spaces, ampersands in the message, or newlines in the URL
    expect(url).not.toMatch(/[ \n]/)
    expect(url.split('?')[1].split('&')).toHaveLength(2) // only subject and body; the message "&" is encoded
    expect(url).toContain('%0A')
    expect(url).toContain('%26')

    const info = await screen.findByTestId('contact-mail-opened')
    expect(info).toHaveTextContent(/email app/i)
    expect(screen.queryByText(/message sent|sent successfully/i)).not.toBeInTheDocument()
  })

  it('leaves the phone line out when no phone was given', async () => {
    const user = userEvent.setup()
    renderPage()
    fill()
    await user.click(send())
    expect(mailtoParts().body).not.toMatch(/Phone:/)
  })

  it('the WhatsApp button opens the support chat in a new tab with a prefilled greeting', () => {
    renderPage()
    const chat = screen.getByRole('link', { name: 'Chat on WhatsApp' })
    expect(chat.getAttribute('href')).toMatch(/^https:\/\/wa\.me\/250793634217\?text=/)
    expect(decodeURIComponent(chat.getAttribute('href')!.split('?text=')[1])).toMatch(/^Hello OuWealth Support/)
    expect(chat).toHaveAttribute('target', '_blank')
    expect(chat.getAttribute('rel')).toMatch(/noopener/)
    expect(openMailClient).not.toHaveBeenCalled()
  })

  it('stays fully usable on a phone-width viewport', async () => {
    const user = userEvent.setup()
    Object.defineProperty(window, 'innerWidth', { configurable: true, writable: true, value: 375 })
    window.dispatchEvent(new Event('resize'))
    renderPage()
    const form = screen.getByTestId('contact-form')
    // every field and both actions are present in the one form, in reading order
    const names = within(form)
      .getAllByRole('textbox')
      .map((el) => el.getAttribute('name'))
    expect(names).toEqual(['firstName', 'lastName', 'email', 'phone', 'message'])
    fill()
    await user.click(send())
    expect(openMailClient).toHaveBeenCalledTimes(1)
  })
})

describe('compact contact layout', () => {
  it('centres one narrow column: about 560px of content, nearly full width on phones', () => {
    renderPage()
    const column = screen.getByTestId('contact-column')
    expect(cssFor(column)).toMatch(/max-width: 608px/)
    const card = screen.getByTestId('contact-form-card')
    const css = cssFor(card)
    expect(css).toMatch(/max-width: 560px/)
    expect(css).toMatch(/width: 100%/)
    expect(css).toMatch(/margin-(left|inline)[^;]*auto|margin: [^;]*auto/)
  })

  it('keeps the support channels in one compact card as wide as the form, with the three links as rows', () => {
    renderPage()
    const methods = screen.getByTestId('support-methods')
    expect(within(methods).getAllByRole('link')).toHaveLength(3)
    expect(screen.getByTestId('contact-column')).toContainElement(methods)
    expect(screen.getByTestId('contact-column')).toContainElement(screen.getByTestId('contact-form-card'))
    // not a three-column grid any more
    expect(cssFor(methods)).not.toMatch(/grid-template-columns/)
  })

  it('keeps the fields full width inside the compact card', () => {
    renderPage()
    const card = screen.getByTestId('contact-form-card')
    for (const label of [/^Email/i, /^Message/i]) {
      expect(within(card).getByLabelText(label).closest('.MuiFormControl-root')).toHaveClass('MuiFormControl-fullWidth')
    }
  })

  it('sizes Send message and Chat on WhatsApp to their content, grouped on the right', () => {
    renderPage()
    const actions = screen.getByTestId('contact-actions')
    expect(cssFor(actions)).toMatch(/justify-content: flex-end/)
    expect(cssFor(actions)).toMatch(/flex-direction: row/)
    const send = screen.getByRole('button', { name: 'Send message' })
    const chat = screen.getByRole('link', { name: 'Chat on WhatsApp' })
    for (const el of [send, chat]) {
      expect(el).not.toHaveClass('MuiButton-fullWidth')
      expect(cssFor(el)).toMatch(/width: auto/)
      expect(cssFor(el)).toMatch(/min-width: 140px/)
    }
  })
})
