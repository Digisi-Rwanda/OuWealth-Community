import { Alert, Box, Stack, Typography } from '@mui/material'
import { useTranslation } from 'react-i18next'
import { ResponsiveTable, type TableColumn } from '@/shared/components/ResponsiveTable'
import type { LoanInstallment, LoanSchedulePreview as LoanSchedulePreviewData } from '@/shared/types/loan'
import { formatMoney } from '@/shared/utils/formatMoney'

interface LoanSchedulePreviewProps {
  schedule?: LoanSchedulePreviewData | null
  compact?: boolean
}

export function LoanSchedulePreview({ schedule, compact = false }: LoanSchedulePreviewProps) {
  const { t } = useTranslation()
  if (!schedule) return null

  const columns: TableColumn<LoanInstallment>[] = [
    {
      id: 'installmentNumber',
      label: t('loans.schedule.installmentNumber'),
      render: (row) => String(row.installmentNumber),
    },
    {
      id: 'dueDate',
      label: t('loans.fields.dueDate'),
      render: (row) => row.dueDate || '—',
    },
    {
      id: 'opening',
      label: t('loans.schedule.openingBalance'),
      render: (row) => formatMoney(row.openingPrincipalBalance ?? 0),
      hideOnMobile: true,
    },
    {
      id: 'principal',
      label: t('loans.schedule.principalComponent'),
      render: (row) => formatMoney(row.principalComponent),
    },
    {
      id: 'interest',
      label: t('loans.schedule.interestComponent'),
      render: (row) => formatMoney(row.interestComponent),
    },
    {
      id: 'penaltyDue',
      label: t('loans.schedule.penaltyDue'),
      render: (row) => formatMoney(row.penaltyDue ?? 0),
    },
    {
      id: 'total',
      label: t('loans.schedule.paymentAmount'),
      render: (row) => formatMoney(row.paymentAmount),
    },
    {
      id: 'principalPaid',
      label: t('loans.schedule.principalPaid'),
      render: (row) => formatMoney(row.principalPaid ?? 0),
      hideOnMobile: true,
    },
    {
      id: 'interestPaid',
      label: t('loans.schedule.interestPaid'),
      render: (row) => formatMoney(row.interestPaid ?? 0),
      hideOnMobile: true,
    },
    {
      id: 'penaltyPaid',
      label: t('loans.schedule.penaltyPaid'),
      render: (row) => formatMoney(row.penaltyPaid ?? 0),
      hideOnMobile: true,
    },
    {
      id: 'paid',
      label: t('loans.schedule.amountPaid'),
      render: (row) => formatMoney(row.amountPaid ?? 0),
      hideOnMobile: true,
    },
    {
      id: 'balance',
      label: t('loans.schedule.balance'),
      render: (row) => formatMoney(row.remainingAmount ?? row.balance ?? row.paymentAmount),
    },
    {
      id: 'status',
      label: t('loans.fields.status'),
      render: (row) =>
        t(`loans.schedule.status.${row.status ?? 'PENDING'}`, {
          defaultValue: String(row.status ?? 'PENDING'),
        }),
    },
  ]

  return (
    <Box>
      <Alert severity="info" sx={{ mb: compact ? 1.5 : 2 }}>
        {t('loans.schedule.flatInterestNotice')}
        {!schedule.scheduleFinalized ? ` ${t('loans.schedule.previewDisclaimer')}` : ''}
        {` ${t('loans.schedule.contractualNotice')}`}
      </Alert>
      <Stack spacing={0.75} sx={{ mb: schedule.installments?.length ? 2 : 0 }}>
        {schedule.repaymentDateModel ? (
          <Typography variant="body2">
            {t('loans.fields.repaymentDateModel')}:{' '}
            {t(`loans.schedule.repaymentModel.${schedule.repaymentDateModel}`, {
              defaultValue: String(schedule.repaymentDateModel),
            })}
          </Typography>
        ) : null}
        <Typography variant="body2">
          {t('loans.schedule.regularMonthlyInterest')}: {formatMoney(schedule.regularMonthlyInterest)}
        </Typography>
        {schedule.prorataEnabled ? (
          <Typography variant="body2">
            {t('loans.schedule.firstPeriodInterest')}: {formatMoney(schedule.firstPeriodInterest)}
            {schedule.firstPeriodDays != null
              ? ` (${t('loans.schedule.firstPeriodDaysValue', { days: schedule.firstPeriodDays })})`
              : ''}
          </Typography>
        ) : null}
        <Typography variant="body2">
          {t('loans.schedule.totalInterest')}: {formatMoney(schedule.totalInterest)}
        </Typography>
        <Typography variant="body2">
          {t('loans.schedule.totalRepayment')}: {formatMoney(schedule.totalRepayment)}
        </Typography>
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          {t('loans.schedule.equalInstallment')}: {formatMoney(schedule.equalInstallmentAmount)}
        </Typography>
      </Stack>
      {schedule.installments && schedule.installments.length > 0 && !compact ? (
        <ResponsiveTable
          columns={columns}
          rows={schedule.installments}
          getRowId={(row) => String(row.id ?? row.installmentNumber)}
        />
      ) : null}
    </Box>
  )
}
