import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import FavoriteIcon from '@mui/icons-material/Favorite'
import GavelIcon from '@mui/icons-material/Gavel'
import PaymentsIcon from '@mui/icons-material/Payments'
import PercentIcon from '@mui/icons-material/Percent'
import SavingsIcon from '@mui/icons-material/Savings'
import { Box, Grid, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useAppSelector } from '@/app/store/hooks'
import { getErrorMessage } from '@/shared/api/client'
import { fetchMemberFinancialSummary } from '@/shared/api/members'
import { ErrorState } from '@/shared/components/ErrorState'
import { MetricCard } from '@/shared/components/MetricCard'
import { formatMoney } from '@/shared/utils/formatMoney'

const METRIC_COLS = { xs: 12, sm: 6, md: 4, lg: 2.4 }

interface MyMemberStatusSectionProps {
  cooperativeId: string
  compact?: boolean
}

/**
 * Personal savings status — officers are still members and pay contributions.
 * The member's quick actions are a separate section ({@link MemberQuickActionsSection}) rendered above this one.
 */
export function MyMemberStatusSection({ cooperativeId, compact = false }: MyMemberStatusSectionProps) {
  const { t } = useTranslation()
  const user = useAppSelector((s) => s.auth.user)

  const summaryQuery = useQuery({
    queryKey: ['members', 'financial-summary', cooperativeId, user?.id],
    queryFn: () => fetchMemberFinancialSummary(cooperativeId, user!.id),
    enabled: Boolean(cooperativeId && user?.id),
  })

  const summary = summaryQuery.data
  const currency = summary?.currency || 'RWF'
  const loading = summaryQuery.isLoading
  const money = (value: string | number | null | undefined) =>
    formatMoney(value ?? 0, { currency })

  const outstandingLoan =
    (Number(summary?.outstandingLoanPrincipal) || 0) +
    (Number(summary?.outstandingLoanInterest) || 0)

  return (
    <Box data-testid="my-member-status">
      <Typography variant={compact ? 'subtitle1' : 'h6'} sx={{ fontWeight: 700, mb: 0.5 }}>
        {t('dashboard.member.myStatusTitle')}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 720 }}>
        {t('dashboard.member.myStatusHint')}
      </Typography>

      {summaryQuery.isError ? (
        <Box sx={{ mb: 2 }}>
          <ErrorState
            message={getErrorMessage(summaryQuery.error)}
            onRetry={() => void summaryQuery.refetch()}
          />
        </Box>
      ) : null}

      <Grid container spacing={2}>
        <Grid size={METRIC_COLS}>
          <MetricCard
            label={t('dashboard.member.totalContributions')}
            value={money(summary?.actualContributions)}
            icon={<SavingsIcon fontSize="small" />}
            accent="blue"
            loading={loading}
          />
        </Grid>
        <Grid size={METRIC_COLS}>
          <MetricCard
            label={t('dashboard.member.outstandingLoan')}
            value={money(outstandingLoan)}
            icon={<AccountBalanceWalletIcon fontSize="small" />}
            accent="orange"
            loading={loading}
          />
        </Grid>
        <Grid size={METRIC_COLS}>
          <MetricCard
            label={t('dashboard.member.outstandingFines')}
            value={money(summary?.unpaidFines)}
            icon={<GavelIcon fontSize="small" />}
            accent="red"
            loading={loading}
          />
        </Grid>
        <Grid size={METRIC_COLS}>
          <MetricCard
            label={t('dashboard.member.socialContributions')}
            value={money(summary?.socialContributions)}
            icon={<FavoriteIcon fontSize="small" />}
            accent="purple"
            loading={loading}
          />
        </Grid>
        <Grid size={METRIC_COLS}>
          <MetricCard
            label={t('dashboard.member.contributionPercentage')}
            value={
              summary?.contributionPercentage != null && summary.contributionPercentage !== ''
                ? `${Number(summary.contributionPercentage).toFixed(2)}%`
                : '—'
            }
            icon={<PercentIcon fontSize="small" />}
            accent="green"
            loading={loading}
          />
        </Grid>
        <Grid size={METRIC_COLS}>
          <MetricCard
            label={t('dashboard.member.sharesHeld')}
            value={summary?.sharesHeld != null ? String(summary.sharesHeld) : '—'}
            icon={<PaymentsIcon fontSize="small" />}
            accent="blue"
            loading={loading}
          />
        </Grid>
        <Grid size={METRIC_COLS}>
          <MetricCard
            label={t('dashboard.member.currentShareValue')}
            value={money(summary?.currentShareValue)}
            icon={<PaymentsIcon fontSize="small" />}
            accent="green"
            loading={loading}
          />
        </Grid>
        <Grid size={METRIC_COLS}>
          <MetricCard
            label={t('dashboard.member.totalShareValue')}
            value={money(summary?.totalShareValue)}
            icon={<PaymentsIcon fontSize="small" />}
            accent="purple"
            loading={loading}
          />
        </Grid>
      </Grid>
    </Box>
  )
}
