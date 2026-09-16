import { Alert, Typography } from '@mui/material'
import { useTranslation } from 'react-i18next'
import { useAppSelector } from '@/app/store/hooks'
import { selectIsLeadership } from '@/app/store/authSlice'
import { useCooperativeSubscription } from './useCooperativeSubscription'
import {
  TRIAL_WARNING_DAYS,
  accessUntilIso,
  formatSubscriptionDate,
} from './subscriptionAccess'

export function SubscriptionBanner() {
  const { t } = useTranslation()
  const isLeadership = useAppSelector(selectIsLeadership)
  const { cooperativeId, subscription, effectiveStatus, readOnly, isSuperAdmin, daysRemaining, isSuccess } =
    useCooperativeSubscription()

  if (!cooperativeId || !isSuccess) return null

  const until = formatSubscriptionDate(accessUntilIso(subscription))
  const trialEndingSoon =
    effectiveStatus === 'TRIAL' && daysRemaining != null && daysRemaining <= TRIAL_WARNING_DAYS
  const leadership = isLeadership || isSuperAdmin
  const membersRestricted = subscription?.writeAllowed === false

  if (effectiveStatus === 'ACTIVE' && !isSuperAdmin) {
    return null
  }

  if (effectiveStatus === 'TRIAL') {
    return (
      <Alert
        severity={trialEndingSoon ? 'warning' : 'info'}
        sx={{ mx: { xs: 2, sm: 3 }, mt: 2 }}
        data-testid="subscription-banner"
      >
        <Typography variant="subtitle2">{t('subscription.trial.title')}</Typography>
        <Typography variant="body2">
          {t('subscription.trial.body', { date: until || t('subscription.unknownDate') })}
        </Typography>
        {daysRemaining != null ? (
          <Typography variant="body2">
            {t('subscription.trial.remaining', { count: daysRemaining })}
          </Typography>
        ) : null}
      </Alert>
    )
  }

  if (effectiveStatus === 'PAST_DUE') {
    return (
      <Alert severity="warning" sx={{ mx: { xs: 2, sm: 3 }, mt: 2 }} data-testid="subscription-banner">
        <Typography variant="subtitle2">{t('subscription.pastDue.title')}</Typography>
        <Typography variant="body2">
          {leadership
            ? t('subscription.pastDue.leadership', { date: until || t('subscription.unknownDate') })
            : t('subscription.pastDue.member')}
        </Typography>
      </Alert>
    )
  }

  if (effectiveStatus === 'CANCELED' && !readOnly) {
    return (
      <Alert severity="info" sx={{ mx: { xs: 2, sm: 3 }, mt: 2 }} data-testid="subscription-banner">
        <Typography variant="subtitle2">{t('subscription.canceled.title')}</Typography>
        <Typography variant="body2">
          {t('subscription.canceled.body', { date: until || t('subscription.unknownDate') })}
        </Typography>
      </Alert>
    )
  }

  if (effectiveStatus === 'ACTIVE' && isSuperAdmin) {
    return null
  }

  if (readOnly || effectiveStatus === 'EXPIRED' || effectiveStatus === 'NONE' || effectiveStatus === 'CANCELED') {
    const none = effectiveStatus === 'NONE'
    return (
      <Alert severity="warning" sx={{ mx: { xs: 2, sm: 3 }, mt: 2 }} data-testid="subscription-banner">
        <Typography variant="subtitle2">
          {none ? t('subscription.none.title') : t('subscription.expired.title')}
        </Typography>
        <Typography variant="body2">
          {isSuperAdmin && membersRestricted
            ? t('subscription.superAdminNote')
            : leadership
              ? t('subscription.expired.leadership')
              : t('subscription.expired.member')}
        </Typography>
      </Alert>
    )
  }

  return null
}
