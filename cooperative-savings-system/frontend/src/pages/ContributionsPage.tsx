import { Box, Button, Stack, Tab, Tabs } from '@mui/material'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router-dom'
import { useAppSelector } from '@/app/store/hooks'
import {
  selectCanRecordContributions,
  selectCanReviewSharePurchases,
  selectIsSuperAdmin,
} from '@/app/store/authSlice'
import {
  ContributionApprovalsPanel,
  HistoryPanel,
  MemberContributionSubmitPanel,
  MonthlyEntryPanel,
  SpecialCampaignsPanel,
} from '@/features/contributions'
import { SharePurchaseApprovalsPanel } from '@/features/shares'
import { contributionTabsForUser } from '@/features/contributions/contributionHelpers'
import { EmptyState } from '@/shared/components/EmptyState'
import { PageHeader } from '@/shared/components/PageHeader'
import { ROUTES } from '@/shared/constants/routes'

type ContributionTab = 'monthly' | 'submit' | 'approvals' | 'history' | 'special'

function initialTabFromQuery(tabParam: string | null, tabs: ContributionTab[]): number {
  const requested = tabParam === 'mine' ? 'history' : tabParam
  if (requested && tabs.includes(requested as ContributionTab)) {
    return tabs.indexOf(requested as ContributionTab)
  }
  return 0
}

export function ContributionsPage() {
  const { t } = useTranslation()
  const { campaignId } = useParams()
  const [searchParams] = useSearchParams()
  const cooperativeId = useAppSelector((s) => s.auth.selectedCooperativeId)
  const canRecord = useAppSelector(selectCanRecordContributions)
  const canReviewShares = useAppSelector(selectCanReviewSharePurchases)
  const isSuperAdmin = useAppSelector(selectIsSuperAdmin)
  const tabs = useMemo(
    () => contributionTabsForUser(canRecord, isSuperAdmin),
    [canRecord, isSuperAdmin],
  )
  const [tab, setTab] = useState(() => initialTabFromQuery(searchParams.get('tab'), tabs))
  const active = tabs[tab] ?? tabs[0]

  if (!cooperativeId) {
    return (
      <Box>
        <PageHeader
          title={t('pages.contributions.title')}
          description={t('pages.contributions.description')}
        />
        <EmptyState
          title={t('contributions.selectCooperativeTitle')}
          description={t('contributions.selectCooperativeDescription')}
        />
      </Box>
    )
  }

  if (campaignId) {
    return (
      <Box>
        <PageHeader
          title={t('pages.contributions.title')}
          description={t('contributions.campaigns.detailDescription')}
          backTo={ROUTES.contributions}
        />
        <SpecialCampaignsPanel
          cooperativeId={cooperativeId}
          canWrite={canRecord}
          campaignId={campaignId}
        />
      </Box>
    )
  }

  return (
    <Box>
      <PageHeader
        title={t('pages.contributions.title')}
        description={t('pages.contributions.description')}
        actions={
          <Stack
            direction="row"
            spacing={1}
            useFlexGap
            sx={{ flexWrap: 'wrap' }}
          >
            {!isSuperAdmin ? (
              <Button
                variant="contained"
                size="small"
                onClick={() => setTab(Math.max(0, tabs.indexOf('submit')))}
              >
                {t('contributions.submit.action')}
              </Button>
            ) : null}
            {canRecord ? (
              <Button
                variant="outlined"
                size="small"
                onClick={() => setTab(Math.max(0, tabs.indexOf('approvals')))}
              >
                {t('contributions.tabs.approvals')}
              </Button>
            ) : null}
          </Stack>
        }
      />

      <Tabs
        value={tab}
        onChange={(_, value: number) => setTab(value)}
        variant="scrollable"
        allowScrollButtonsMobile
        sx={{ mb: 2.5, borderBottom: 1, borderColor: 'divider' }}
      >
        {tabs.map((item) => (
          <Tab
            key={item}
            label={
              item === 'history' && !canRecord
                ? t('contributions.tabs.mine')
                : t(`contributions.tabs.${item}`)
            }
          />
        ))}
      </Tabs>

      {active === 'monthly' ? (
        <MonthlyEntryPanel cooperativeId={cooperativeId} canWrite={canRecord} />
      ) : null}
      {active === 'submit' ? (
        <MemberContributionSubmitPanel cooperativeId={cooperativeId} />
      ) : null}
      {active === 'approvals' ? (
        <Stack spacing={4}>
          <ContributionApprovalsPanel cooperativeId={cooperativeId} />
          {canReviewShares ? (
            <SharePurchaseApprovalsPanel cooperativeId={cooperativeId} />
          ) : null}
        </Stack>
      ) : null}
      {active === 'history' ? (
        <HistoryPanel cooperativeId={cooperativeId} isAdmin={canRecord} />
      ) : null}
      {active === 'special' ? (
        <SpecialCampaignsPanel cooperativeId={cooperativeId} canWrite={canRecord} />
      ) : null}
    </Box>
  )
}
