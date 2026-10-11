import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAppSelector } from '@/app/store/hooks'
import { selectIsSuperAdmin } from '@/app/store/authSlice'
import { superAdminEquivalentOf } from '@/shared/constants/portal'

/**
 * Guards the Saving Scheme client application. A system SUPER_ADMIN is not a Saving Scheme user, so any
 * client route sends them to the system portal (to its equivalent page where one exists).
 */
export function ClientPortalRoute() {
  const isSuperAdmin = useAppSelector(selectIsSuperAdmin)
  const { pathname } = useLocation()
  if (isSuperAdmin) {
    return <Navigate to={superAdminEquivalentOf(pathname)} replace />
  }
  return <Outlet />
}
