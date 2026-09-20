import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined'
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Container,
  Stack,
  Typography,
  useTheme,
} from '@mui/material'
import { useTranslation } from 'react-i18next'
import { Link as RouterLink } from 'react-router-dom'
import { PUBLIC_SUBSCRIPTION_PRICING } from '@/shared/constants/publicPricing'
import { ROUTES } from '@/shared/constants/routes'
import { formatMoney } from '@/shared/utils/formatMoney'

const pricing = PUBLIC_SUBSCRIPTION_PRICING

function money(amount: number) {
  const formatted = formatMoney(amount, {
    currency: pricing.currency,
    locale: 'en-US',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  })
  // Keep the public marketing code as RWF (product spelling), not locale currency symbols.
  const digits = formatted.replace(/[^\d,.-]/g, '').trim()
  return `RWF ${digits}`
}

export function LandingPricingSection() {
  const { t } = useTranslation()
  const theme = useTheme()
  const dark = theme.palette.mode === 'dark'

  return (
    <Box
      component="section"
      id="pricing"
      data-testid="landing-pricing"
      aria-labelledby="landing-pricing-heading"
      sx={{
        py: { xs: 6, md: 8 },
        scrollMarginTop: { xs: 80, md: 96 },
        bgcolor: dark ? 'rgba(255,255,255,0.03)' : 'rgba(27,77,140,0.04)',
      }}
    >
      <Container maxWidth="lg">
        <Stack spacing={1.5} sx={{ mb: 4, maxWidth: 640 }}>
          <Typography id="landing-pricing-heading" variant="h3" component="h2">
            {t('public.landing.pricing.title')}
          </Typography>
          <Typography color="text.secondary">{t('public.landing.pricing.subtitle')}</Typography>
          <Typography variant="subtitle1" fontWeight={700} color="primary.main" data-testid="pricing-trial-lead">
            {t('public.landing.pricing.startFree', { months: pricing.trialMonths })}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t('public.landing.pricing.noPaymentNow')}
          </Typography>
        </Stack>

        <Box
          data-testid="landing-pricing-cards"
          sx={{
            display: 'grid',
            gap: 2.5,
            gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' },
            alignItems: 'stretch',
          }}
        >
          <Card variant="outlined" data-testid="pricing-card-trial" sx={{ height: '100%' }}>
            <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, height: '100%' }}>
              <Typography variant="overline" color="text.secondary" fontWeight={700}>
                {t('public.landing.pricing.trial.badge')}
              </Typography>
              <Typography variant="h5" component="h3">
                {t('public.landing.pricing.trial.name')}
              </Typography>
              <Typography variant="h4" component="p" data-testid="pricing-trial-months">
                {t('public.landing.pricing.trial.duration', { months: pricing.trialMonths })}
              </Typography>
              <Stack component="ul" spacing={1} sx={{ pl: 0, m: 0, listStyle: 'none', flex: 1 }}>
                {(['access', 'noPayment'] as const).map((key) => (
                  <Stack key={key} component="li" direction="row" spacing={1} alignItems="flex-start">
                    <CheckCircleOutlinedIcon color="primary" fontSize="small" sx={{ mt: 0.25 }} aria-hidden />
                    <Typography variant="body2">{t(`public.landing.pricing.trial.${key}`)}</Typography>
                  </Stack>
                ))}
              </Stack>
            </CardContent>
          </Card>

          <Card variant="outlined" data-testid="pricing-card-monthly" sx={{ height: '100%' }}>
            <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, height: '100%' }}>
              <Typography variant="overline" color="text.secondary" fontWeight={700}>
                {t('public.landing.pricing.monthly.badge')}
              </Typography>
              <Typography variant="h5" component="h3">
                {t('public.landing.pricing.monthly.name')}
              </Typography>
              <Typography variant="h4" component="p" data-testid="pricing-monthly-amount">
                {t('public.landing.pricing.monthly.price', { amount: money(pricing.monthlyAmount) })}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ flex: 1 }}>
                {t('public.landing.pricing.monthly.blurb')}
              </Typography>
            </CardContent>
          </Card>

          <Card
            variant="outlined"
            data-testid="pricing-card-annual"
            sx={{
              height: '100%',
              borderColor: 'secondary.main',
              borderWidth: 2,
              boxShadow: dark ? '0 12px 32px rgba(0,0,0,0.35)' : '0 12px 32px rgba(255,122,0,0.16)',
            }}
          >
            <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, height: '100%' }}>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                <Typography variant="overline" color="secondary.main" fontWeight={700}>
                  {t('public.landing.pricing.annual.badge')}
                </Typography>
                <Chip
                  size="small"
                  color="secondary"
                  label={t('public.landing.pricing.annual.savePercent', {
                    percent: pricing.annualDiscountPercent,
                  })}
                  data-testid="pricing-annual-discount"
                />
              </Stack>
              <Typography variant="h5" component="h3">
                {t('public.landing.pricing.annual.name')}
              </Typography>
              <Box>
                <Typography variant="h4" component="p" data-testid="pricing-annual-amount">
                  {t('public.landing.pricing.annual.price', { amount: money(pricing.annualAmount) })}
                </Typography>
                <Typography variant="body2" color="text.secondary" data-testid="pricing-annual-list">
                  <Box component="span" sx={{ textDecoration: 'line-through' }}>
                    {money(pricing.annualListPrice)}
                  </Box>
                  {' — '}
                  {t('public.landing.pricing.annual.listPriceLabel')}
                </Typography>
              </Box>
              <Stack component="ul" spacing={1} sx={{ pl: 0, m: 0, listStyle: 'none', flex: 1 }}>
                <Stack component="li" direction="row" spacing={1} alignItems="flex-start">
                  <CheckCircleOutlinedIcon color="secondary" fontSize="small" sx={{ mt: 0.25 }} aria-hidden />
                  <Typography variant="body2" data-testid="pricing-annual-savings">
                    {t('public.landing.pricing.annual.savings', {
                      amount: money(pricing.annualSavings),
                    })}
                  </Typography>
                </Stack>
                <Stack component="li" direction="row" spacing={1} alignItems="flex-start">
                  <CheckCircleOutlinedIcon color="secondary" fontSize="small" sx={{ mt: 0.25 }} aria-hidden />
                  <Typography variant="body2" data-testid="pricing-annual-equivalent">
                    {t('public.landing.pricing.annual.equivalent', {
                      amount: money(pricing.annualEquivalentMonthly),
                    })}
                  </Typography>
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        </Box>

        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1.5}
          sx={{ mt: 4 }}
          alignItems={{ xs: 'stretch', sm: 'center' }}
        >
          <Button
            component={RouterLink}
            to={ROUTES.signup}
            variant="contained"
            color="secondary"
            size="large"
            data-testid="pricing-cta-signup"
          >
            {t('public.nav.createScheme')}
          </Button>
          <Button
            component={RouterLink}
            to={ROUTES.login}
            variant="text"
            size="large"
            data-testid="pricing-cta-login"
          >
            {t('public.landing.pricing.loginCta')}
          </Button>
        </Stack>
      </Container>
    </Box>
  )
}
