import {
  Box,
  LinearProgress,
  Link,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAppSelector } from '@/app/store/hooks'
import { selectCanManageMembers } from '@/app/store/authSlice'
import { fetchDashboardAdvancedInsights } from '@/shared/api/dashboard'
import { getErrorMessage } from '@/shared/api/client'
import { ErrorState } from '@/shared/components/ErrorState'
import { ROUTES } from '@/shared/constants/routes'
import {
  canViewMemberFinanceInsights,
  canViewMemberLoanInsights,
  canViewAnyMemberInsights,
} from '@/shared/types/auth'
import { formatMoney } from '@/shared/utils/formatMoney'
import { InvestmentByMonthChart } from './InvestmentByMonthChart'
import { LoansDisbursedByMonthChart } from './LoansDisbursedByMonthChart'

interface AdvancedInsightsSectionProps {
  cooperativeId: string
}

export function AdvancedInsightsSection({ cooperativeId }: AdvancedInsightsSectionProps) {
  const { t } = useTranslation()
  const userRoles = useAppSelector((s) => s.auth.user?.roles ?? [])
  const canManageMembers = useAppSelector(selectCanManageMembers)
  const showLoans = canViewMemberLoanInsights(userRoles)
  const showFinance = canViewMemberFinanceInsights(userRoles)
  const [chartYear, setChartYear] = useState(dayjs().year())

  const query = useQuery({
    queryKey: ['dashboard', 'advanced-insights', cooperativeId],
    queryFn: () => fetchDashboardAdvancedInsights(cooperativeId),
    enabled: Boolean(cooperativeId) && canViewAnyMemberInsights(userRoles),
  })

  const yearOptions = useMemo(() => {
    const current = dayjs().year()
    return Array.from({ length: 6 }, (_, i) => current - 3 + i)
  }, [])

  if (!canViewAnyMemberInsights(userRoles)) return null

  const data = query.data
  const currency = data?.currency || 'RWF'
  const money = (v: string | number | null | undefined) => formatMoney(v ?? 0, { currency })
  const reliability = data?.repaymentReliability
  const reliabilityMembers = reliability?.members ?? []

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

  const formatRate = (value: string | number | null | undefined) => {
    const n = Number(value)
    if (!Number.isFinite(n)) return '—'
    return `${n.toFixed(1)}%`
  }

  return (
    <Box sx={{ mb: 3 }} data-testid="advanced-insights">
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1.5}
        sx={{ mb: 2, justifyContent: 'space-between', alignItems: { sm: 'flex-start' } }}
      >
        <Box>
          <Typography variant="h5" component="h2" sx={{ fontWeight: 700, mb: 0.5 }}>
            {t('dashboard.advancedInsights.title')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t('dashboard.advancedInsights.subtitle')}
          </Typography>
        </Box>
        {showLoans || showFinance ? (
          <TextField
            select
            size="small"
            label={t('contributions.fields.year')}
            value={chartYear}
            onChange={(e) => setChartYear(Number(e.target.value))}
            sx={{ minWidth: 110 }}
            data-testid="advanced-insights-year"
          >
            {yearOptions.map((y) => (
              <MenuItem key={y} value={y}>
                {y}
              </MenuItem>
            ))}
          </TextField>
        ) : null}
      </Stack>

      {query.isError ? (
        <Box sx={{ mb: 2 }}>
          <ErrorState message={getErrorMessage(query.error)} onRetry={() => void query.refetch()} />
        </Box>
      ) : null}

      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ mb: 2, alignItems: 'stretch' }}
        useFlexGap
      >
        {showLoans ? (
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <LoansDisbursedByMonthChart
              cooperativeId={cooperativeId}
              year={chartYear}
              currency={currency}
            />
          </Box>
        ) : null}
        {showFinance ? (
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <InvestmentByMonthChart
              cooperativeId={cooperativeId}
              year={chartYear}
              currency={currency}
            />
          </Box>
        ) : null}
      </Stack>

      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ alignItems: 'stretch' }}
        useFlexGap
      >
        {showLoans ? (
          <Paper
            elevation={0}
            data-testid="frequent-borrowers-card"
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
              {t('dashboard.advancedInsights.frequentBorrowers')}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
              {t('dashboard.advancedInsights.frequentBorrowersHint')}
            </Typography>
            {query.isLoading ? (
              <Typography color="text.secondary">{t('common.loading')}</Typography>
            ) : (data?.frequentBorrowers ?? []).length === 0 ? (
              <Typography color="text.secondary" data-testid="frequent-borrowers-empty">
                {t('dashboard.advancedInsights.frequentBorrowersEmpty')}
              </Typography>
            ) : (
              <Box sx={{ overflowX: 'auto' }}>
                <Table size="small" aria-label={t('dashboard.advancedInsights.frequentBorrowers')}>
                  <TableHead>
                    <TableRow>
                      <TableCell>#</TableCell>
                      <TableCell>{t('dashboard.advancedInsights.member')}</TableCell>
                      <TableCell align="right">{t('dashboard.advancedInsights.loans')}</TableCell>
                      <TableCell align="right">
                        {t('dashboard.advancedInsights.totalBorrowed')}
                      </TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(data?.frequentBorrowers ?? []).map((row) => (
                      <TableRow key={row.memberId}>
                        <TableCell>{row.rank}</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>{row.displayName}</TableCell>
                        <TableCell align="right">{row.numberOfLoansDisbursed}</TableCell>
                        <TableCell align="right">{money(row.totalPrincipalBorrowed)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Box>
            )}
          </Paper>
        ) : null}

        {showFinance ? (
          <Paper
            elevation={0}
            data-testid="largest-active-investments-card"
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
              {t('dashboard.advancedInsights.largestInvestments')}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
              {t('dashboard.advancedInsights.largestInvestmentsHint')}
            </Typography>
            {query.isLoading ? (
              <Typography color="text.secondary">{t('common.loading')}</Typography>
            ) : (data?.largestActiveInvestments ?? []).length === 0 ? (
              <Typography color="text.secondary" data-testid="largest-active-investments-empty">
                {t('dashboard.advancedInsights.largestInvestmentsEmpty')}
              </Typography>
            ) : (
              <Box sx={{ overflowX: 'auto' }}>
                <Table size="small" aria-label={t('dashboard.advancedInsights.largestInvestments')}>
                  <TableHead>
                    <TableRow>
                      <TableCell>{t('dashboard.advancedInsights.investmentName')}</TableCell>
                      <TableCell align="right">
                        {t('dashboard.advancedInsights.originalCapital')}
                      </TableCell>
                      <TableCell align="right">
                        {t('dashboard.advancedInsights.remainingCapital')}
                      </TableCell>
                      <TableCell align="right">
                        {t('dashboard.advancedInsights.profitReturned')}
                      </TableCell>
                      <TableCell>{t('dashboard.advancedInsights.status')}</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(data?.largestActiveInvestments ?? []).map((row) => (
                      <TableRow key={row.investmentId}>
                        <TableCell sx={{ fontWeight: 600 }}>{row.name}</TableCell>
                        <TableCell align="right">{money(row.originalCapital)}</TableCell>
                        <TableCell align="right">{money(row.remainingCapital)}</TableCell>
                        <TableCell align="right">{money(row.profitReturned)}</TableCell>
                        <TableCell>{row.status}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Box>
            )}
          </Paper>
        ) : null}
      </Stack>

      {showLoans ? (
        <Paper
          elevation={0}
          data-testid="repayment-reliability-card"
          sx={{
            mt: 2,
            p: { xs: 2, md: 2.5 },
            border: '1px solid',
            borderColor: 'divider',
            borderRadius: 2,
          }}
        >
          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
            {t('dashboard.advancedInsights.repaymentReliability')}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
            {t('dashboard.advancedInsights.repaymentReliabilityHint')}
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
            {t('dashboard.advancedInsights.repaymentReliabilityMinSample', {
              count: reliability?.minimumSample ?? 3,
            })}
          </Typography>
          {query.isLoading ? (
            <Typography color="text.secondary">{t('common.loading')}</Typography>
          ) : reliabilityMembers.length === 0 ? (
            <Typography color="text.secondary" data-testid="repayment-reliability-empty">
              {t('dashboard.advancedInsights.repaymentReliabilityEmpty')}
            </Typography>
          ) : (
            <Box sx={{ overflowX: 'auto' }}>
              <Table size="small" aria-label={t('dashboard.advancedInsights.repaymentReliability')}>
                <TableHead>
                  <TableRow>
                    <TableCell>{t('dashboard.advancedInsights.member')}</TableCell>
                    <TableCell sx={{ minWidth: 140 }}>
                      {t('dashboard.advancedInsights.onTimeRate')}
                    </TableCell>
                    <TableCell align="right">{t('dashboard.advancedInsights.onTime')}</TableCell>
                    <TableCell align="right">{t('dashboard.advancedInsights.paidLate')}</TableCell>
                    <TableCell align="right">{t('dashboard.advancedInsights.pastDue')}</TableCell>
                    <TableCell align="right">{t('dashboard.advancedInsights.evaluated')}</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {reliabilityMembers.map((row) => {
                    const rate = Number(row.onTimeRate) || 0
                    return (
                      <TableRow key={row.memberId}>
                        <TableCell>{memberLink(row.memberId, row.displayName)}</TableCell>
                        <TableCell>
                          <Stack spacing={0.5}>
                            <Typography variant="body2" sx={{ fontWeight: 600 }}>
                              {formatRate(row.onTimeRate)}
                            </Typography>
                            <LinearProgress
                              variant="determinate"
                              value={Math.max(0, Math.min(100, rate))}
                              aria-label={formatRate(row.onTimeRate)}
                              sx={{ height: 6, borderRadius: 1 }}
                            />
                          </Stack>
                        </TableCell>
                        <TableCell align="right">{row.installmentsPaidOnTime}</TableCell>
                        <TableCell align="right">{row.installmentsPaidLate}</TableCell>
                        <TableCell align="right">{row.installmentsUnpaidPastDue}</TableCell>
                        <TableCell align="right">{row.installmentsDue}</TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </Box>
          )}
        </Paper>
      ) : null}
    </Box>
  )
}
