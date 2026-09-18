import { ROUTES } from '@/shared/constants/routes'

export interface PublicNavLink {
  labelKey: string
  path: string
}

/** Primary header links (Home / About / Contact). Auth CTAs are separate. */
export const publicPrimaryLinks: PublicNavLink[] = [
  { labelKey: 'public.nav.home', path: ROUTES.home },
  { labelKey: 'public.nav.about', path: ROUTES.about },
  { labelKey: 'public.nav.contact', path: ROUTES.contact },
]

export const publicFooterLinks: PublicNavLink[] = [
  { labelKey: 'public.nav.about', path: ROUTES.about },
  { labelKey: 'public.nav.privacy', path: ROUTES.privacy },
  { labelKey: 'public.nav.terms', path: ROUTES.terms },
  { labelKey: 'public.nav.contact', path: ROUTES.contact },
  { labelKey: 'public.nav.login', path: ROUTES.login },
  { labelKey: 'public.nav.createScheme', path: ROUTES.signup },
]
