import { Box, Typography } from '@mui/material'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { BrandHomeLink } from '@/shared/components/BrandHomeLink'
import { BrandLogo } from '@/shared/components/BrandLogo'

interface AuthSplitShellProps {
  /** Supporting copy under the tagline, already translated. */
  supporting: string
  /** Wider shell for the signup wizard. */
  wide?: boolean
  children: ReactNode
}

/**
 * One centered auth card: a brand panel (logo, tagline, short copy) next to the form. On phones the brand
 * panel shrinks to a compact header strip above the form so the form keeps the screen.
 */
export function AuthSplitShell({ supporting, wide = false, children }: AuthSplitShellProps) {
  const { t } = useTranslation()

  return (
    <Box
      data-testid="auth-split-shell"
      sx={{
        display: 'flex',
        flexDirection: { xs: 'column', md: 'row' },
        width: '100%',
        maxWidth: wide ? 1120 : 920,
        mx: 'auto',
        borderRadius: 3,
        overflow: 'hidden',
        border: '1px solid',
        borderColor: 'divider',
        boxShadow: '0 18px 48px rgba(15, 23, 42, 0.14)',
        bgcolor: 'background.paper',
      }}
    >
      <Box
        component="section"
        data-testid="auth-brand-panel"
        sx={{
          flex: { md: '0 0 38%' },
          color: '#FFFFFF',
          px: { xs: 2.5, md: 4.5 },
          py: { xs: 2, md: 5 },
          display: 'flex',
          flexDirection: 'column',
          justifyContent: { md: 'center' },
          gap: { xs: 1, md: 3 },
          background:
            'radial-gradient(ellipse at 100% 100%, rgba(255,122,0,0.20) 0%, transparent 55%), linear-gradient(160deg, #0A0A0A 0%, #0E2748 58%, #1B4D8C 140%)',
        }}
      >
        <BrandHomeLink>
          <BrandLogo variant="lockup" size={40} onDark />
        </BrandHomeLink>
        <Box>
          <Typography
            component="p"
            sx={{
              m: 0,
              fontWeight: 700,
              lineHeight: 1.25,
              fontSize: { xs: '1.05rem', md: '1.6rem' },
              maxWidth: 360,
            }}
          >
            {t('app.tagline')}
          </Typography>
          <Box
            sx={{ width: 40, height: 3, borderRadius: 2, bgcolor: 'secondary.main', mt: { xs: 1, md: 2 } }}
          />
        </Box>
        <Typography
          component="p"
          variant="body2"
          sx={{ display: { xs: 'none', md: 'block' }, color: 'rgba(255,255,255,0.78)', maxWidth: 340 }}
        >
          {supporting}
        </Typography>
      </Box>

      <Box
        data-testid="auth-form-panel"
        sx={{ flex: 1, minWidth: 0, p: { xs: 2.5, sm: 4, md: 5 }, bgcolor: 'background.paper' }}
      >
        {children}
      </Box>
    </Box>
  )
}
