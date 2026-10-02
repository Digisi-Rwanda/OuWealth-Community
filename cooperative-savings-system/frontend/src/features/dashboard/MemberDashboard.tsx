import { Box, Typography } from '@mui/material'
import { useTranslation } from 'react-i18next'
import { useAppSelector } from '@/app/store/hooks'
import { DashboardSubscriptionCard } from '@/features/subscription/DashboardSubscriptionCard'
import { isActionNeeded } from '@/features/subscription/subscriptionAccess'
import { useCooperativeSubscription } from '@/features/subscription/useCooperativeSubscription'
import { MemberQuickActionsSection } from './MemberQuickActionsSection'
import { MonthlyContributionsChart } from './MonthlyContributionsChart'
import { MyMemberStatusSection } from './MyMemberStatusSection'

interface MemberDashboardProps {
  cooperativeId: string
}

export function MemberDashboard({ cooperativeId }: MemberDashboardProps) {
  const { t } = useTranslation()
  const user = useAppSelector((s) => s.auth.user)
  const { subscription, effectiveStatus, daysRemaining, isSuccess } =
    useCooperativeSubscription(cooperativeId)
  const showCard = isSuccess && isActionNeeded(effectiveStatus, daysRemaining)

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          {t('dashboard.member.welcome', { name: user?.firstName || user?.fullName || '' })}
        </Typography>
        <Typography variant="body1" color="text.secondary">
          {t('dashboard.member.description')}
        </Typography>
      </Box>

      {showCard ? <DashboardSubscriptionCard subscription={subscription} variant="member" /> : null}

      <Box sx={{ mb: 3 }}>
        <MemberQuickActionsSection cooperativeId={cooperativeId} />
      </Box>

      <Box sx={{ mb: 3 }}>
        <MyMemberStatusSection cooperativeId={cooperativeId} />
      </Box>

      <MonthlyContributionsChart cooperativeId={cooperativeId} currency="RWF" />
    </Box>
  )
}
