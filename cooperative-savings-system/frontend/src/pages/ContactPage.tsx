import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined'
import PhoneInTalkIcon from '@mui/icons-material/PhoneInTalk'
import SendIcon from '@mui/icons-material/Send'
import WhatsAppIcon from '@mui/icons-material/WhatsApp'
import {
  Alert,
  Box,
  Button,
  Container,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { yupResolver } from '@hookform/resolvers/yup'
import { useMemo, useState, type ReactNode } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import {
  buildContactSchema,
  buildSupportMailto,
  contactDefaults,
  COUNTRY_CODES,
  CONTACT_LIMITS,
  type ContactFormValues,
} from '@/features/contact/contactForm'
import { openMailClient } from '@/features/contact/openMailClient'
import { SUPPORT_CONTACTS, whatsappHrefWithText } from '@/shared/constants/supportContacts'

const WHATSAPP_GREEN = '#1FA855'

/** Public contact page: direct support channels plus a form that prepares an email to OuWealth support. */
export function ContactPage() {
  const { t } = useTranslation()
  const [mailOpened, setMailOpened] = useState(false)
  const schema = useMemo(() => buildContactSchema(t), [t])

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<ContactFormValues>({
    resolver: yupResolver(schema),
    defaultValues: contactDefaults,
  })

  const onValid = handleSubmit((values) => {
    openMailClient(buildSupportMailto(values, t))
    setMailOpened(true)
  })

  return (
    <Box data-testid="contact-page" sx={{ py: { xs: 4, md: 7 } }}>
      <Container maxWidth="md">
        <Typography variant="h2" component="h1" gutterBottom sx={{ fontSize: { xs: '2rem', md: '2.5rem' } }}>
          {t('public.contact.title')}
        </Typography>
        <Typography color="text.secondary" component="p" sx={{ mb: 3, maxWidth: 640 }}>
          {t('public.contact.intro')}
        </Typography>

        <Box
          component="section"
          aria-label={t('public.contact.methodsTitle')}
          data-testid="support-methods"
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' },
            gap: 2,
            mb: 3,
          }}
        >
          <SupportMethod
            testId="support-call"
            icon={<PhoneInTalkIcon />}
            label={t('support.callUs')}
            value={SUPPORT_CONTACTS.phoneDisplay}
            href={SUPPORT_CONTACTS.phoneHref}
            accent="primary.main"
          />
          <SupportMethod
            testId="support-whatsapp"
            icon={<WhatsAppIcon />}
            label={t('support.whatsapp')}
            value={SUPPORT_CONTACTS.whatsappDisplay}
            href={SUPPORT_CONTACTS.whatsappHref}
            accent={WHATSAPP_GREEN}
            external
          />
          <SupportMethod
            testId="support-email"
            icon={<EmailOutlinedIcon />}
            label={t('support.email')}
            value={SUPPORT_CONTACTS.email}
            href={SUPPORT_CONTACTS.emailHref}
            accent="secondary.main"
          />
        </Box>

        <Paper
          elevation={0}
          sx={{
            p: { xs: 2.5, sm: 4 },
            border: '1px solid',
            borderColor: 'divider',
            borderRadius: 3,
            boxShadow: '0 12px 32px rgba(15, 23, 42, 0.08)',
          }}
        >
          <Typography variant="h5" component="h2" gutterBottom>
            {t('public.contact.formTitle')}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
            {t('public.contact.formHint')}
          </Typography>

          <Box component="form" onSubmit={onValid} noValidate data-testid="contact-form">
            <Stack spacing={2.5}>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label={t('public.contact.firstName')}
                  autoComplete="given-name"
                  required
                  fullWidth
                  error={Boolean(errors.firstName)}
                  helperText={errors.firstName?.message}
                  slotProps={{ htmlInput: { maxLength: CONTACT_LIMITS.name + 20 } }}
                  {...register('firstName')}
                />
                <TextField
                  label={t('public.contact.lastName')}
                  autoComplete="family-name"
                  required
                  fullWidth
                  error={Boolean(errors.lastName)}
                  helperText={errors.lastName?.message}
                  slotProps={{ htmlInput: { maxLength: CONTACT_LIMITS.name + 20 } }}
                  {...register('lastName')}
                />
              </Stack>
              <TextField
                label={t('public.contact.email')}
                type="email"
                autoComplete="email"
                required
                fullWidth
                error={Boolean(errors.email)}
                helperText={errors.email?.message}
                {...register('email')}
              />
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <Controller
                  name="countryCode"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      select
                      label={t('public.contact.countryCode')}
                      required
                      error={Boolean(errors.countryCode)}
                      helperText={errors.countryCode?.message}
                      sx={{ width: { sm: 190 }, flexShrink: 0 }}
                    >
                      {COUNTRY_CODES.map((country) => (
                        <MenuItem key={country.iso} value={country.dial}>
                          {country.label}
                        </MenuItem>
                      ))}
                    </TextField>
                  )}
                />
                <TextField
                  label={t('public.contact.phone')}
                  type="tel"
                  autoComplete="tel-national"
                  placeholder="07XXXXXXXX"
                  fullWidth
                  error={Boolean(errors.phone)}
                  helperText={errors.phone?.message ?? t('public.contact.phoneHint')}
                  {...register('phone')}
                />
              </Stack>
              <TextField
                label={t('public.contact.message')}
                required
                fullWidth
                multiline
                minRows={4}
                error={Boolean(errors.message)}
                helperText={errors.message?.message}
                {...register('message')}
              />

              {mailOpened ? (
                <Alert severity="info" data-testid="contact-mail-opened">
                  {t('public.contact.mailOpened')}
                </Alert>
              ) : null}

              <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1.5}
                sx={{ justifyContent: { sm: 'flex-end' } }}
              >
                <Button
                  component="a"
                  href={whatsappHrefWithText(t('public.contact.whatsappPrefill'))}
                  target="_blank"
                  rel="noopener noreferrer"
                  variant="outlined"
                  startIcon={<WhatsAppIcon />}
                  sx={{
                    minHeight: 44,
                    px: 3,
                    color: WHATSAPP_GREEN,
                    borderColor: WHATSAPP_GREEN,
                    '&:hover': { borderColor: '#188A45', bgcolor: 'rgba(31,168,85,0.08)' },
                  }}
                >
                  {t('public.contact.whatsappChat')}
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={<SendIcon />}
                  sx={{ minHeight: 44, px: 3 }}
                >
                  {t('public.contact.send')}
                </Button>
              </Stack>
            </Stack>
          </Box>
        </Paper>
      </Container>
    </Box>
  )
}

interface SupportMethodProps {
  testId: string
  icon: ReactNode
  label: string
  value: string
  href: string
  accent: string
  external?: boolean
}

function SupportMethod({ testId, icon, label, value, href, accent, external }: SupportMethodProps) {
  return (
    <Paper
      component="a"
      href={href}
      target={external ? '_blank' : undefined}
      rel={external ? 'noopener noreferrer' : undefined}
      data-testid={testId}
      variant="outlined"
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1.5,
        p: 2,
        borderRadius: 2.5,
        textDecoration: 'none',
        color: 'text.primary',
        minHeight: 64,
        transition: 'border-color 150ms ease, box-shadow 150ms ease',
        '&:hover': { borderColor: accent, boxShadow: '0 6px 18px rgba(15, 23, 42, 0.10)' },
        '&:focus-visible': { outline: '2px solid', outlineColor: accent, outlineOffset: 2 },
      }}
    >
      <Box
        aria-hidden="true"
        sx={{
          width: 40,
          height: 40,
          borderRadius: '50%',
          display: 'grid',
          placeItems: 'center',
          color: '#FFFFFF',
          bgcolor: accent,
          flexShrink: 0,
        }}
      >
        {icon}
      </Box>
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="caption" color="text.secondary" component="span" sx={{ display: 'block' }}>
          {label}
        </Typography>
        <Typography variant="body1" component="span" sx={{ display: 'block', fontWeight: 600, overflowWrap: 'anywhere' }}>
          {value}
        </Typography>
      </Box>
    </Paper>
  )
}
