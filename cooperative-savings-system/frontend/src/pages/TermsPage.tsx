import { Alert, Box, Container, Typography } from '@mui/material'
import { useTranslation } from 'react-i18next'

/** Placeholder until owner-approved Terms & Conditions are supplied. */
export function TermsPage() {
  const { t } = useTranslation()

  return (
    <Box data-testid="terms-page" sx={{ py: { xs: 5, md: 8 } }}>
      <Container maxWidth="md">
        <Typography variant="h2" component="h1" gutterBottom sx={{ fontSize: { xs: '2rem', md: '2.5rem' } }}>
          {t('public.terms.title')}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          {t('public.terms.effective')}
        </Typography>
        <Alert severity="info" data-testid="terms-placeholder">
          {t('public.terms.placeholder')}
        </Alert>
      </Container>
    </Box>
  )
}
