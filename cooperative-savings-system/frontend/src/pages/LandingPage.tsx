import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import AssessmentIcon from '@mui/icons-material/Assessment'
import FavoriteIcon from '@mui/icons-material/Favorite'
import GavelIcon from '@mui/icons-material/Gavel'
import GroupsIcon from '@mui/icons-material/Groups'
import PaymentsIcon from '@mui/icons-material/Payments'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong'
import SavingsIcon from '@mui/icons-material/Savings'
import ShowChartIcon from '@mui/icons-material/ShowChart'
import ReceiptIcon from '@mui/icons-material/Receipt'
import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Stack,
  Typography,
  useTheme,
} from '@mui/material'
import type { SvgIconComponent } from '@mui/icons-material'
import { useTranslation } from 'react-i18next'
import { Link as RouterLink } from 'react-router-dom'
import { OuWealthMark } from '@/features/branding/OuWealthMark'
import { ROUTES } from '@/shared/constants/routes'

const HOW_STEPS = [
  'public.landing.how.step1',
  'public.landing.how.step2',
  'public.landing.how.step3',
  'public.landing.how.step4',
  'public.landing.how.step5',
  'public.landing.how.step6',
] as const

const CAPABILITIES: { labelKey: string; descriptionKey: string; icon: SvgIconComponent }[] = [
  { labelKey: 'public.landing.capabilities.members', descriptionKey: 'public.landing.capabilities.membersDesc', icon: GroupsIcon },
  { labelKey: 'public.landing.capabilities.contributions', descriptionKey: 'public.landing.capabilities.contributionsDesc', icon: SavingsIcon },
  { labelKey: 'public.landing.capabilities.loans', descriptionKey: 'public.landing.capabilities.loansDesc', icon: AccountBalanceWalletIcon },
  { labelKey: 'public.landing.capabilities.fines', descriptionKey: 'public.landing.capabilities.finesDesc', icon: GavelIcon },
  { labelKey: 'public.landing.capabilities.social', descriptionKey: 'public.landing.capabilities.socialDesc', icon: FavoriteIcon },
  { labelKey: 'public.landing.capabilities.investments', descriptionKey: 'public.landing.capabilities.investmentsDesc', icon: ShowChartIcon },
  { labelKey: 'public.landing.capabilities.payouts', descriptionKey: 'public.landing.capabilities.payoutsDesc', icon: PaymentsIcon },
  { labelKey: 'public.landing.capabilities.reports', descriptionKey: 'public.landing.capabilities.reportsDesc', icon: AssessmentIcon },
  { labelKey: 'public.landing.capabilities.ledger', descriptionKey: 'public.landing.capabilities.ledgerDesc', icon: ReceiptLongIcon },
  { labelKey: 'public.landing.capabilities.billing', descriptionKey: 'public.landing.capabilities.billingDesc', icon: ReceiptIcon },
]

function HeroVisual() {
  const { t } = useTranslation()
  const theme = useTheme()
  const dark = theme.palette.mode === 'dark'

  return (
    <Box
      aria-hidden
      data-testid="landing-hero-visual"
      sx={{
        position: 'relative',
        borderRadius: 3,
        overflow: 'hidden',
        minHeight: { xs: 260, md: 360 },
        background: dark
          ? 'linear-gradient(145deg, #143A6B 0%, #0A0A0A 55%, #1a1208 100%)'
          : 'linear-gradient(145deg, #1B4D8C 0%, #4A7AB8 42%, #FF7A00 120%)',
        boxShadow: dark ? '0 24px 48px rgba(0,0,0,0.45)' : '0 24px 48px rgba(27,77,140,0.28)',
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          background:
            'radial-gradient(circle at 20% 25%, rgba(255,255,255,0.18) 0%, transparent 40%), radial-gradient(circle at 85% 75%, rgba(255,122,0,0.35) 0%, transparent 45%)',
        }}
      />
      <Stack
        spacing={2}
        alignItems="center"
        justifyContent="center"
        sx={{ position: 'relative', height: '100%', minHeight: { xs: 260, md: 360 }, px: 3, py: 4 }}
      >
        <OuWealthMark size={72} />
        <Typography
          variant="h5"
          component="p"
          sx={{ color: '#FFFFFF', textAlign: 'center', fontFamily: 'Georgia, serif', maxWidth: 320 }}
        >
          {t('public.landing.hero.visualTitle')}
        </Typography>
        <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.82)', textAlign: 'center', maxWidth: 300 }}>
          {t('public.landing.hero.visualSubtitle')}
        </Typography>
        {/* Reserved for a future approved marketing photograph */}
        <Box
          component="img"
          data-marketing-image-slot="hero-community"
          alt=""
          sx={{ display: 'none' }}
        />
      </Stack>
    </Box>
  )
}

export function LandingPage() {
  const { t } = useTranslation()
  const theme = useTheme()
  const dark = theme.palette.mode === 'dark'

  return (
    <Box data-testid="landing-page">
      <Box
        component="section"
        aria-labelledby="landing-hero-heading"
        sx={{
          py: { xs: 5, md: 8 },
          background: dark
            ? 'radial-gradient(ellipse at 10% 0%, rgba(27,77,140,0.35) 0%, transparent 50%), linear-gradient(180deg, #0A0A0A 0%, #121212 100%)'
            : 'radial-gradient(ellipse at 12% 0%, rgba(27,77,140,0.14) 0%, transparent 50%), radial-gradient(ellipse at 90% 20%, rgba(255,122,0,0.08) 0%, transparent 40%), linear-gradient(180deg, #FFFFFF 0%, #F4F8FD 100%)',
        }}
      >
        <Container maxWidth="lg">
          <Box
            sx={{
              display: 'grid',
              gap: { xs: 4, md: 6 },
              gridTemplateColumns: { xs: '1fr', md: '1.05fr 0.95fr' },
              alignItems: 'center',
            }}
          >
            <Stack spacing={2.5}>
              <Typography variant="overline" color="primary" sx={{ letterSpacing: '0.12em', fontWeight: 700 }}>
                {t('app.name')}
              </Typography>
              <Typography id="landing-hero-heading" variant="h2" component="h1" sx={{ fontSize: { xs: '2rem', md: '2.75rem' } }}>
                {t('public.landing.hero.title')}
              </Typography>
              <Typography variant="h6" component="p" color="text.secondary" sx={{ fontWeight: 400, maxWidth: 540 }}>
                {t('public.landing.hero.subtitle')}
              </Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ pt: 1 }}>
                <Button
                  component={RouterLink}
                  to={ROUTES.signup}
                  variant="contained"
                  color="secondary"
                  size="large"
                  data-testid="landing-cta-signup"
                >
                  {t('public.nav.createScheme')}
                </Button>
                <Button
                  component={RouterLink}
                  to={ROUTES.login}
                  variant="outlined"
                  size="large"
                  data-testid="landing-cta-login"
                >
                  {t('public.nav.login')}
                </Button>
              </Stack>
            </Stack>
            <HeroVisual />
          </Box>
        </Container>
      </Box>

      <Box component="section" aria-labelledby="landing-how-heading" sx={{ py: { xs: 6, md: 8 } }}>
        <Container maxWidth="lg">
          <Typography id="landing-how-heading" variant="h3" component="h2" gutterBottom>
            {t('public.landing.how.title')}
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 4, maxWidth: 640 }}>
            {t('public.landing.how.subtitle')}
          </Typography>
          <Box
            sx={{
              display: 'grid',
              gap: 2,
              gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(3, 1fr)' },
            }}
          >
            {HOW_STEPS.map((key, index) => (
              <Card key={key} variant="outlined" sx={{ height: '100%', bgcolor: 'background.paper' }}>
                <CardContent>
                  <Typography variant="overline" color="secondary.main" fontWeight={700}>
                    {t('public.landing.how.stepLabel', { step: index + 1 })}
                  </Typography>
                  <Typography variant="h6" component="h3" sx={{ mt: 0.5 }}>
                    {t(key)}
                  </Typography>
                </CardContent>
              </Card>
            ))}
          </Box>
        </Container>
      </Box>

      <Box
        component="section"
        aria-labelledby="landing-capabilities-heading"
        sx={{ py: { xs: 6, md: 8 }, bgcolor: dark ? 'rgba(255,255,255,0.03)' : 'rgba(27,77,140,0.04)' }}
      >
        <Container maxWidth="lg">
          <Typography id="landing-capabilities-heading" variant="h3" component="h2" gutterBottom>
            {t('public.landing.capabilities.title')}
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 4, maxWidth: 640 }}>
            {t('public.landing.capabilities.subtitle')}
          </Typography>
          <Box
            sx={{
              display: 'grid',
              gap: 2,
              gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(3, 1fr)', lg: 'repeat(5, 1fr)' },
            }}
          >
            {CAPABILITIES.map(({ labelKey, descriptionKey, icon: Icon }) => (
              <Card key={labelKey} variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <Icon color="primary" sx={{ mb: 1 }} aria-hidden />
                  <Typography variant="subtitle1" component="h3" fontWeight={700}>
                    {t(labelKey)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                    {t(descriptionKey)}
                  </Typography>
                </CardContent>
              </Card>
            ))}
          </Box>
        </Container>
      </Box>

      <Box component="section" aria-labelledby="landing-value-heading" sx={{ py: { xs: 6, md: 8 } }}>
        <Container maxWidth="lg">
          <Box
            sx={{
              display: 'grid',
              gap: 4,
              gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
              alignItems: 'center',
            }}
          >
            <Box>
              <Typography id="landing-value-heading" variant="h3" component="h2" gutterBottom>
                {t('public.landing.value.title')}
              </Typography>
              <Typography color="text.secondary" paragraph>
                {t('public.landing.value.body')}
              </Typography>
              <Stack component="ul" spacing={1} sx={{ pl: 2, m: 0 }}>
                {(['organize', 'visibility', 'obligations', 'position'] as const).map((item) => (
                  <Typography key={item} component="li" variant="body1">
                    {t(`public.landing.value.${item}`)}
                  </Typography>
                ))}
              </Stack>
            </Box>
            <Box
              aria-hidden
              sx={{
                borderRadius: 3,
                minHeight: 240,
                background: dark
                  ? 'linear-gradient(160deg, rgba(27,77,140,0.35), rgba(255,122,0,0.12))'
                  : 'linear-gradient(160deg, rgba(27,77,140,0.12), rgba(255,122,0,0.16))',
                border: '1px solid',
                borderColor: 'divider',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                px: 3,
              }}
            >
              <Typography variant="h5" component="p" sx={{ textAlign: 'center', maxWidth: 280, fontFamily: 'Georgia, serif' }}>
                {t('public.landing.value.visual')}
              </Typography>
            </Box>
          </Box>
        </Container>
      </Box>

      <Box
        component="section"
        aria-labelledby="landing-about-heading"
        sx={{ py: { xs: 5, md: 6 }, bgcolor: dark ? 'rgba(255,255,255,0.03)' : 'rgba(27,77,140,0.04)' }}
      >
        <Container maxWidth="md">
          <Typography id="landing-about-heading" variant="h4" component="h2" gutterBottom>
            {t('public.landing.aboutTeaser.title')}
          </Typography>
          <Typography color="text.secondary" paragraph>
            {t('public.landing.aboutTeaser.body')}
          </Typography>
          <Button component={RouterLink} to={ROUTES.about} variant="outlined" data-testid="landing-about-link">
            {t('public.landing.aboutTeaser.cta')}
          </Button>
        </Container>
      </Box>

      <Box component="section" aria-labelledby="landing-contact-heading" sx={{ py: { xs: 5, md: 7 } }}>
        <Container maxWidth="md">
          <Typography id="landing-contact-heading" variant="h4" component="h2" gutterBottom>
            {t('public.landing.contactTeaser.title')}
          </Typography>
          <Typography color="text.secondary" paragraph>
            {t('public.landing.contactTeaser.body')}
          </Typography>
          <Button component={RouterLink} to={ROUTES.contact} variant="contained" data-testid="landing-contact-link">
            {t('public.landing.contactTeaser.cta')}
          </Button>
        </Container>
      </Box>
    </Box>
  )
}
