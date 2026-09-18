import { Alert, Box, Container, Typography } from '@mui/material'
import { useTranslation } from 'react-i18next'

/** Placeholder until an owner-approved Privacy Policy is supplied. */
export function PrivacyPage() {
  const { t } = useTranslation()

  return (
    <Box data-testid="privacy-page" sx={{ py: { xs: 5, md: 8 } }}>
      <Container maxWidth="md">
        <Typography variant="h2" component="h1" gutterBottom sx={{ fontSize: { xs: '2rem', md: '2.5rem' } }}>
          {t('public.privacy.title')}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          {t('public.privacy.effective')}
        </Typography>
        <Alert severity="info" data-testid="privacy-placeholder">
          {t('public.privacy.placeholder')}
        </Alert>
      </Container>
    </Box>
  )
}
