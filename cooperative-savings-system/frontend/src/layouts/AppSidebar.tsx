import PushPinIcon from '@mui/icons-material/PushPin'
import PushPinOutlinedIcon from '@mui/icons-material/PushPinOutlined'
import {
  Badge,
  Box,
  Divider,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Toolbar,
  Tooltip,
} from '@mui/material'
import { useTranslation } from 'react-i18next'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { BrandLogo } from '@/shared/components/BrandLogo'
import { ROUTES } from '@/shared/constants/routes'
import {
  getSidebarNavGroups,
  isNavItemActive,
  type NavItem,
} from './navItems'

export const APP_SIDEBAR_WIDTH = 260

/** Desktop-only pin control rendered in the sidebar header. Never passed on mobile. */
export interface SidebarPinControl {
  pinned: boolean
  onToggle: () => void
}

interface AppSidebarProps {
  userRoles: string[]
  unreadCount?: number
  /** When true, use on-dark brand treatment (permanent dark sidebar chrome). */
  onDarkBrand?: boolean
  /** Invoked when a nav link is activated (e.g. close mobile drawer). */
  onNavigate?: () => void
  /** Desktop pin/unpin button. Omitted on mobile, where the drawer is always temporary. */
  pinControl?: SidebarPinControl
}

export function AppSidebar({
  userRoles,
  unreadCount = 0,
  onDarkBrand = false,
  onNavigate,
  pinControl,
}: AppSidebarProps) {
  const { t } = useTranslation()
  const location = useLocation()
  const groups = getSidebarNavGroups(userRoles)

  const renderItem = (item: NavItem) => {
    const Icon = item.icon
    const selected = isNavItemActive(location.pathname, location.search, item.path)
    const showBadge = item.path === ROUTES.notifications && unreadCount > 0

    return (
      <ListItemButton
        key={`${item.labelKey}-${item.path}`}
        // Plain Link (not NavLink): NavLink marks every link to the same pathname as current, ignoring ?tab=,
        // which would announce Contributions and both approval views as current at once.
        component={Link}
        to={item.path}
        selected={selected}
        aria-current={selected ? 'page' : undefined}
        onClick={() => onNavigate?.()}
        data-testid={`nav-item-${item.path.split('?')[0].replace(/^\//, '') || 'root'}${
          item.path.includes('?') ? `-${item.path.split('?')[1]}` : ''
        }`}
        sx={{
          borderRadius: 2,
          mb: 0.25,
          minHeight: 44,
          mx: 1,
          '&.Mui-selected': {
            bgcolor: 'action.selected',
            fontWeight: 700,
            borderLeft: 3,
            borderColor: 'primary.main',
            pl: 1.25,
          },
          '&.Mui-selected .MuiListItemIcon-root': {
            color: 'primary.main',
          },
          '&.Mui-selected .MuiListItemText-primary': {
            fontWeight: 700,
          },
        }}
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

  return (
    <Box
      component="nav"
      aria-label={t('nav.sidebarAria')}
      data-testid="app-sidebar"
      sx={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        bgcolor: 'background.paper',
        borderRight: '1px solid',
        borderColor: 'divider',
      }}
    >
      <Toolbar
        sx={{
          px: 2,
          minHeight: 68,
          justifyContent: 'space-between',
          gap: 1,
        }}
      >
        <Box
          component={NavLink}
          to={ROUTES.dashboard}
          aria-label={t('app.name')}
          sx={{
            display: 'inline-flex',
            textDecoration: 'none',
            color: 'inherit',
            minWidth: 'max-content',
            flexShrink: 0,
          }}
        >
          <BrandLogo variant="lockup" size={40} onDark={onDarkBrand} />
        </Box>
        {pinControl ? (
          <Tooltip title={t(pinControl.pinned ? 'nav.unpinSidebar' : 'nav.pinSidebar')}>
            <IconButton
              size="small"
              onClick={pinControl.onToggle}
              aria-label={t(pinControl.pinned ? 'nav.unpinSidebar' : 'nav.pinSidebar')}
              data-testid="sidebar-pin-toggle"
              sx={{ flexShrink: 0, color: pinControl.pinned ? 'primary.main' : 'text.secondary' }}
            >
              {pinControl.pinned ? <PushPinIcon fontSize="small" /> : <PushPinOutlinedIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
        ) : null}
      </Toolbar>
      <Divider />
      <Box sx={{ flex: 1, overflowY: 'auto', py: 1 }}>
        {groups.map((group) => (
          <List
            key={group.id}
            dense
            subheader={
              group.id === 'overview' ? undefined : (
                <ListSubheader
                  component="div"
                  disableSticky
                  sx={{
                    bgcolor: 'transparent',
                    lineHeight: 2,
                    typography: 'overline',
                    color: 'text.secondary',
                    px: 2.5,
                  }}
                >
                  {t(group.labelKey)}
                </ListSubheader>
              )
            }
            sx={{ mb: 0.5 }}
          >
            {group.items.map(renderItem)}
          </List>
        ))}
      </Box>
    </Box>
  )
}
