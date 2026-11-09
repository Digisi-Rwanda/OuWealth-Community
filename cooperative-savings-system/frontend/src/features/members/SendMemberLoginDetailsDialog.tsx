import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Typography } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { sendLoginDetails, type LoginDetailsResult } from '@/shared/api/members'
import { localizedApiError } from '@/shared/utils/localizedApiError'

/** Emails one member their username and a new temporary password. */
export function SendMemberLoginDetailsDialog({
  cooperativeId,
  userId,
  name,
  username,
  email,
  onClose,
}: {
  cooperativeId: string
  userId: string
  name: string
  username: string
  email?: string | null
  onClose: () => void
}) {
  const { t } = useTranslation()
  const [result, setResult] = useState<LoginDetailsResult | null>(null)
  const hasEmail = Boolean(email && email.trim())
  const send = useMutation({
    mutationFn: () => sendLoginDetails(cooperativeId, { memberUserIds: [userId] }),
    onSuccess: setResult,
  })
  return (
    <Dialog open onClose={send.isPending ? undefined : onClose} fullWidth maxWidth="xs">
      <DialogTitle>{t('members.loginDetails.oneTitle', { name })}</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5} sx={{ pt: 1 }}>
          {result ? (
            result.sent > 0 ? (
              <Alert severity="success" data-testid="member-login-details-result">
                {t('members.loginDetails.oneSent', { email })}
              </Alert>
            ) : (
              <Alert severity="warning">{t('members.loginDetails.oneNotSent')}</Alert>
            )
          ) : (
            <>
              <Typography variant="body2">
                {t('members.loginDetails.oneExplain', { username, email: hasEmail ? email : '—' })}
              </Typography>
              {hasEmail ? (
                <Alert severity="warning">{t('members.loginDetails.oneWarning')}</Alert>
              ) : (
                <Alert severity="error">{t('members.loginDetails.oneNoEmail')}</Alert>
              )}
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
          <Button variant="contained" disabled={!hasEmail || send.isPending} onClick={() => send.mutate()}>
            {t('members.loginDetails.oneSend')}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}
