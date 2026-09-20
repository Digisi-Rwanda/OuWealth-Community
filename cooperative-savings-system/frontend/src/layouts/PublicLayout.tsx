import MenuIcon from '@mui/icons-material/Menu'
import {
  AppBar,
  Box,
  Button,
  Container,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Stack,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link as RouterLink, NavLink, Outlet, useLocation } from 'react-router-dom'
import { BrandLogo } from '@/shared/components/BrandLogo'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'
import { ThemeSwitcher } from '@/shared/components/ThemeSwitcher'
import { ROUTES } from '@/shared/constants/routes'
import { publicFooterLinks, publicPrimaryLinks, isPublicNavActive, publicNavTarget } from './publicNav'

const DRAWER_WIDTH = 300

export function PublicLayout() {
  const { t } = useTranslation()
  const theme = useTheme()
  const isMdUp = useMediaQuery(theme.breakpoints.up('md'))
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const dark = theme.palette.mode === 'dark'

  useEffect(() => {
    setMobileOpen(false)
  }, [location.pathname, location.hash])

  const drawer = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }} data-testid="public-mobile-nav">
      <Toolbar sx={{ minHeight: 72, px: 2 }}>
        <BrandLogo variant="lockup" size={36} onDark={dark} />
      </Toolbar>
      <Divider />
      <List sx={{ px: 1, py: 1.5, flex: 1 }}>
        {publicPrimaryLinks.map((item) => (
          <ListItemButton
            key={`${item.labelKey}-${item.path}-${item.hash ?? ''}`}
            component={NavLink}
            to={publicNavTarget(item)}
            selected={isPublicNavActive(item, location.pathname, location.hash)}
            sx={{ borderRadius: 2, mb: 0.5, minHeight: 44 }}
          >
            <ListItemText primary={t(item.labelKey)} />
          </ListItemButton>
        ))}
      </List>
      <Divider />
      <Stack spacing={1.25} sx={{ p: 2 }}>
        <Button component={RouterLink} to={ROUTES.login} variant="outlined" fullWidth>
          {t('public.nav.login')}
        </Button>
        <Button component={RouterLink} to={ROUTES.signup} variant="contained" color="secondary" fullWidth>
          {t('public.nav.createScheme')}
        </Button>
        <Stack direction="row" spacing={1} justifyContent="center">
          <LanguageSwitcher onDark={dark} />
          <ThemeSwitcher onDark={dark} />
        </Stack>
      </Stack>
    </Box>
  )

  return (
    <Box
      sx={{
        minHeight: '100dvh',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: dark ? '#0A0A0A' : '#F7FAFD',
      }}
      data-testid="public-layout"
    >
      <AppBar
        position="sticky"
        color="inherit"
        elevation={0}
        component="header"
        sx={{
          bgcolor: dark ? 'rgba(10,10,10,0.92)' : 'rgba(255,255,255,0.94)',
          color: 'text.primary',
          borderBottom: '1px solid',
          borderColor: 'divider',
          backdropFilter: 'blur(10px)',
        }}
      >
        <Toolbar sx={{ gap: 1, minHeight: { xs: 64, md: 72 } }}>
          {!isMdUp ? (
            <IconButton
              edge="start"
              aria-label={t('common.openMenu')}
              onClick={() => setMobileOpen(true)}
              sx={{ minWidth: 44, minHeight: 44 }}
            >
              <MenuIcon />
            </IconButton>
          ) : null}

          <Box
            component={RouterLink}
            to={ROUTES.home}
            aria-label={t('app.name')}
            sx={{
              display: 'flex',
              alignItems: 'center',
              textDecoration: 'none',
              color: 'inherit',
              mr: { md: 2 },
            }}
          >
            <BrandLogo variant="lockup" size={40} onDark={dark} />
          </Box>

          {isMdUp ? (
            <Stack direction="row" spacing={0.5} component="nav" aria-label={t('public.nav.aria')} sx={{ ml: 1 }}>
              {publicPrimaryLinks.map((item) => (
                <Button
                  key={`${item.labelKey}-${item.path}-${item.hash ?? ''}`}
                  component={NavLink}
                  to={publicNavTarget(item)}
                  color="inherit"
                  sx={{
                    fontWeight: 600,
                    minHeight: 40,
                    color: isPublicNavActive(item, location.pathname, location.hash)
                      ? 'primary.main'
                      : 'text.primary',
                  }}
                >
                  {t(item.labelKey)}
                </Button>
              ))}
            </Stack>
          ) : null}

          <Box sx={{ flex: 1 }} />

          {isMdUp ? (
            <Stack direction="row" spacing={1} alignItems="center">
              <LanguageSwitcher onDark={dark} />
              <ThemeSwitcher onDark={dark} />
              <Button component={RouterLink} to={ROUTES.login} variant="outlined" size="medium">
                {t('public.nav.login')}
              </Button>
              <Button
                component={RouterLink}
                to={ROUTES.signup}
                variant="contained"
                color="secondary"
                size="medium"
              >
                {t('public.nav.createScheme')}
              </Button>
            </Stack>
          ) : (
            <Button
              component={RouterLink}
              to={ROUTES.signup}
              variant="contained"
              color="secondary"
              size="small"
              sx={{ display: { xs: 'inline-flex', sm: 'inline-flex' } }}
            >
              {t('public.nav.createScheme')}
            </Button>
          )}
        </Toolbar>
      </AppBar>

      <Drawer
        open={mobileOpen}
        onClose={() => setMobileOpen(false)}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH },
        }}
      >
        {drawer}
      </Drawer>

      <Box component="main" sx={{ flex: 1 }}>
        <Outlet />
      </Box>

      <Box
        component="footer"
        data-testid="public-footer"
        sx={{
          borderTop: '1px solid',
          borderColor: 'divider',
          bgcolor: dark ? '#0A0A0A' : '#0A0A0A',
          color: '#FFFFFF',
          py: { xs: 4, md: 5 },
          mt: 'auto',
        }}
      >
        <Container maxWidth="lg">
          <Stack spacing={3}>
            <Stack
              direction={{ xs: 'column', md: 'row' }}
              spacing={3}
              justifyContent="space-between"
              alignItems={{ xs: 'flex-start', md: 'center' }}
            >
              <Box>
                <BrandLogo variant="lockup" size={40} onDark />
                <Typography variant="body2" sx={{ mt: 1.5, maxWidth: 420, color: 'rgba(255,255,255,0.72)' }}>
                  {t('public.footer.tagline')}
                </Typography>
              </Box>
              <Stack
                component="nav"
                aria-label={t('public.footer.navAria')}
                direction="row"
                flexWrap="wrap"
                useFlexGap
                spacing={1.5}
                sx={{ maxWidth: { xs: '100%', md: 520 } }}
              >
                {publicFooterLinks.map((item) => (
                  <Button
                    key={`${item.labelKey}-${item.path}-${item.hash ?? ''}`}
                    component={RouterLink}
                    to={publicNavTarget(item)}
                    size="small"
                    sx={{ color: 'rgba(255,255,255,0.88)', minHeight: 40 }}
                  >
                    {t(item.labelKey)}
                  </Button>
                ))}
              </Stack>
            </Stack>
            <Divider sx={{ borderColor: 'rgba(255,255,255,0.12)' }} />
            <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.55)' }}>
              {t('public.footer.copyright', { year: new Date().getFullYear() })}
            </Typography>
          </Stack>
        </Container>
      </Box>
    </Box>
  )
}
