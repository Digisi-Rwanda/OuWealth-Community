import type { TFunction } from 'i18next'
import * as yup from 'yup'
import { SUPPORT_CONTACTS } from '@/shared/constants/supportContacts'
import { isValidRwandanPhone, normalizeRwandanPhone } from '@/shared/utils/rwandaCooperative'

/**
 * Only Rwanda is offered for now: OuWealth support is a Rwandan service and we do not want to invent an
 * international dialling list. The value is still a real field so more countries can be added later.
 */
export const COUNTRY_CODES = [{ iso: 'RW', dial: '+250', label: 'RW (+250)' }] as const

export interface ContactFormValues {
  firstName: string
  lastName: string
  email: string
  countryCode: string
  phone: string
  message: string
}

export const CONTACT_LIMITS = { name: 80, email: 255, phone: 20, message: 1000 } as const

export const contactDefaults: ContactFormValues = {
  firstName: '',
  lastName: '',
  email: '',
  countryCode: COUNTRY_CODES[0].dial,
  phone: '',
  message: '',
}

export function buildContactSchema(t: TFunction): yup.ObjectSchema<ContactFormValues> {
  const tooLong = t('public.contact.validation.tooLong')
  return yup.object({
    firstName: yup
      .string()
      .trim()
      .required(t('public.contact.validation.firstNameRequired'))
      .max(CONTACT_LIMITS.name, tooLong),
    lastName: yup
      .string()
      .trim()
      .required(t('public.contact.validation.lastNameRequired'))
      .max(CONTACT_LIMITS.name, tooLong),
    email: yup
      .string()
      .trim()
      .required(t('public.contact.validation.emailRequired'))
      .max(CONTACT_LIMITS.email, tooLong)
      .email(t('public.contact.validation.emailInvalid')),
    countryCode: yup
      .string()
      .trim()
      .required(t('public.contact.validation.countryRequired'))
      .oneOf(
        COUNTRY_CODES.map((c) => c.dial as string),
        t('public.contact.validation.countryRequired'),
      ),
    phone: yup
      .string()
      .trim()
      .default('')
      .max(CONTACT_LIMITS.phone, tooLong)
      .test('rwanda-phone', t('public.contact.validation.phoneInvalid'), (value) => !value || isValidRwandanPhone(value)),
    message: yup
      .string()
      .trim()
      .required(t('public.contact.validation.messageRequired'))
      .max(CONTACT_LIMITS.message, tooLong),
  })
}

/** "+250 782102154" for a Rwandan number typed as 07XXXXXXXX or +2507XXXXXXXX; blank when none was given. */
export function formatInternationalPhone(values: Pick<ContactFormValues, 'countryCode' | 'phone'>): string {
  const phone = values.phone.trim()
  if (!phone) return ''
  const national = normalizeRwandanPhone(phone).replace(/^0/, '')
  return `${values.countryCode} ${national}`
}

/**
 * A pre-filled email to OuWealth support. There is no support-mail delivery service behind this form: the
 * visitor's own email app sends the message, so nothing here may claim it was sent.
 */
export function buildSupportMailto(values: ContactFormValues, t: TFunction): string {
  const name = `${values.firstName.trim()} ${values.lastName.trim()}`.trim()
  const phone = formatInternationalPhone(values)
  const lines = [
    `${t('public.contact.bodyName')}: ${name}`,
    `${t('public.contact.bodyEmail')}: ${values.email.trim()}`,
    ...(phone ? [`${t('public.contact.bodyPhone')}: ${phone}`] : []),
    '',
    `${t('public.contact.bodyMessage')}:`,
    values.message.trim(),
  ]
  const subject = t('public.contact.mailSubject', { name })
  return `${SUPPORT_CONTACTS.emailHref}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(lines.join('\n'))}`
}
