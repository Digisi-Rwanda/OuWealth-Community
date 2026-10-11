export const ROUTES = {
  home: '/',
  about: '/about',
  contact: '/contact',
  terms: '/terms',
  login: '/login',
  loginSuccess: '/login-success',
  signup: '/signup',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
  changePassword: '/change-password',
  dashboard: '/dashboard',
  members: '/members',
  memberDetail: (userId: string) => `/members/${userId}`,
  contributions: '/contributions',
  specialCampaign: (campaignId: string) => `/contributions/special/${campaignId}`,
  loans: '/loans',
  loanDetail: (loanId: string) => `/loans/${loanId}`,
  fines: '/fines',
  fineDetail: (fineId: string) => `/fines/${fineId}`,
  finePayments: '/fine-payments',
  socialFund: '/social-fund',
  investments: '/investments',
  investmentDetail: (investmentId: string) => `/investments/${investmentId}`,
  transactions: '/transactions',
  ledger: '/ledger',
  payouts: '/payouts',
  payoutDetail: (runId: string) => `/payouts/${runId}`,
  reports: '/reports',
  historicalImport: '/historical-import',
  billing: '/billing',
  billingPaymentReturn: '/billing/payment-return',
  settings: '/settings',
  notifications: '/notifications',
  auditLogs: '/audit-logs',
  profile: '/profile',
  cooperatives: '/cooperatives',
  cooperativeDetail: (id: string) => `/cooperatives/${id}`,
  system: '/system',
} as const

export type AppRoute = Exclude<(typeof ROUTES)[keyof typeof ROUTES], (...args: never[]) => string>

/** The OuWealth system-administration portal. Only the global SUPER_ADMIN role may enter. */
export const SUPER_ADMIN_ROUTES = {
  home: '/superadmin',
  admins: '/superadmin/admins',
  cooperatives: '/superadmin/cooperatives',
  cooperativeDetail: (id: string) => `/superadmin/cooperatives/${id}`,
  system: '/superadmin/system',
  profile: '/superadmin/profile',
  changePassword: '/superadmin/change-password',
} as const

export const SUPER_ADMIN_BASE = SUPER_ADMIN_ROUTES.home

export function isSuperAdminPath(pathname: string): boolean {
  return pathname === SUPER_ADMIN_BASE || pathname.startsWith(`${SUPER_ADMIN_BASE}/`)
}
