import AccountBalanceIcon from '@mui/icons-material/AccountBalance'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'
import DashboardIcon from '@mui/icons-material/Dashboard'
import HealthAndSafetyIcon from '@mui/icons-material/HealthAndSafety'
import LockResetIcon from '@mui/icons-material/LockReset'
import PersonIcon from '@mui/icons-material/Person'
import { SUPER_ADMIN_ROUTES } from '@/shared/constants/routes'
import type { NavItem, SidebarNavGroup } from './navItems'

/**
 * Navigation of the system-administration portal. Deliberately separate from the Saving Scheme
 * `navItems`: a platform administrator does not get operational modules (members, loans, ledger, ...).
 */
const overview: NavItem = {
  labelKey: 'superAdmin.nav.overview',
  path: SUPER_ADMIN_ROUTES.home,
  icon: DashboardIcon,
  sidebarGroup: 'platform',
  end: true,
}

const platform: NavItem[] = [
  {
    labelKey: 'superAdmin.nav.savingSchemes',
    path: SUPER_ADMIN_ROUTES.cooperatives,
    icon: AccountBalanceIcon,
    sidebarGroup: 'platform',
  },
  {
    labelKey: 'superAdmin.nav.superAdmins',
    path: SUPER_ADMIN_ROUTES.admins,
    icon: AdminPanelSettingsIcon,
    sidebarGroup: 'platform',
  },
  {
    labelKey: 'superAdmin.nav.system',
    path: SUPER_ADMIN_ROUTES.system,
    icon: HealthAndSafetyIcon,
    sidebarGroup: 'platform',
  },
]

const account: NavItem[] = [
  {
    labelKey: 'nav.profile',
    path: SUPER_ADMIN_ROUTES.profile,
    icon: PersonIcon,
    sidebarGroup: 'account',
  },
  {
    labelKey: 'superAdmin.nav.changePassword',
    path: SUPER_ADMIN_ROUTES.changePassword,
    icon: LockResetIcon,
    sidebarGroup: 'account',
  },
]

export const superAdminNavItems: NavItem[] = [overview, ...platform, ...account]

export function getSuperAdminSidebarGroups(): SidebarNavGroup[] {
  return [
    { id: 'platform', labelKey: 'superAdmin.nav.groupAdministration', items: [overview, ...platform] },
    { id: 'account', labelKey: 'nav.group.account', items: account },
  ]
}
