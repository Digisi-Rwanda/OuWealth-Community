import GavelIcon from '@mui/icons-material/Gavel'
import { Paper, Stack, Typography } from '@mui/material'
import { useTranslation } from 'react-i18next'
import type { FinesInsights } from '@/shared/types/dashboard'
import { formatMoney } from '@/shared/utils/formatMoney'
import { MonthOverMonthDelta } from './MonthOverMonthDelta'

interface FineActivityCardProps {
  fines: FinesInsights | undefined
  currency?: string
  loading?: boolean
}

export function FineActivityCard({ fines, currency = 'RWF', loading }: FineActivityCardProps) {
  const { t } = useTranslation()
  const money = (v: string | number | null | undefined) => formatMoney(v ?? 0, { currency })

  return (
    <Paper
      elevation={0}
      data-testid="fine-activity-card"
      sx={{
        p: { xs: 2.5, md: 3 },
        height: '100%',
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
      }}
    >
      <Stack direction="row" spacing={1} sx={{ mb: 1.5, alignItems: 'center' }}>
        <GavelIcon fontSize="small" color="action" />
        <Typography variant="h6">{t('dashboard.insights.fineActivityTitle')}</Typography>
      </Stack>

      {loading || !fines ? (
        <Typography color="text.secondary">{t('common.loading')}</Typography>
      ) : (
        <Stack spacing={1.5}>
          <div>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', fontWeight: 700 }}>
              {t('dashboard.insights.finesIssued')}
            </Typography>
            <Typography variant="body1" sx={{ fontWeight: 600 }}>
              {t('dashboard.insights.finesIssuedValue', {
                count: fines.issuedCountCurrentMonth,
                amount: money(fines.issuedAmountCurrentMonth),
              })}
            </Typography>
          </div>
          <div>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', fontWeight: 700 }}>
              {t('dashboard.insights.finesCollected')}
            </Typography>
            <Typography variant="body1" sx={{ fontWeight: 600 }} data-testid="fines-collected-value">
              {money(fines.collectedCurrentMonth)}
            </Typography>
            <MonthOverMonthDelta
              changePercent={fines.collectedChangePercent}
              changeState={fines.collectedChangeState}
              semantics="neutral"
            />
          </div>
        </Stack>
      )}
    </Paper>
  )
}
