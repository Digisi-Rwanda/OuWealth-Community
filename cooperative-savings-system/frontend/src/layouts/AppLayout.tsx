import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import MenuIcon from '@mui/icons-material/Menu'
import {
  AppBar,
  Badge,
  Box,
  Button,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { selectIsCooperativeAdmin, selectIsSuperAdmin } from '@/app/store/authSlice'
import { useAppSelector } from '@/app/store/hooks'
import { fetchUnreadCount } from '@/shared/api/notifications'
import { PwaInstallButton } from '@/pwa/PwaInstallButton'
import { AdminNavMenu } from '@/shared/components/AdminNavMenu'
import { BrandLogo } from '@/shared/components/BrandLogo'
import { MemberNavMenu } from '@/shared/components/MemberNavMenu'
import { CooperativeSelector } from '@/shared/components/CooperativeSelector'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'
import { OfflineBanner } from '@/shared/components/OfflineBanner'
import { ThemeSwitcher } from '@/shared/components/ThemeSwitcher'
import { UserMenu } from '@/shared/components/UserMenu'
import { NotificationBell, NOTIFICATION_POLL_MS } from '@/features/notifications'
import { SubscriptionBanner } from '@/features/subscription/SubscriptionBanner'
import { ROUTES } from '@/shared/constants/routes'
import { getMobileNavItems } from './navItems'

const DRAWER_WIDTH = 280

export function AppLayout() {
  const { t } = useTranslation()
  const theme = useTheme()
  const isMdUp = useMediaQuery(theme.breakpoints.up('md'))
  const location = useLocation()
  const userRoles = useAppSelector((s) => s.auth.user?.roles ?? [])
  const isCoopAdmin = useAppSelector(selectIsCooperativeAdmin)
  const isSuperAdmin = useAppSelector(selectIsSuperAdmin)
  const isMember = !isCoopAdmin && !isSuperAdmin
  const accessToken = useAppSelector((s) => s.auth.accessToken)
  const [mobileOpen, setMobileOpen] = useState(false)

  const mobileItems = getMobileNavItems(userRoles)

  const unreadQuery = useQuery({
    queryKey: ['notifications-unread-count'],
    queryFn: fetchUnreadCount,
    enabled: Boolean(accessToken),
    refetchInterval: NOTIFICATION_POLL_MS,
    refetchOnWindowFocus: true,
    retry: 1,
  })
  const unreadCount = unreadQuery.data ?? 0

  useEffect(() => {
    setMobileOpen(false)
  }, [location.pathname])

  const renderNavItem = (item: (typeof mobileItems)[number]) => {
    const Icon = item.icon
    const selected =
      location.pathname === item.path || location.pathname.startsWith(`${item.path}/`)
    const showBadge = item.path === ROUTES.notifications && unreadCount > 0

    return (
      <ListItemButton
        key={`${item.labelKey}-${item.path}`}
        component={NavLink}
        to={item.path}
        selected={selected}
        sx={{ borderRadius: 2, mb: 0.5, minHeight: 44 }}
      >
        <ListItemIcon sx={{ minWidth: 40 }}>
          {showBadge ? (
            <Badge color="error" badgeContent={unreadCount > 99 ? '99+' : unreadCount}>
              <Icon fontSize="small" />
            </Badge>
          ) : (
            <Icon fontSize="small" />
          )}
        </ListItemIcon>
        <ListItemText primary={t(item.labelKey)} />
      </ListItemButton>
    )
  }

  const drawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar
        sx={{
          px: 2,
          minHeight: 72,
          justifyContent: 'flex-start',
          bgcolor: 'transparent',
        }}
      >
        <BrandLogo variant="lockup" size={40} onDark={theme.palette.mode === 'dark'} />
      </Toolbar>
      <Divider />
      <Box sx={{ px: 2, py: 1.5, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
        <LanguageSwitcher />
        <ThemeSwitcher />
        {isCoopAdmin || isMember ? <CooperativeSelector /> : null}
      </Box>
      <Divider />
      <List sx={{ px: 1, py: 1.5, flex: 1, overflowY: 'auto' }}>{mobileItems.map(renderNavItem)}</List>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100dvh' }}>
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          borderBottom: '1px solid',
          borderColor: 'rgba(255,255,255,0.12)',
          bgcolor: '#0A0A0A',
          color: '#FFFFFF',
        }}
      >
        <Toolbar sx={{ gap: { xs: 0.5, md: 1.5 }, minHeight: { xs: 64, sm: 68 } }}>
          {!isMdUp ? (
            <IconButton
              edge="start"
              aria-label={t('common.openMenu')}
              onClick={() => setMobileOpen(true)}
              sx={{ minWidth: 44, minHeight: 44, color: '#FFFFFF' }}
            >
              <MenuIcon />
            </IconButton>
          ) : null}

          {!isMdUp && isMember && location.pathname !== ROUTES.dashboard ? (
            <IconButton
              component={NavLink}
              to={ROUTES.dashboard}
              aria-label={t('common.backToDashboard')}
              sx={{ minWidth: 44, minHeight: 44, color: '#FFFFFF' }}
            >
              <ArrowBackIcon />
            </IconButton>
          ) : null}

          <Box
            component={NavLink}
            to={ROUTES.dashboard}
            aria-label={t('app.name')}
            sx={{
              display: 'flex',
              alignItems: 'center',
              flexShrink: 0,
              minWidth: 'max-content',
              textDecoration: 'none',
              backgroundColor: 'transparent',
              mr: { md: 1 },
            }}
          >
            <BrandLogo variant="lockup" size={40} onDark />
          </Box>

          {isMdUp ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, ml: 2 }}>
              <Button
                component={NavLink}
                to={ROUTES.dashboard}
                color="inherit"
                sx={{
                  fontWeight: 600,
                  minHeight: 40,
                  color: location.pathname === ROUTES.dashboard ? 'primary.light' : '#FFFFFF',
                }}
              >
                {t('nav.dashboard')}
              </Button>
              {isSuperAdmin ? (
                <Button
                  component={NavLink}
                  to={ROUTES.cooperatives}
                  color="inherit"
                  sx={{
                    fontWeight: 600,
                    minHeight: 40,
                    color: location.pathname.startsWith(ROUTES.cooperatives)
                      ? 'primary.light'
                      : '#FFFFFF',
                  }}
                >
                  {t('nav.cooperatives')}
                </Button>
              ) : null}
              {isCoopAdmin ? <AdminNavMenu /> : null}
              {isMember ? <MemberNavMenu /> : null}
            </Box>
          ) : null}

          <Box sx={{ flex: 1, minWidth: 0 }} />

          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: { xs: 0.25, md: 0.5 },
              flexShrink: 0,
              minWidth: 0,
            }}
          >
            {isMdUp ? (
              <>
                <LanguageSwitcher onDark />
                <ThemeSwitcher onDark />
                <PwaInstallButton />
                {isCoopAdmin || isMember ? <CooperativeSelector onDark /> : null}
              </>
            ) : (
              <>
                <ThemeSwitcher onDark />
                <PwaInstallButton />
              </>
            )}

            <NotificationBell />
            <UserMenu />
          </Box>
        </Toolbar>
      </AppBar>
      <OfflineBanner />
      <SubscriptionBanner />

      {!isMdUp ? (
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
          }}
        >
          {drawerContent}
        </Drawer>
      ) : null}

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: '100%',
          minWidth: 0,
          bgcolor: 'background.default',
        }}
      >
        <Toolbar sx={{ minHeight: { xs: 64, sm: 68 } }} />
        <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 1280, mx: 'auto' }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  )
}
