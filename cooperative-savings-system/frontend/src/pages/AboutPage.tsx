import { Box, Container, Stack, Typography } from '@mui/material'
import { useTranslation } from 'react-i18next'

export function AboutPage() {
  const { t } = useTranslation()

  return (
    <Box data-testid="about-page" sx={{ py: { xs: 5, md: 8 } }}>
      <Container maxWidth="md">
        <Typography variant="overline" color="primary" fontWeight={700}>
          {t('app.name')}
        </Typography>
        <Typography variant="h2" component="h1" gutterBottom sx={{ fontSize: { xs: '2rem', md: '2.5rem' } }}>
          {t('public.about.title')}
        </Typography>
        <Typography color="text.secondary" paragraph sx={{ fontSize: '1.1rem' }}>
          {t('public.about.intro')}
        </Typography>
        <Typography paragraph>{t('public.about.purpose')}</Typography>
        <Typography variant="h5" component="h2" sx={{ mt: 4, mb: 2 }}>
          {t('public.about.areasTitle')}
        </Typography>
        <Stack component="ul" spacing={1.25} sx={{ pl: 2, m: 0 }}>
          {(['members', 'contributions', 'loans', 'social', 'investments', 'reports'] as const).map((key) => (
            <Typography key={key} component="li">
              {t(`public.about.areas.${key}`)}
            </Typography>
          ))}
        </Stack>
        <Typography color="text.secondary" sx={{ mt: 4 }}>
          {t('public.about.closing')}
        </Typography>
      </Container>
    </Box>
  )
}
