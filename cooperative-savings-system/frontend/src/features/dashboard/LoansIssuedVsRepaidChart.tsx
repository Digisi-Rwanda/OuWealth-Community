import { Box, Paper, Typography } from '@mui/material'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { formatMoney } from '@/shared/utils/formatMoney'

interface LoansIssuedVsRepaidChartProps {
  issuedAmount: string | number
  repaidAmount: string | number
  currency?: string
  loading?: boolean
}

export function LoansIssuedVsRepaidChart({
  issuedAmount,
  repaidAmount,
  currency = 'RWF',
  loading,
}: LoansIssuedVsRepaidChartProps) {
  const { t } = useTranslation()
  const issued = Number(issuedAmount) || 0
  const repaid = Number(repaidAmount) || 0
  const empty = issued === 0 && repaid === 0

  const data = useMemo(
    () => [
      { name: t('dashboard.insights.loansIssued'), amount: issued, fill: '#1B4D8C' },
      { name: t('dashboard.insights.loansRepaid'), amount: repaid, fill: '#FF7A00' },
    ],
    [issued, repaid, t],
  )

  return (
    <Paper
      elevation={0}
      data-testid="loans-issued-vs-repaid-chart"
      sx={{
        p: { xs: 2.5, md: 3 },
        height: '100%',
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
      }}
    >
      <Typography variant="h6" sx={{ mb: 1.5 }}>
        {t('dashboard.insights.issuedVsRepaidTitle')}
      </Typography>

      {loading ? (
        <Box sx={{ height: 240, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Typography color="text.secondary">{t('common.loading')}</Typography>
        </Box>
      ) : empty ? (
        <Box
          sx={{ height: 240, display: 'flex', alignItems: 'center', justifyContent: 'center', px: 2 }}
          data-testid="loans-issued-vs-repaid-empty"
        >
          <Typography color="text.secondary" align="center">
            {t('dashboard.insights.issuedVsRepaidEmpty')}
          </Typography>
        </Box>
      ) : (
        <Box sx={{ width: '100%', height: 240 }}>
          <ResponsiveContainer>
            <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="name" tickLine={false} axisLine={false} />
              <YAxis tickLine={false} axisLine={false} width={56} />
              <Tooltip
                formatter={(value) => [
                  formatMoney(String(value ?? 0), { currency }),
                  t('dashboard.insights.amount'),
                ]}
              />
              <Bar dataKey="amount" radius={[4, 4, 0, 0]} maxBarSize={64}>
                {data.map((entry) => (
                  <Cell key={entry.name} fill={entry.fill} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Box>
      )}
    </Paper>
  )
}
