import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { AppLayout } from '@/layouts/AppLayout'
import { LoginPage } from '@/pages/LoginPage'
import { LoginSuccessSplashPage } from '@/pages/LoginSuccessSplashPage'
import { SignupPage } from '@/pages/SignupPage'
import { ForgotPasswordPage } from '@/pages/ForgotPasswordPage'
import { ResetPasswordPage } from '@/pages/ResetPasswordPage'
import { ChangePasswordPage } from '@/pages/ChangePasswordPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { MembersPage } from '@/pages/MembersPage'
import { MemberDetailPage } from '@/pages/MemberDetailPage'
import { ContributionsPage } from '@/pages/ContributionsPage'
import { LoansPage } from '@/pages/LoansPage'
import { LoanDetailRoute } from '@/pages/LoanDetailPage'
import { FinesPage } from '@/pages/FinesPage'
import { FineDetailPage } from '@/pages/FineDetailPage'
import { FinePaymentQueuePage } from '@/pages/FinePaymentQueuePage'
import { SocialFundPage } from '@/pages/SocialFundPage'
import { InvestmentsPage } from '@/pages/InvestmentsPage'
import { InvestmentDetailPage } from '@/pages/InvestmentDetailPage'
import { TransactionsPage } from '@/pages/TransactionsPage'
import { LedgerPage } from '@/pages/LedgerPage'
import { PayoutsPage } from '@/pages/PayoutsPage'
import { PayoutDetailPage } from '@/pages/PayoutDetailPage'
import { ReportsPage } from '@/pages/ReportsPage'
import { HistoricalImportPage } from '@/pages/HistoricalImportPage'
import { SettingsPage } from '@/pages/SettingsPage'
import { BillingPage } from '@/pages/BillingPage'
import { PaymentReturnPage } from '@/pages/PaymentReturnPage'
import { NotificationsPage } from '@/pages/NotificationsPage'
import { AuditLogsPage } from '@/pages/AuditLogsPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { CooperativesPage } from '@/pages/CooperativesPage'
import { CooperativeDetailPage } from '@/pages/CooperativeDetailPage'
import { SystemHealthPage } from '@/pages/SystemHealthPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { ROUTES } from '@/shared/constants/routes'
import {
  FINANCE_ACCESS_ROLES,
  LEADERSHIP_ROLES,
  ROLE_SUPER_ADMIN,
  SECRETARY_ACCESS_ROLES,
} from '@/shared/types/auth'
import { AuthBootstrap } from './AuthBootstrap'
import { ProtectedRoute } from './ProtectedRoute'
import { RoleRoute } from './RoleRoute'

export function AppRouter() {
  return (
    <BrowserRouter>
      <AuthBootstrap>
        <Routes>
          <Route element={<AuthLayout />}>
            <Route path={ROUTES.login} element={<LoginPage />} />
            <Route path={ROUTES.signup} element={<SignupPage />} />
            <Route path={ROUTES.forgotPassword} element={<ForgotPasswordPage />} />
            <Route path={ROUTES.resetPassword} element={<ResetPasswordPage />} />
          </Route>

          <Route element={<ProtectedRoute />}>
            <Route path={ROUTES.loginSuccess} element={<LoginSuccessSplashPage />} />
            <Route element={<AppLayout />}>
              <Route path={ROUTES.dashboard} element={<DashboardPage />} />
              <Route
                element={<RoleRoute roles={SECRETARY_ACCESS_ROLES} />}
              >
                <Route path={ROUTES.members} element={<MembersPage />} />
                <Route path="/members/:userId" element={<MemberDetailPage />} />
              </Route>
              <Route path={ROUTES.contributions} element={<ContributionsPage />} />
              <Route
                path="/contributions/special/:campaignId"
                element={<ContributionsPage />}
              />
              <Route path={ROUTES.loans} element={<LoansPage />} />
              <Route path="/loans/:loanId" element={<LoanDetailRoute />} />
              <Route path={ROUTES.fines} element={<FinesPage />} />
              <Route path="/fines/:fineId" element={<FineDetailPage />} />
              <Route
                element={<RoleRoute roles={FINANCE_ACCESS_ROLES} />}
              >
                <Route path={ROUTES.finePayments} element={<FinePaymentQueuePage />} />
              </Route>
              <Route path={ROUTES.socialFund} element={<SocialFundPage />} />
              <Route
                element={<RoleRoute roles={FINANCE_ACCESS_ROLES} />}
              >
                <Route path={ROUTES.investments} element={<InvestmentsPage />} />
                <Route
                  path="/investments/:investmentId"
                  element={<InvestmentDetailPage />}
                />
                <Route path={ROUTES.transactions} element={<TransactionsPage />} />
                <Route path={ROUTES.ledger} element={<LedgerPage />} />
              </Route>
              <Route path={ROUTES.payouts} element={<PayoutsPage />} />
              <Route path="/payouts/:runId" element={<PayoutDetailPage />} />
              <Route path={ROUTES.reports} element={<ReportsPage />} />
              <Route path={ROUTES.billing} element={<BillingPage />} />
              <Route path={ROUTES.billingPaymentReturn} element={<PaymentReturnPage />} />
              <Route
                element={<RoleRoute roles={[...LEADERSHIP_ROLES]} />}
              >
                <Route path={ROUTES.historicalImport} element={<HistoricalImportPage />} />
              </Route>
              <Route path={ROUTES.notifications} element={<NotificationsPage />} />
              <Route path={ROUTES.profile} element={<ProfilePage />} />
              <Route path={ROUTES.changePassword} element={<ChangePasswordPage />} />
              <Route
                element={<RoleRoute roles={[...LEADERSHIP_ROLES]} />}
              >
                <Route path={ROUTES.settings} element={<SettingsPage />} />
              </Route>
              <Route
                element={<RoleRoute roles={SECRETARY_ACCESS_ROLES} />}
              >
                <Route path={ROUTES.auditLogs} element={<AuditLogsPage />} />
              </Route>
              <Route element={<RoleRoute roles={[ROLE_SUPER_ADMIN]} />}>
                <Route path={ROUTES.cooperatives} element={<CooperativesPage />} />
                <Route path="/cooperatives/:id" element={<CooperativeDetailPage />} />
                <Route path={ROUTES.system} element={<SystemHealthPage />} />
              </Route>
            </Route>
          </Route>

          <Route path="/" element={<Navigate to={ROUTES.login} replace />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </AuthBootstrap>
    </BrowserRouter>
  )
}
