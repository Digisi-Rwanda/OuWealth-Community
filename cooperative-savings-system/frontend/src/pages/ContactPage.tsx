import { Alert, Box, Container, Typography } from '@mui/material'
import { useTranslation } from 'react-i18next'

/**
 * Public contact page shell. No corporate support email/phone is configured in-repo yet.
 * Cooperative contact fields must never be shown as OuWealth support.
 */
export function ContactPage() {
  const { t } = useTranslation()

  return (
    <Box data-testid="contact-page" sx={{ py: { xs: 5, md: 8 } }}>
      <Container maxWidth="md">
        <Typography variant="h2" component="h1" gutterBottom sx={{ fontSize: { xs: '2rem', md: '2.5rem' } }}>
          {t('public.contact.title')}
        </Typography>
        <Typography color="text.secondary" paragraph>
          {t('public.contact.intro')}
        </Typography>
        <Alert severity="info" data-testid="contact-placeholder">
          {t('public.contact.placeholder')}
        </Alert>
      </Container>
    </Box>
  )
}
