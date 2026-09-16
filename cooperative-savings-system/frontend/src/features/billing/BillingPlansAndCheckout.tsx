import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  FormControl,
  FormControlLabel,
  FormLabel,
  Radio,
  RadioGroup,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getErrorMessage } from '@/shared/api/client'
import {
  billingPaymentQueryKey,
  billingPaymentsQueryKey,
  fetchSubscriptionPayment,
  isPaymentIntegrationUnavailableError,
  startBillingCheckout,
} from '@/shared/api/billing'
import { cooperativeSubscriptionQueryKey } from '@/shared/api/subscription'
import type {
  BillingCheckoutResponse,
  BillingCycle,
  BillingPaymentChannel,
  BillingPlanQuote,
  BillingPlansResponse,
  SubscriptionPaymentStatus,
} from '@/shared/types/billing'
import { formatMoney } from '@/shared/utils/formatMoney'
import { isValidRwandanPhone, normalizeRwandanPhone } from '@/shared/utils/rwandaCooperative'
import { equivalentMonthlyAmount } from './billingAccess'

interface BillingPlansAndCheckoutProps {
  cooperativeId: string
  plans: BillingPlansResponse
  canManage: boolean
  trialActive: boolean
}

export function BillingPlansAndCheckout({
  cooperativeId,
  plans,
  canManage,
  trialActive,
}: BillingPlansAndCheckoutProps) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [selectedCycle, setSelectedCycle] = useState<BillingCycle | null>(null)
  const [paymentChannel, setPaymentChannel] = useState<BillingPaymentChannel | null>(null)
  const [payerPhone, setPayerPhone] = useState('')
  const [phoneError, setPhoneError] = useState<string | null>(null)
  const [checkoutNotice, setCheckoutNotice] = useState<string | null>(null)
  const [activeCheckout, setActiveCheckout] = useState<BillingCheckoutResponse | null>(null)

  const monthly = plans.plans.find((plan) => plan.billingCycle === 'MONTHLY')
  const annual = plans.plans.find((plan) => plan.billingCycle === 'ANNUAL')
  const selectedPlan = plans.plans.find((plan) => plan.billingCycle === selectedCycle)
  const currency = plans.currency || 'RWF'
  const pendingPaymentId = activeCheckout?.paymentId
  const cardSelected = paymentChannel === 'CARD'
  const mtnSelected = paymentChannel === 'MTN_MOMO'

  const paymentQuery = useQuery({
    queryKey: pendingPaymentId
      ? billingPaymentQueryKey(cooperativeId, pendingPaymentId)
      : ['billing', 'payment', 'none'],
    queryFn: () => fetchSubscriptionPayment(cooperativeId, pendingPaymentId!),
    enabled: Boolean(pendingPaymentId),
    refetchInterval: (query) => {
      if (import.meta.env.MODE === 'test') return false
      return query.state.data?.status === 'PENDING' ? 5_000 : false
    },
  })

  const paymentStatus: SubscriptionPaymentStatus | undefined =
    paymentQuery.data?.status ?? activeCheckout?.status

  useEffect(() => {
    if (!paymentStatus || paymentStatus === 'PENDING') return
    void queryClient.invalidateQueries({ queryKey: cooperativeSubscriptionQueryKey(cooperativeId) })
    void queryClient.invalidateQueries({
      queryKey: ['cooperatives', cooperativeId, 'billing', 'payments'],
    })
  }, [cooperativeId, paymentStatus, queryClient])

  const checkout = useMutation({
    mutationFn: () => {
      const payload =
        paymentChannel === 'MTN_MOMO'
          ? {
              billingCycle: selectedCycle!,
              paymentChannel: paymentChannel!,
              payerPhoneNumber: normalizeRwandanPhone(payerPhone),
            }
          : {
              billingCycle: selectedCycle!,
              paymentChannel: paymentChannel!,
            }
      return startBillingCheckout(cooperativeId, payload)
    },
    onSuccess: (data) => {
      setCheckoutNotice(null)
      setActiveCheckout(data)
      void queryClient.invalidateQueries({ queryKey: billingPaymentsQueryKey(cooperativeId, { page: 0, size: 20 }) })
      void queryClient.invalidateQueries({
        queryKey: ['cooperatives', cooperativeId, 'billing', 'payments'],
      })
    },
    onError: (error) => {
      if (isPaymentIntegrationUnavailableError(error)) {
        setCheckoutNotice(getErrorMessage(error, t('subscription.billing.checkoutUnavailable')))
        return
      }
      setCheckoutNotice(getErrorMessage(error, t('errors.generic')))
    },
  })

  const startMtnCheckout = () => {
    if (!isValidRwandanPhone(payerPhone)) {
      setPhoneError(t('subscription.billing.payerPhoneInvalid'))
      return
    }
    setPhoneError(null)
    checkout.mutate()
  }

  const retryCheckout = () => {
    setActiveCheckout(null)
    setCheckoutNotice(null)
  }

  return (
    <Stack spacing={2} data-testid="billing-plans">
      <Typography variant="h6" component="h2">
        {t('subscription.billing.plansTitle')}
      </Typography>
      {trialActive ? (
        <Alert severity="info">{t('subscription.billing.trialPlansHint')}</Alert>
      ) : null}
      {!canManage ? (
        <Alert severity="info">{t('subscription.billing.memberNotice')}</Alert>
      ) : null}

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
        }}
      >
        {monthly ? (
          <PlanCard
            plan={monthly}
            currency={currency}
            selected={selectedCycle === 'MONTHLY'}
            canManage={canManage}
            onChoose={() => {
              setSelectedCycle('MONTHLY')
              setCheckoutNotice(null)
              setActiveCheckout(null)
            }}
          />
        ) : null}
        {annual ? (
          <PlanCard
            plan={annual}
            currency={currency}
            selected={selectedCycle === 'ANNUAL'}
            canManage={canManage}
            recommended
            onChoose={() => {
              setSelectedCycle('ANNUAL')
              setCheckoutNotice(null)
              setActiveCheckout(null)
            }}
          />
        ) : null}
      </Box>

      {canManage && selectedPlan ? (
        <Card variant="outlined" data-testid="payment-method-card">
          <CardContent>
            <FormControl>
              <FormLabel id="payment-method-label">{t('subscription.billing.paymentMethod')}</FormLabel>
              <RadioGroup
                aria-labelledby="payment-method-label"
                name="payment-channel"
                value={paymentChannel ?? ''}
                onChange={(event) => {
                  setPaymentChannel(event.target.value as BillingPaymentChannel)
                  setCheckoutNotice(null)
                  setActiveCheckout(null)
                  setPhoneError(null)
                }}
              >
                <FormControlLabel
                  value="MTN_MOMO"
                  control={<Radio />}
                  label={t('subscription.billing.mtnMomo')}
                />
                <FormControlLabel
                  value="CARD"
                  control={<Radio />}
                  label={t('subscription.billing.bankCard')}
                />
              </RadioGroup>
            </FormControl>
            {mtnSelected ? (
              <TextField
                sx={{ mt: 2 }}
                fullWidth
                required
                type="tel"
                autoComplete="tel"
                label={t('subscription.billing.payerPhone')}
                value={payerPhone}
                onChange={(event) => {
                  setPayerPhone(event.target.value)
                  setPhoneError(null)
                }}
                error={Boolean(phoneError)}
                helperText={phoneError ?? t('subscription.billing.payerPhoneHint')}
              />
            ) : null}
            {cardSelected ? (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                {t('subscription.billing.cardComingSoon')}
              </Typography>
            ) : null}
            <Button sx={{ mt: 1 }} onClick={() => setSelectedCycle(null)}>
              {t('subscription.billing.changePlan')}
            </Button>
          </CardContent>
        </Card>
      ) : null}

      {canManage && selectedPlan && paymentChannel ? (
        <Card variant="outlined" data-testid="billing-review-card">
          <CardContent>
            <Typography variant="h6" component="h3" gutterBottom>
              {t('subscription.billing.reviewTitle')}
            </Typography>
            <ReviewRow label={t('subscription.billing.reviewPlan')} value={t(`subscription.cycle.${selectedPlan.billingCycle}`)} />
            {selectedPlan.billingCycle === 'ANNUAL' && Number(selectedPlan.discountPercent) > 0 ? (
              <>
                <ReviewRow
                  label={t('subscription.billing.reviewListPrice')}
                  value={formatMoney(selectedPlan.listPrice, { currency })}
                />
                <ReviewRow
                  label={t('subscription.billing.reviewDiscount')}
                  value={`${Number(selectedPlan.discountPercent)}%`}
                />
                <ReviewRow
                  label={t('subscription.billing.reviewSavings')}
                  value={formatMoney(selectedPlan.savings, { currency })}
                />
              </>
            ) : null}
            <ReviewRow
              label={t('subscription.billing.reviewAmount')}
              value={formatMoney(selectedPlan.amount, { currency })}
            />
            <ReviewRow
              label={t('subscription.billing.reviewBilling')}
              value={
                selectedPlan.billingCycle === 'ANNUAL'
                  ? t('subscription.billing.oneYear')
                  : t('subscription.billing.oneMonth')
              }
            />
            <ReviewRow
              label={t('subscription.billing.reviewMethod')}
              value={
                paymentChannel === 'MTN_MOMO'
                  ? t('subscription.billing.mtnMomo')
                  : t('subscription.billing.bankCard')
              }
            />
            {mtnSelected && payerPhone ? (
              <ReviewRow label={t('subscription.billing.payerPhone')} value={normalizeRwandanPhone(payerPhone)} />
            ) : null}
            {cardSelected ? (
              <Alert severity="info" sx={{ mt: 2 }} data-testid="card-coming-soon">
                {t('subscription.billing.cardComingSoon')}
              </Alert>
            ) : null}
            {checkoutNotice ? (
              <Alert severity="info" sx={{ mt: 2 }} data-testid="checkout-unavailable">
                {checkoutNotice}
              </Alert>
            ) : null}
            {activeCheckout && paymentStatus === 'PENDING' ? (
              <Stack spacing={1} sx={{ mt: 2 }} data-testid="mtn-pending-status">
                <Alert severity="info">{t('subscription.billing.momoSent')}</Alert>
                <Alert severity="info">{t('subscription.billing.momoApprove')}</Alert>
                <Typography variant="body2">
                  {t('subscription.billing.colStatus')}: {t('subscription.billing.paymentStatus.PENDING')}
                </Typography>
              </Stack>
            ) : null}
            {paymentStatus === 'SUCCESS' ? (
              <Alert severity="success" sx={{ mt: 2 }} data-testid="payment-success">
                {t('subscription.billing.paymentSuccess')}
              </Alert>
            ) : null}
            {paymentStatus === 'FAILED' || paymentStatus === 'CANCELED' ? (
              <Alert severity="error" sx={{ mt: 2 }} data-testid="payment-failed">
                {t('subscription.billing.paymentFailed')}
              </Alert>
            ) : null}
          </CardContent>
          <CardActions sx={{ px: 2, pb: 2 }}>
            {cardSelected ? (
              <Button variant="contained" disabled>
                {t('subscription.billing.continuePayment')}
              </Button>
            ) : paymentStatus === 'PENDING' && pendingPaymentId ? (
              <Button
                variant="outlined"
                onClick={() => void paymentQuery.refetch()}
                disabled={paymentQuery.isFetching}
              >
                {t('subscription.billing.checkPaymentStatus')}
              </Button>
            ) : paymentStatus === 'FAILED' || paymentStatus === 'CANCELED' ? (
              <Button variant="contained" onClick={retryCheckout}>
                {t('subscription.billing.retryPayment')}
              </Button>
            ) : paymentStatus === 'SUCCESS' ? null : (
              <Button
                variant="contained"
                disabled={checkout.isPending}
                onClick={startMtnCheckout}
              >
                {t('subscription.billing.payWithMtn')}
              </Button>
            )}
          </CardActions>
        </Card>
      ) : null}
    </Stack>
  )
}

function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <Stack direction="row" justifyContent="space-between" spacing={2} sx={{ py: 0.5 }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body2">{value}</Typography>
    </Stack>
  )
}

function PlanCard({
  plan,
  currency,
  selected,
  canManage,
  recommended,
  onChoose,
}: {
  plan: BillingPlanQuote
  currency: string
  selected: boolean
  canManage: boolean
  recommended?: boolean
  onChoose: () => void
}) {
  const { t } = useTranslation()
  const annual = plan.billingCycle === 'ANNUAL'
  const monthlyEquivalent = equivalentMonthlyAmount(plan)
  const chooseLabel = annual
    ? t('subscription.billing.chooseAnnual')
    : t('subscription.billing.chooseMonthly')

  return (
    <Card
      variant="outlined"
      aria-pressed={canManage ? selected : undefined}
      data-testid={`plan-card-${plan.billingCycle}`}
      sx={{
        borderColor: selected ? 'primary.main' : 'divider',
        borderWidth: selected ? 2 : 1,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <CardContent sx={{ flexGrow: 1 }}>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
          <Typography variant="h6" component="h3">
            {t(`subscription.cycle.${plan.billingCycle}`)}
          </Typography>
          {recommended && Number(plan.discountPercent) > 0 ? (
            <Chip size="small" color="primary" label={t('subscription.billing.recommended')} />
          ) : null}
          {Number(plan.discountPercent) > 0 ? (
            <Chip
              size="small"
              variant="outlined"
              label={t('subscription.billing.savePercent', { percent: Number(plan.discountPercent) })}
            />
          ) : null}
        </Stack>
        <Typography variant="h4" component="p">
          {formatMoney(plan.amount, { currency })}
        </Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {annual ? t('subscription.billing.perYear') : t('subscription.billing.perMonth')}
        </Typography>
        {annual && Number(plan.listPrice) > Number(plan.amount) ? (
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ textDecoration: 'line-through' }}
            data-testid="annual-list-price"
          >
            {formatMoney(plan.listPrice, { currency })}
          </Typography>
        ) : null}
        {annual && Number(plan.savings) > 0 ? (
          <Typography variant="body2" data-testid="annual-savings">
            {t('subscription.billing.youSave', { amount: formatMoney(plan.savings, { currency }) })}
          </Typography>
        ) : null}
        {monthlyEquivalent != null ? (
          <Typography variant="body2" color="text.secondary">
            {t('subscription.billing.equivalentMonthly', {
              amount: formatMoney(monthlyEquivalent, { currency }),
            })}
          </Typography>
        ) : null}
        <Box component="ul" sx={{ pl: 2, mt: 2, mb: 0 }}>
          {annual ? (
            <>
              <li>
                <Typography variant="body2">
                  {t('subscription.billing.annualBenefitDiscount', {
                    percent: Number(plan.discountPercent),
                  })}
                </Typography>
              </li>
              <li>
                <Typography variant="body2">{t('subscription.billing.annualBenefitAccess')}</Typography>
              </li>
            </>
          ) : (
            <>
              <li>
                <Typography variant="body2">{t('subscription.billing.monthlyBenefitBilling')}</Typography>
              </li>
              <li>
                <Typography variant="body2">{t('subscription.billing.monthlyBenefitAccess')}</Typography>
              </li>
            </>
          )}
        </Box>
      </CardContent>
      {canManage ? (
        <CardActions sx={{ px: 2, pb: 2 }}>
          <Button
            fullWidth
            variant={selected ? 'contained' : 'outlined'}
            aria-pressed={selected}
            onClick={onChoose}
          >
            {chooseLabel}
          </Button>
        </CardActions>
      ) : null}
    </Card>
  )
}
