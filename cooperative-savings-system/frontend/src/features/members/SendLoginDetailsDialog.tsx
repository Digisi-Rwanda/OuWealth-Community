import { Alert, Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, Stack, Typography } from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchLoginDetailsPreview, sendLoginDetails, type LoginDetailsResult } from '@/shared/api/members'
import { localizedApiError } from '@/shared/utils/localizedApiError'

/** Emails members their username and a temporary password (by default only those who have never signed in). */
export function SendLoginDetailsDialog({ cooperativeId, onClose }: { cooperativeId: string; onClose: () => void }) {
  const { t } = useTranslation()
  const [includeSignedIn, setIncludeSignedIn] = useState(false)
  const [result, setResult] = useState<LoginDetailsResult | null>(null)
  const preview = useQuery({
    queryKey: ['members', cooperativeId, 'login-details'],
    queryFn: () => fetchLoginDetailsPreview(cooperativeId),
  })
  const send = useMutation({
    mutationFn: () => sendLoginDetails(cooperativeId, { includeSignedIn }),
    onSuccess: setResult,
  })
  const p = preview.data
  const count = p ? p.neverSignedIn + (includeSignedIn ? p.signedIn : 0) : 0
  return (
    <Dialog open onClose={send.isPending ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>{t('members.loginDetails.title')}</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5} sx={{ pt: 1 }}>
          {result ? (
            <Alert severity="success" data-testid="login-details-result">
              {t('members.loginDetails.sent', { count: result.sent })}
              {result.skippedNoEmail > 0 ? ` ${t('members.loginDetails.noEmail', { count: result.skippedNoEmail })}` : ''}
            </Alert>
          ) : (
            <>
              <Typography variant="body2">{t('members.loginDetails.explain')}</Typography>
              {p && !p.emailReady ? <Alert severity="error">{t('members.loginDetails.notReady')}</Alert> : null}
              {p ? (
                <Typography variant="body2">
                  {t('members.loginDetails.counts', { never: p.neverSignedIn, signed: p.signedIn, noEmail: p.withoutEmail })}
                </Typography>
              ) : null}
              <FormControlLabel
                control={<Checkbox checked={includeSignedIn} onChange={(_, v) => setIncludeSignedIn(v)} />}
                label={t('members.loginDetails.includeSignedIn')}
              />
              {includeSignedIn ? <Alert severity="warning">{t('members.loginDetails.includeWarning')}</Alert> : null}
              {send.isError ? <Alert severity="error">{localizedApiError(t, send.error, 'members.loginDetails.errors')}</Alert> : null}
            </>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} disabled={send.isPending}>
          {result ? t('common.close') : t('common.cancel')}
        </Button>
        {result ? null : (
          <Button variant="contained" disabled={!p || !p.emailReady || count === 0 || send.isPending} onClick={() => send.mutate()}>
            {t('members.loginDetails.send', { count })}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}
