import type { Theme } from '@mui/material'
import { alpha } from '@mui/material/styles'

/**
 * Typography, spacing and card treatment for the public landing page only. The app theme is intentionally
 * untouched so authenticated pages keep their existing look.
 */

/** Vertical rhythm for landing sections (slightly tighter than before). */
export const LANDING_SECTION_PY = { xs: 4.5, md: 6.5 } as const
export const LANDING_SECTION_PY_COMPACT = { xs: 4, md: 5 } as const

/** Visual size of section titles. Pair with `component="h2"` to keep semantic headings. */
export const landingSectionTitleSx = {
  fontSize: { xs: '1.6rem', md: '2rem' },
  fontWeight: 600,
  lineHeight: 1.25,
  letterSpacing: '-0.01em',
  mb: 1,
} as const

export const landingSectionSubtitleSx = {
  fontSize: { xs: '0.95rem', md: '1rem' },
  fontWeight: 400,
  lineHeight: 1.6,
  maxWidth: 640,
  mb: 3.5,
} as const

/** Card titles: modest, semibold. */
export const landingCardTitleSx = {
  fontSize: '1.02rem',
  fontWeight: 600,
  lineHeight: 1.35,
} as const

/** Normal explanatory text inside cards. */
export const landingCardBodySx = {
  fontSize: '0.9rem',
  fontWeight: 400,
  lineHeight: 1.55,
} as const

/** Small uppercase labels (STEP 1, plan badges): light-to-medium weight. */
export const landingLabelSx = {
  fontSize: '0.7rem',
  fontWeight: 500,
  letterSpacing: '0.08em',
} as const

export type LandingAccent = 'primary' | 'secondary'

/**
 * Soft, slightly raised card with a gentle hover lift. The lift only applies on devices that really hover and is
 * removed under prefers-reduced-motion (the border/shadow change stays, as it does not move anything).
 */
export function landingCardSx(accent: LandingAccent = 'primary', highlighted = false) {
  return (theme: Theme) => {
    const dark = theme.palette.mode === 'dark'
    const main = theme.palette[accent].main
    return {
      height: '100%',
      borderRadius: 2.5,
      border: '1px solid',
      borderColor: highlighted ? alpha(main, 0.7) : 'divider',
      bgcolor: 'background.paper',
      boxShadow: highlighted
        ? dark
          ? '0 6px 18px rgba(0,0,0,0.3)'
          : `0 6px 18px ${alpha(main, 0.12)}`
        : dark
          ? '0 1px 2px rgba(0,0,0,0.3)'
          : '0 1px 3px rgba(15,23,42,0.06)',
      transition: 'transform 200ms ease, box-shadow 200ms ease, border-color 200ms ease',
      '@media (hover: hover) and (pointer: fine)': {
        '&:hover': {
          transform: 'translateY(-3px)',
          borderColor: alpha(main, 0.45),
          boxShadow: dark
            ? '0 10px 24px rgba(0,0,0,0.42)'
            : `0 10px 24px ${alpha(highlighted ? main : theme.palette.primary.main, highlighted ? 0.18 : 0.12)}`,
          '& .landing-card-icon': {
            bgcolor: alpha(main, dark ? 0.3 : 0.16),
          },
        },
      },
      '@media (prefers-reduced-motion: reduce)': {
        transition: 'box-shadow 200ms ease, border-color 200ms ease',
        '&:hover': { transform: 'none' },
      },
    }
  }
}
