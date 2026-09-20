import ArrowBackIosNewIcon from '@mui/icons-material/ArrowBackIosNew'
import ArrowForwardIosIcon from '@mui/icons-material/ArrowForwardIos'
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import AssessmentIcon from '@mui/icons-material/Assessment'
import GroupsIcon from '@mui/icons-material/Groups'
import SavingsIcon from '@mui/icons-material/Savings'
import ShowChartIcon from '@mui/icons-material/ShowChart'
import Diversity3Icon from '@mui/icons-material/Diversity3'
import {
  Box,
  Button,
  Container,
  IconButton,
  Stack,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import type { SvgIconComponent } from '@mui/icons-material'
import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { OuWealthMark } from '@/features/branding/OuWealthMark'

const STEPS: {
  id: string
  titleKey: string
  bodyKey: string
  moduleKey: string
  icon: SvgIconComponent
}[] = [
  {
    id: 'community',
    titleKey: 'public.landing.journey.steps.community.title',
    bodyKey: 'public.landing.journey.steps.community.body',
    moduleKey: 'public.landing.journey.steps.community.module',
    icon: GroupsIcon,
  },
  {
    id: 'contributions',
    titleKey: 'public.landing.journey.steps.contributions.title',
    bodyKey: 'public.landing.journey.steps.contributions.body',
    moduleKey: 'public.landing.journey.steps.contributions.module',
    icon: SavingsIcon,
  },
  {
    id: 'loans',
    titleKey: 'public.landing.journey.steps.loans.title',
    bodyKey: 'public.landing.journey.steps.loans.body',
    moduleKey: 'public.landing.journey.steps.loans.module',
    icon: AccountBalanceWalletIcon,
  },
  {
    id: 'investments',
    titleKey: 'public.landing.journey.steps.investments.title',
    bodyKey: 'public.landing.journey.steps.investments.body',
    moduleKey: 'public.landing.journey.steps.investments.module',
    icon: ShowChartIcon,
  },
  {
    id: 'visibility',
    titleKey: 'public.landing.journey.steps.visibility.title',
    bodyKey: 'public.landing.journey.steps.visibility.body',
    moduleKey: 'public.landing.journey.steps.visibility.module',
    icon: AssessmentIcon,
  },
  {
    id: 'progress',
    titleKey: 'public.landing.journey.steps.progress.title',
    bodyKey: 'public.landing.journey.steps.progress.body',
    moduleKey: 'public.landing.journey.steps.progress.module',
    icon: Diversity3Icon,
  },
]

export function LandingJourneySection() {
  const { t } = useTranslation()
  const theme = useTheme()
  const dark = theme.palette.mode === 'dark'
  const reduceMotion = useMediaQuery('(prefers-reduced-motion: reduce)')
  const [index, setIndex] = useState(0)
  const headingId = useId()
  const panelId = useId()
  const step = STEPS[index]!
  const Icon = step.icon

  const go = (next: number) => {
    setIndex((next + STEPS.length) % STEPS.length)
  }

  return (
    <Box
      component="section"
      id="journey"
      data-testid="landing-journey"
      aria-labelledby={headingId}
      sx={{ py: { xs: 6, md: 8 } }}
    >
      <Container maxWidth="lg">
        <Typography id={headingId} variant="h3" component="h2" gutterBottom>
          {t('public.landing.journey.title')}
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 4, maxWidth: 640 }}>
          {t('public.landing.journey.subtitle')}
        </Typography>

        <Box
          sx={{
            display: 'grid',
            gap: 3,
            gridTemplateColumns: { xs: '1fr', md: '1fr 1.15fr' },
            alignItems: 'stretch',
          }}
        >
          <Box
            aria-hidden
            data-testid="journey-visual"
            data-marketing-image-slot={`journey-${step.id}`}
            sx={{
              borderRadius: 3,
              minHeight: { xs: 220, md: 340 },
              position: 'relative',
              overflow: 'hidden',
              border: '1px solid',
              borderColor: 'divider',
              background: dark
                ? 'linear-gradient(145deg, #143A6B 0%, #0A0A0A 55%, #1a1208 100%)'
                : 'linear-gradient(145deg, #1B4D8C 0%, #4A7AB8 42%, #FF7A00 120%)',
              transition: reduceMotion ? undefined : 'background 280ms ease',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexDirection: 'column',
              gap: 1.5,
              px: 3,
            }}
          >
            <OuWealthMark size={56} />
            <Icon sx={{ fontSize: 48, color: 'rgba(255,255,255,0.92)' }} />
            <Typography
              variant="subtitle1"
              sx={{ color: '#FFFFFF', textAlign: 'center', fontFamily: 'Georgia, serif', maxWidth: 260 }}
            >
              {t(step.titleKey)}
            </Typography>
            {/* Reserved for approved community photography */}
            <Box component="img" alt="" sx={{ display: 'none' }} />
          </Box>

          <Stack spacing={2} sx={{ minWidth: 0 }}>
            <Stack
              direction="row"
              spacing={1}
              flexWrap="wrap"
              useFlexGap
              role="tablist"
              aria-label={t('public.landing.journey.stepsAria')}
            >
              {STEPS.map((s, i) => (
                <Button
                  key={s.id}
                  role="tab"
                  id={`journey-tab-${s.id}`}
                  aria-selected={i === index}
                  aria-controls={panelId}
                  data-testid={`journey-step-tab-${s.id}`}
                  size="small"
                  variant={i === index ? 'contained' : 'outlined'}
                  color={i === index ? 'primary' : 'inherit'}
                  onClick={() => setIndex(i)}
                  sx={{ minHeight: 40, textTransform: 'none' }}
                >
                  {t('public.landing.journey.stepLabel', { step: i + 1 })}
                </Button>
              ))}
            </Stack>

            <Box
              role="tabpanel"
              id={panelId}
              aria-labelledby={`journey-tab-${step.id}`}
              data-testid={`journey-panel-${step.id}`}
              sx={{
                borderRadius: 3,
                border: '1px solid',
                borderColor: 'divider',
                bgcolor: 'background.paper',
                p: { xs: 2.5, md: 3 },
                minHeight: { xs: 200, md: 240 },
              }}
            >
              <Typography variant="overline" color="secondary.main" fontWeight={700}>
                {t('public.landing.journey.stepLabel', { step: index + 1 })}
              </Typography>
              <Typography variant="h5" component="h3" sx={{ mt: 0.5 }} data-testid="journey-step-title">
                {t(step.titleKey)}
              </Typography>
              <Typography color="text.secondary" sx={{ mt: 1.5 }} data-testid="journey-step-body">
                {t(step.bodyKey)}
              </Typography>
              <Typography variant="body2" sx={{ mt: 2 }} color="primary.main" fontWeight={600}>
                {t(step.moduleKey)}
              </Typography>
            </Box>

            <Stack direction="row" spacing={1} alignItems="center">
              <IconButton
                aria-label={t('public.landing.journey.prev')}
                onClick={() => go(index - 1)}
                data-testid="journey-prev"
                sx={{ minWidth: 44, minHeight: 44 }}
              >
                <ArrowBackIosNewIcon fontSize="small" />
              </IconButton>
              <Typography variant="body2" color="text.secondary" aria-live="polite">
                {t('public.landing.journey.progress', { current: index + 1, total: STEPS.length })}
              </Typography>
              <IconButton
                aria-label={t('public.landing.journey.next')}
                onClick={() => go(index + 1)}
                data-testid="journey-next"
                sx={{ minWidth: 44, minHeight: 44 }}
              >
                <ArrowForwardIosIcon fontSize="small" />
              </IconButton>
            </Stack>
          </Stack>
        </Box>
      </Container>
    </Box>
  )
}
