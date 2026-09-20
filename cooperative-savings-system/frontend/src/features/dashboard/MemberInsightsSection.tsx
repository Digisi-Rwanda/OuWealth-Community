import { Box, Link, List, ListItem, Paper, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAppSelector } from '@/app/store/hooks'
import { selectCanManageMembers } from '@/app/store/authSlice'
import { fetchDashboardMemberInsights } from '@/shared/api/dashboard'
import { getErrorMessage } from '@/shared/api/client'
import { ErrorState } from '@/shared/components/ErrorState'
import { ROUTES } from '@/shared/constants/routes'
import {
  canViewMemberFinanceInsights,
  canViewMemberLoanInsights,
  canViewAnyMemberInsights,
} from '@/shared/types/auth'
import { formatMoney } from '@/shared/utils/formatMoney'

interface MemberInsightsSectionProps {
  cooperativeId: string
}

export function MemberInsightsSection({ cooperativeId }: MemberInsightsSectionProps) {
  const { t } = useTranslation()
  const userRoles = useAppSelector((s) => s.auth.user?.roles ?? [])
  const canManageMembers = useAppSelector(selectCanManageMembers)
  const showFinance = canViewMemberFinanceInsights(userRoles)
  const showLoans = canViewMemberLoanInsights(userRoles)

  const query = useQuery({
    queryKey: ['dashboard', 'member-insights', cooperativeId],
    queryFn: () => fetchDashboardMemberInsights(cooperativeId),
    enabled: Boolean(cooperativeId) && canViewAnyMemberInsights(userRoles),
  })

  if (!canViewAnyMemberInsights(userRoles)) return null

  const data = query.data
  const currency = data?.currency || 'RWF'
  const money = (v: string | number | null | undefined) => formatMoney(v ?? 0, { currency })

  const memberLink = (memberId: string, displayName: string) => {
    if (!canManageMembers) {
      return (
        <Typography component="span" sx={{ fontWeight: 600 }}>
          {displayName}
        </Typography>
      )
    }
    return (
      <Link
        component={RouterLink}
        to={`${ROUTES.members}/${memberId}`}
        underline="hover"
        sx={{ fontWeight: 600 }}
      >
        {displayName}
      </Link>
    )
  }

  return (
    <Box sx={{ mb: 3 }} data-testid="member-insights">
      <Typography variant="h5" component="h2" sx={{ fontWeight: 700, mb: 0.5 }}>
        {t('dashboard.memberInsights.title')}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        {t('dashboard.memberInsights.subtitle')}
      </Typography>

      {query.isError ? (
        <Box sx={{ mb: 2 }}>
          <ErrorState message={getErrorMessage(query.error)} onRetry={() => void query.refetch()} />
        </Box>
      ) : null}

      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ alignItems: 'stretch' }}
        useFlexGap
      >
        {showFinance ? (
          <InsightRankCard
            testId="top-contributors-card"
            title={t('dashboard.memberInsights.topContributors')}
            hint={t('dashboard.memberInsights.topContributorsHint')}
            empty={t('dashboard.memberInsights.topContributorsEmpty')}
            loading={query.isLoading}
            rows={(data?.topContributors ?? []).map((row) => ({
              key: row.memberId,
              rank: row.rank,
              primary: memberLink(row.memberId, row.displayName),
              secondary: money(row.amount),
            }))}
          />
        ) : null}

        {showFinance ? (
          <InsightRankCard
            testId="fine-follow-up-card"
            title={t('dashboard.memberInsights.fineFollowUp')}
            hint={t('dashboard.memberInsights.fineFollowUpHint')}
            empty={t('dashboard.memberInsights.fineFollowUpEmpty')}
            loading={query.isLoading}
            rows={(data?.fineFollowUp ?? []).map((row) => ({
              key: row.memberId,
              rank: row.rank,
              primary: memberLink(row.memberId, row.displayName),
              secondary: money(row.outstandingAmount),
              meta: t('dashboard.memberInsights.fineFollowUpMeta', {
                count: row.fineCount,
                issued: money(row.issuedAmount),
              }),
            }))}
          />
        ) : null}

        {showLoans ? (
          <InsightRankCard
            testId="overdue-loans-card"
            title={t('dashboard.memberInsights.overdueLoans')}
            hint={t('dashboard.memberInsights.overdueLoansHint')}
            empty={t('dashboard.memberInsights.overdueLoansEmpty')}
            loading={query.isLoading}
            rows={(data?.overdueLoans ?? []).map((row) => ({
              key: row.memberId,
              rank: row.rank,
              primary: memberLink(row.memberId, row.displayName),
              secondary: money(row.outstandingPrincipal),
              meta: t('dashboard.memberInsights.overdueLoansMeta', {
                count: row.overdueLoanCount,
              }),
            }))}
          />
        ) : null}
      </Stack>
    </Box>
  )
}

interface RankRow {
  key: string
  rank: number
  primary: ReactNode
  secondary: string
  meta?: string
}

function InsightRankCard({
  title,
  hint,
  empty,
  rows,
  loading,
  testId,
}: {
  title: string
  hint: string
  empty: string
  rows: RankRow[]
  loading?: boolean
  testId: string
}) {
  const { t } = useTranslation()

  return (
    <Paper
      elevation={0}
      data-testid={testId}
      sx={{
        flex: 1,
        minWidth: 0,
        p: { xs: 2, md: 2.5 },
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
      }}
    >
      <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
        {title}
      </Typography>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
        {hint}
      </Typography>

      {loading ? (
        <Typography color="text.secondary">{t('common.loading')}</Typography>
      ) : rows.length === 0 ? (
        <Typography color="text.secondary" data-testid={`${testId}-empty`}>
          {empty}
        </Typography>
      ) : (
        <List dense disablePadding>
          {rows.map((row) => (
            <ListItem
              key={row.key}
              disableGutters
              sx={{
                py: 0.75,
                display: 'flex',
                alignItems: 'flex-start',
                gap: 1,
                borderBottom: '1px solid',
                borderColor: 'divider',
                '&:last-of-type': { borderBottom: 'none' },
              }}
            >
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ minWidth: 20, fontVariantNumeric: 'tabular-nums' }}
              >
                {row.rank}.
              </Typography>
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Box
                  sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 1,
                    flexWrap: 'wrap',
                  }}
                >
                  {row.primary}
                  <Typography
                    variant="body2"
                    sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}
                  >
                    {row.secondary}
                  </Typography>
                </Box>
                {row.meta ? (
                  <Typography variant="caption" color="text.secondary">
                    {row.meta}
                  </Typography>
                ) : null}
              </Box>
            </ListItem>
          ))}
        </List>
      )}
    </Paper>
  )
}
