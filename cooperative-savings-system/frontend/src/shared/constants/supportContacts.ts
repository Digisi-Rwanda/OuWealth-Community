/**
 * Single source of truth for OuWealth support contact details. Every call, WhatsApp and email link in the
 * app (support dock, contact page, footer) must read from here.
 */
export const SUPPORT_CONTACTS = {
  phoneDisplay: '0782102154',
  phoneHref: 'tel:+250782102154',
  whatsappDisplay: '0793634217',
  whatsappHref: 'https://wa.me/250793634217',
  email: 'support@ozufy.com',
  emailHref: 'mailto:support@ozufy.com',
} as const

/** WhatsApp chat link with optional pre-filled text. */
export function whatsappHrefWithText(text?: string): string {
  const trimmed = text?.trim()
  return trimmed
    ? `${SUPPORT_CONTACTS.whatsappHref}?text=${encodeURIComponent(trimmed)}`
    : SUPPORT_CONTACTS.whatsappHref
}
