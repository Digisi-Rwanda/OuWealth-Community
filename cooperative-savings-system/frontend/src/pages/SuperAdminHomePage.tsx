import AccountBalanceIcon from '@mui/icons-material/AccountBalance'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'
import GroupsIcon from '@mui/icons-material/Groups'
import HealthAndSafetyIcon from '@mui/icons-material/HealthAndSafety'
import PeopleAltIcon from '@mui/icons-material/PeopleAlt'
import { Box, Button, Grid, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link as RouterLink } from 'react-router-dom'
import { dashboardPageTitleSx } from '@/features/dashboard/dashboardTypography'
import { getErrorMessage } from '@/shared/api/client'
import { fetchPlatformOverviewIfAvailable } from '@/shared/api/dashboard'
import { SUPER_ADMINS_QUERY_KEY, fetchSuperAdmins } from '@/shared/api/superAdmins'
import { ErrorState } from '@/shared/components/ErrorState'
import { MetricCard } from '@/shared/components/MetricCard'
import { SUPER_ADMIN_ROUTES } from '@/shared/constants/routes'

/** Landing page of the system-administration portal: platform-level facts only, no Saving Scheme operations. */
export function SuperAdminHomePage() {
  const { t } = useTranslation()

  const overviewQuery = useQuery({
    queryKey: ['platform', 'dashboard', 'overview'],
    queryFn: fetchPlatformOverviewIfAvailable,
  })
  const adminsQuery = useQuery({ queryKey: SUPER_ADMINS_QUERY_KEY, queryFn: fetchSuperAdmins })

  const overview = overviewQuery.data ?? undefined

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" component="h1" sx={dashboardPageTitleSx}>
          {t('superAdmin.home.title')}
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5, maxWidth: 720 }}>
          {t('superAdmin.home.subtitle')}
        </Typography>
      </Box>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} useFlexGap sx={{ mb: 3, flexWrap: 'wrap' }}>
        <Button
          component={RouterLink}
          to={SUPER_ADMIN_ROUTES.admins}
          variant="contained"
          startIcon={<AdminPanelSettingsIcon />}
        >
          {t('superAdmin.home.manageAdmins')}
        </Button>
        <Button
          component={RouterLink}
          to={SUPER_ADMIN_ROUTES.cooperatives}
          variant="outlined"
          startIcon={<AccountBalanceIcon />}
        >
          {t('superAdmin.home.manageSchemes')}
        </Button>
        <Button
          component={RouterLink}
          to={SUPER_ADMIN_ROUTES.system}
          variant="outlined"
          startIcon={<HealthAndSafetyIcon />}
        >
          {t('superAdmin.home.systemHealth')}
        </Button>
      </Stack>

      {overviewQuery.isError ? (
        <Box sx={{ mb: 2 }}>
          <ErrorState
            message={getErrorMessage(overviewQuery.error)}
            onRetry={() => void overviewQuery.refetch()}
          />
        </Box>
      ) : null}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            scale="dashboard"
            label={t('superAdmin.home.totalSchemes')}
            value={overview ? String(overview.totalCooperatives) : undefined}
            hint={
              overview ? t('superAdmin.home.activeSchemesHint', { count: overview.activeCooperatives }) : undefined
            }
            icon={<AccountBalanceIcon fontSize="small" />}
            accent="blue"
            loading={overviewQuery.isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            scale="dashboard"
            label={t('superAdmin.home.totalMembers')}
            value={overview ? String(overview.totalMembers) : undefined}
            hint={
              overview ? t('superAdmin.home.activeMembersHint', { count: overview.activeMembers }) : undefined
            }
            icon={<GroupsIcon fontSize="small" />}
            accent="green"
            loading={overviewQuery.isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            scale="dashboard"
            label={t('superAdmin.home.totalUsers')}
            value={overview ? String(overview.totalUsers) : undefined}
            icon={<PeopleAltIcon fontSize="small" />}
            accent="purple"
            loading={overviewQuery.isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            scale="dashboard"
            label={t('superAdmin.home.totalSuperAdmins')}
            value={adminsQuery.data ? String(adminsQuery.data.length) : undefined}
            icon={<AdminPanelSettingsIcon fontSize="small" />}
            accent="teal"
            loading={adminsQuery.isLoading}
          />
        </Grid>
      </Grid>
    </Box>
  )
}
