import { Box, Container, useTheme } from '@mui/material'
import { Outlet, useLocation } from 'react-router-dom'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'
import { ThemeSwitcher } from '@/shared/components/ThemeSwitcher'
import { ROUTES } from '@/shared/constants/routes'
import { AuthSurfaceContext } from './authSurface'

export function AuthLayout() {
  const theme = useTheme()
  const dark = theme.palette.mode === 'dark'
  const location = useLocation()
  const wide = location.pathname === ROUTES.signup

  return (
    <AuthSurfaceContext.Provider value={{ onDark: dark }}>
      <Box
        sx={{
          minHeight: '100dvh',
          display: 'flex',
          alignItems: 'center',
          position: 'relative',
          background: dark
            ? 'radial-gradient(ellipse at 15% 0%, rgba(27,77,140,0.32) 0%, transparent 52%), radial-gradient(ellipse at 95% 85%, rgba(255,122,0,0.1) 0%, transparent 42%), linear-gradient(160deg, #0A0A0A 0%, #121212 100%)'
            : 'radial-gradient(ellipse at 15% 0%, rgba(27,77,140,0.16) 0%, transparent 52%), radial-gradient(ellipse at 95% 85%, rgba(255,122,0,0.08) 0%, transparent 42%), linear-gradient(160deg, #FFFFFF 0%, #F4F8FD 60%, #E8F1FB 100%)',
        }}
      >
        <Box
          sx={{
            position: 'absolute',
            top: 16,
            right: 16,
            zIndex: 1,
            display: 'flex',
            alignItems: 'center',
            gap: 1,
          }}
        >
          <LanguageSwitcher onDark={dark} />
          <ThemeSwitcher onDark={dark} />
        </Box>
        <Container maxWidth={wide ? 'md' : 'sm'} sx={{ py: { xs: 8, md: 6 } }}>
          <Outlet />
        </Container>
      </Box>
    </AuthSurfaceContext.Provider>
  )
}
