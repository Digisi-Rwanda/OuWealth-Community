import { Box, Divider, Drawer, useMediaQuery, useTheme } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Outlet, useLocation } from 'react-router-dom'
import { selectIsCooperativeAdmin, selectIsSuperAdmin } from '@/app/store/authSlice'
import { useAppSelector } from '@/app/store/hooks'
import { fetchUnreadCount } from '@/shared/api/notifications'
import { CooperativeSelector } from '@/shared/components/CooperativeSelector'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'
import { OfflineBanner } from '@/shared/components/OfflineBanner'
import { ThemeSwitcher } from '@/shared/components/ThemeSwitcher'
import { NOTIFICATION_POLL_MS } from '@/features/notifications'
import { SubscriptionBanner } from '@/features/subscription/SubscriptionBanner'
import { ROUTES } from '@/shared/constants/routes'
import { AppSidebar, APP_SIDEBAR_WIDTH } from './AppSidebar'
import { AppTopBar } from './AppTopBar'

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
  const showCooperativeSelector = isCoopAdmin || isMember

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
  }, [location.pathname, location.search])

  const dark = theme.palette.mode === 'dark'

  const mobileDrawerExtras = (
    <Box sx={{ px: 2, py: 1.5, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
      <LanguageSwitcher />
      <ThemeSwitcher />
      {showCooperativeSelector ? <CooperativeSelector /> : null}
    </Box>
  )

  return (
    <Box sx={{ display: 'flex', minHeight: '100dvh', bgcolor: 'background.default' }}>
      {/* Desktop permanent sidebar */}
      {isMdUp ? (
        <Box
          component="aside"
          data-testid="app-sidebar-desktop"
          sx={{
            width: APP_SIDEBAR_WIDTH,
            flexShrink: 0,
            position: 'sticky',
            top: 0,
            alignSelf: 'flex-start',
            height: '100dvh',
          }}
        >
          <AppSidebar userRoles={userRoles} unreadCount={unreadCount} onDarkBrand={dark} />
        </Box>
      ) : null}

      {/* Mobile temporary drawer */}
      {!isMdUp ? (
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          aria-label={t('nav.sidebarAria')}
          sx={{
            '& .MuiDrawer-paper': {
              width: APP_SIDEBAR_WIDTH,
              boxSizing: 'border-box',
            },
          }}
        >
          <AppSidebar
            userRoles={userRoles}
            unreadCount={unreadCount}
            onDarkBrand={dark}
            onNavigate={() => setMobileOpen(false)}
          />
          <Divider />
          {mobileDrawerExtras}
        </Drawer>
      ) : null}

      <Box
        sx={{
          flex: 1,
          minWidth: 0,
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <AppTopBar
          isMdUp={isMdUp}
          showCooperativeSelector={showCooperativeSelector}
          onOpenMobileNav={() => setMobileOpen(true)}
          showBackToDashboard={isMember && location.pathname !== ROUTES.dashboard}
        />
        <OfflineBanner />
        <SubscriptionBanner />

        <Box
          component="main"
          sx={{
            flexGrow: 1,
            width: '100%',
            minWidth: 0,
            bgcolor: 'background.default',
          }}
        >
          <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 1440, mx: 'auto', width: '100%' }}>
            <Outlet />
          </Box>
        </Box>
      </Box>
    </Box>
  )
}
