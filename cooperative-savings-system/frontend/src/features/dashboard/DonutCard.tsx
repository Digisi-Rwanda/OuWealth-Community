import { Box, Paper, Skeleton, Stack, Typography } from '@mui/material'
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { ChartTooltip } from './ChartTooltip'

export interface DonutSlice {
  key: string
  label: string
  value: number
  color: string
}

interface DonutCardProps {
  testId: string
  title: string
  slices: DonutSlice[]
  /** Big number in the donut hole, e.g. the total. */
  centerValue: string
  centerLabel: string
  formatValue: (value: number) => string
  emptyMessage: string
  loading?: boolean
}

const DONUT_SIZE = 148

/**
 * Compact donut for genuine parts of one whole. The legend carries the exact values (and shares) as text,
 * so the chart is never the only place a number appears. A zero total shows an empty state, not a blank ring.
 */
export function DonutCard({
  testId,
  title,
  slices,
  centerValue,
  centerLabel,
  formatValue,
  emptyMessage,
  loading,
}: DonutCardProps) {
  const safeSlices = slices.map((slice) => ({
    ...slice,
    value: Number.isFinite(slice.value) && slice.value > 0 ? slice.value : 0,
  }))
  const total = safeSlices.reduce((sum, slice) => sum + slice.value, 0)

  return (
    <Paper
      elevation={0}
      data-testid={testId}
      sx={{
        p: { xs: 2, md: 2.5 },
        height: '100%',
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
      }}
    >
      <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1.5 }}>
        {title}
      </Typography>

      {loading ? (
        <Skeleton variant="rounded" height={DONUT_SIZE} />
      ) : total <= 0 ? (
        <Box
          data-testid={`${testId}-empty`}
          sx={{ minHeight: DONUT_SIZE, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
        >
          <Typography color="text.secondary" align="center">
            {emptyMessage}
          </Typography>
        </Box>
      ) : (
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          sx={{ alignItems: 'center', justifyContent: 'center' }}
        >
          <Box
            aria-hidden="true"
            sx={{ position: 'relative', width: DONUT_SIZE, height: DONUT_SIZE, flexShrink: 0 }}
          >
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={safeSlices}
                  dataKey="value"
                  nameKey="label"
                  innerRadius="64%"
                  outerRadius="94%"
                  startAngle={90}
                  endAngle={-270}
                  paddingAngle={safeSlices.filter((s) => s.value > 0).length > 1 ? 2 : 0}
                  stroke="none"
                  isAnimationActive={false}
                >
                  {safeSlices.map((slice) => (
                    <Cell key={slice.key} fill={slice.color} />
                  ))}
                </Pie>
                <Tooltip
                  content={({ active, payload }) => {
                    const item = active ? payload?.[0]?.payload : undefined
                    if (!item) return null
                    return (
                      <ChartTooltip
                        title={item.label}
                        lines={[
                          { label: formatValue(item.value), value: `${((item.value / total) * 100).toFixed(1)}%` },
                        ]}
                      />
                    )
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
            <Box
              sx={{
                position: 'absolute',
                inset: 0,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                pointerEvents: 'none',
              }}
            >
              <Typography variant="h5" sx={{ fontWeight: 700, lineHeight: 1.1 }}>
                {centerValue}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {centerLabel}
              </Typography>
            </Box>
          </Box>

          <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0, minWidth: 0, flex: { sm: 1 }, width: { xs: '100%', sm: 'auto' } }}>
            {safeSlices.map((slice) => (
              <Box
                key={slice.key}
                component="li"
                sx={{ display: 'flex', alignItems: 'baseline', gap: 1, py: 0.5 }}
              >
                <Box
                  aria-hidden="true"
                  sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: slice.color, flexShrink: 0, alignSelf: 'center' }}
                />
                <Typography variant="body2" sx={{ flex: 1, minWidth: 0 }}>
                  {slice.label}
                </Typography>
                <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                  {formatValue(slice.value)}
                </Typography>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ minWidth: 44, textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}
                >
                  {`${((slice.value / total) * 100).toFixed(1)}%`}
                </Typography>
              </Box>
            ))}
          </Box>
        </Stack>
      )}
    </Paper>
  )
}
