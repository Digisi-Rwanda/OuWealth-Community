import { Box, Divider, Drawer, useMediaQuery, useTheme } from '@mui/material'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Outlet, useLocation } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/store/hooks'
import { selectSidebarPinned, setSidebarPinned } from '@/app/store/uiSlice'
import { AppFooter } from '@/shared/components/AppFooter'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'
import { OfflineBanner } from '@/shared/components/OfflineBanner'
import { SupportDock } from '@/shared/components/SupportDock'
import { ThemeSwitcher } from '@/shared/components/ThemeSwitcher'
import { SUPER_ADMIN_ROUTES } from '@/shared/constants/routes'
import { AppSidebar, APP_SIDEBAR_WIDTH } from './AppSidebar'
import { AppTopBar } from './AppTopBar'
import { getSuperAdminSidebarGroups } from './superAdminNav'

/**
 * Shell of the OuWealth system-administration portal. It shares the visual language of the Saving Scheme
 * `AppLayout` but has its own navigation, no cooperative selector, no notification centre and no
 * subscription banner: a platform administrator is not a customer of any Saving Scheme.
 */
export function SuperAdminLayout() {
  const { t } = useTranslation()
  const theme = useTheme()
  const dispatch = useAppDispatch()
  const isMdUp = useMediaQuery(theme.breakpoints.up('md'))
  const location = useLocation()
  const sidebarPinned = useAppSelector(selectSidebarPinned)
  const [drawerOpen, setDrawerOpen] = useState(false)

  const showPermanentSidebar = isMdUp && sidebarPinned
  const sidebarHidden = !showPermanentSidebar
  const groups = getSuperAdminSidebarGroups()
  const dark = theme.palette.mode === 'dark'
  const closeDrawer = () => setDrawerOpen(false)

  useEffect(() => {
    setDrawerOpen(false)
  }, [location.pathname, location.search])

  useEffect(() => {
    if (showPermanentSidebar) setDrawerOpen(false)
  }, [showPermanentSidebar])

  return (
    <Box sx={{ display: 'flex', minHeight: '100dvh', bgcolor: 'background.default' }}>
      {showPermanentSidebar ? (
        <Box
          component="aside"
          data-testid="superadmin-sidebar-desktop"
          sx={{
            width: APP_SIDEBAR_WIDTH,
            flexShrink: 0,
            position: 'sticky',
            top: 0,
            alignSelf: 'flex-start',
            height: '100dvh',
          }}
        >
          <AppSidebar
            userRoles={[]}
            groups={groups}
            homePath={SUPER_ADMIN_ROUTES.home}
            onDarkBrand={dark}
            pinControl={{ pinned: true, onToggle: () => dispatch(setSidebarPinned(false)) }}
          />
        </Box>
      ) : null}

      {sidebarHidden ? (
        <Drawer
          variant="temporary"
          open={drawerOpen}
          onClose={closeDrawer}
          ModalProps={{ keepMounted: !isMdUp }}
          aria-label={t('nav.sidebarAria')}
          sx={{
            ...(isMdUp ? { zIndex: (muiTheme) => muiTheme.zIndex.drawer + 2 } : {}),
            '& .MuiDrawer-paper': { width: APP_SIDEBAR_WIDTH, boxSizing: 'border-box' },
          }}
        >
          <AppSidebar
            userRoles={[]}
            groups={groups}
            homePath={SUPER_ADMIN_ROUTES.home}
            onDarkBrand={dark}
            onNavigate={closeDrawer}
            pinControl={
              isMdUp
                ? {
                    pinned: false,
                    onToggle: () => {
                      dispatch(setSidebarPinned(true))
                      closeDrawer()
                    },
                  }
                : undefined
            }
          />
          {!isMdUp ? (
            <>
              <Divider />
              <Box sx={{ px: 2, py: 1.5, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                <LanguageSwitcher />
                <ThemeSwitcher />
              </Box>
            </>
          ) : null}
        </Drawer>
      ) : null}

      <Box sx={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
        <AppTopBar
          isMdUp={isMdUp}
          showCooperativeSelector={false}
          sidebarHidden={sidebarHidden}
          onOpenNav={() => setDrawerOpen(true)}
          showBackToDashboard={false}
          homePath={SUPER_ADMIN_ROUTES.home}
          hideNotifications
        />
        <OfflineBanner />

        <Box component="main" sx={{ flexGrow: 1, width: '100%', minWidth: 0, bgcolor: 'background.default' }}>
          <Box
            data-testid="superadmin-main-content"
            sx={{
              p: { xs: 2, sm: 3 },
              maxWidth: isMdUp && !sidebarPinned ? 'none' : 1440,
              mx: 'auto',
              width: '100%',
            }}
          >
            <Outlet />
          </Box>
        </Box>
        <AppFooter variant="compact" />
      </Box>
      <SupportDock />
    </Box>
  )
}
