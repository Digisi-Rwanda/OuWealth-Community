import { yupResolver } from '@hookform/resolvers/yup'
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material'
import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import * as yup from 'yup'
import type { AssignPresidentRequest } from '@/shared/types/cooperative'
import { isValidCooperativeEmail } from '@/shared/utils/rwandaCooperative'

type PresidentMode = 'new' | 'existing'

type AssignPresidentFormValues = {
  presidentMode: PresidentMode
  userId: string
  username: string
  email: string
  firstName: string
  lastName: string
  phone: string
  temporaryPassword: string
}

const defaults: AssignPresidentFormValues = {
  presidentMode: 'new',
  userId: '',
  username: '',
  email: '',
  firstName: '',
  lastName: '',
  phone: '',
  temporaryPassword: '',
}

const schema: yup.ObjectSchema<AssignPresidentFormValues> = yup
  .object({
    presidentMode: yup.mixed<PresidentMode>().oneOf(['new', 'existing']).required(),
    userId: yup.string().trim().default(''),
    username: yup.string().trim().default(''),
    email: yup.string().trim().default(''),
    firstName: yup.string().trim().default(''),
    lastName: yup.string().trim().default(''),
    phone: yup.string().trim().default(''),
    temporaryPassword: yup.string().default(''),
  })
  .test('president-fields', function (values) {
    if (values.presidentMode === 'existing') {
      if (!values.userId?.trim()) {
        return this.createError({ path: 'userId', message: 'Enter the existing user ID' })
      }
      return true
    }
    if (!values.username?.trim()) {
      return this.createError({ path: 'username', message: 'Username is required' })
    }
    if (!values.email?.trim() || !isValidCooperativeEmail(values.email)) {
      return this.createError({ path: 'email', message: 'Enter a valid email address' })
    }
    if (!values.firstName?.trim()) {
      return this.createError({ path: 'firstName', message: 'First name is required' })
    }
    if (!values.lastName?.trim()) {
      return this.createError({ path: 'lastName', message: 'Last name is required' })
    }
    return true
  })

interface AssignPresidentDialogProps {
  open: boolean
  loading?: boolean
  onClose: () => void
  onSubmit: (payload: AssignPresidentRequest) => void
}

export function AssignPresidentDialog({
  open,
  loading = false,
  onClose,
  onSubmit,
}: AssignPresidentDialogProps) {
  const { t } = useTranslation()
  const {
    register,
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<AssignPresidentFormValues>({
    resolver: yupResolver(schema),
    defaultValues: defaults,
  })
  const presidentMode = watch('presidentMode')

  useEffect(() => {
    if (open) reset(defaults)
  }, [open, reset])

  return (
    <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>{t('cooperatives.onboarding.assignPresident')}</DialogTitle>
      <form
        onSubmit={handleSubmit((values) => {
          const payload: AssignPresidentRequest =
            values.presidentMode === 'existing'
              ? { userId: values.userId.trim() }
              : {
                  username: values.username.trim(),
                  email: values.email.trim().toLowerCase(),
                  firstName: values.firstName.trim(),
                  lastName: values.lastName.trim(),
                  phone: values.phone.trim() || undefined,
                  temporaryPassword: values.temporaryPassword.trim() || undefined,
                }
          onSubmit(payload)
        })}
        noValidate
      >
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 0.5 }}>
            <Controller
              name="presidentMode"
              control={control}
              render={({ field }) => (
                <TextField {...field} select label={t('cooperatives.onboarding.presidentMode')} fullWidth>
                  <MenuItem value="new">{t('cooperatives.onboarding.createPresident')}</MenuItem>
                  <MenuItem value="existing">{t('cooperatives.onboarding.existingPresident')}</MenuItem>
                </TextField>
              )}
            />
            {presidentMode === 'existing' ? (
              <TextField
                label={t('cooperatives.onboarding.presidentUserId')}
                fullWidth
                error={Boolean(errors.userId)}
                helperText={errors.userId?.message ?? t('cooperatives.onboarding.presidentUserIdHint')}
                {...register('userId')}
              />
            ) : (
              <>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <TextField
                    label={t('cooperatives.onboarding.presidentFirstName')}
                    required
                    fullWidth
                    error={Boolean(errors.firstName)}
                    helperText={errors.firstName?.message}
                    {...register('firstName')}
                  />
                  <TextField
                    label={t('cooperatives.onboarding.presidentLastName')}
                    required
                    fullWidth
                    error={Boolean(errors.lastName)}
                    helperText={errors.lastName?.message}
                    {...register('lastName')}
                  />
                </Stack>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <TextField
                    label={t('cooperatives.onboarding.presidentUsername')}
                    required
                    fullWidth
                    error={Boolean(errors.username)}
                    helperText={errors.username?.message}
                    {...register('username')}
                  />
                  <TextField
                    label={t('cooperatives.onboarding.presidentEmail')}
                    required
                    fullWidth
                    type="email"
                    error={Boolean(errors.email)}
                    helperText={errors.email?.message}
                    {...register('email')}
                  />
                </Stack>
                <TextField
                  label={t('cooperatives.onboarding.presidentPhone')}
                  fullWidth
                  {...register('phone')}
                />
                <TextField
                  label={t('cooperatives.onboarding.presidentTemporaryPassword')}
                  fullWidth
                  type="password"
                  helperText={t('cooperatives.onboarding.presidentTemporaryPasswordHint')}
                  {...register('temporaryPassword')}
                />
              </>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} disabled={loading}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" variant="contained" disabled={loading}>
            {t('cooperatives.onboarding.assignPresident')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
