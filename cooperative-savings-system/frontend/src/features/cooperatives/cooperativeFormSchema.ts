import * as yup from 'yup'
import type { CooperativeCreateRequest } from '@/shared/types/cooperative'
import {
  isValidCooperativeEmail,
  isValidRegistrationDate,
  isValidRegistrationNumber,
  isValidRwandanPhone,
  MAX_CONTRIBUTION_DUE_DAY,
  MIN_CONTRIBUTION_DUE_DAY,
  MIN_REGISTRATION_DATE,
  normalizeRegistrationNumber,
  normalizeRwandanPhone,
  RWANDA_CURRENCY,
  todayInKigaliIso,
} from '@/shared/utils/rwandaCooperative'

export type PresidentAssignmentMode = 'new' | 'existing'

export type CooperativeFormValues = {
  name: string
  description: string
  registrationNumber: string
  contactEmail: string
  contactPhone: string
  address: string
  currency: string
  financialYearStartMonth: number
  monthlyContributionAmount: string
  contributionDueDay: number
  registrationDate: string
  startTrial: boolean
  assignPresidentNow: boolean
  presidentMode: PresidentAssignmentMode
  presidentUserId: string
  presidentUsername: string
  presidentEmail: string
  presidentFirstName: string
  presidentLastName: string
  presidentPhone: string
  presidentTemporaryPassword: string
}

export const cooperativeFormDefaults: CooperativeFormValues = {
  name: '',
  description: '',
  registrationNumber: '',
  contactEmail: '',
  contactPhone: '',
  address: '',
  currency: RWANDA_CURRENCY,
  financialYearStartMonth: 1,
  monthlyContributionAmount: '0',
  contributionDueDay: 1,
  registrationDate: '',
  startTrial: true,
  assignPresidentNow: false,
  presidentMode: 'new',
  presidentUserId: '',
  presidentUsername: '',
  presidentEmail: '',
  presidentFirstName: '',
  presidentLastName: '',
  presidentPhone: '',
  presidentTemporaryPassword: '',
}

export const cooperativeFormSchema: yup.ObjectSchema<CooperativeFormValues> = yup.object({
  name: yup.string().trim().required('Name is required').max(255),
  description: yup.string().trim().max(2000).default(''),
  registrationNumber: yup
    .string()
    .trim()
    .required('Registration number is required')
    .test(
      'registration',
      'Use a valid registration number (4–32 characters, letters/digits with / or -)',
      (v) => Boolean(v && isValidRegistrationNumber(v)),
    ),
  contactEmail: yup
    .string()
    .trim()
    .required('Contact email is required')
    .test('email', 'Enter a valid email address', (v) => Boolean(v && isValidCooperativeEmail(v))),
  contactPhone: yup
    .string()
    .trim()
    .required('Contact phone is required')
    .test(
      'phone',
      'Enter a Rwandan mobile number (10 digits starting with 07)',
      (v) => Boolean(v && isValidRwandanPhone(v)),
    ),
  address: yup.string().trim().max(512).default(''),
  currency: yup
    .string()
    .trim()
    .required('Currency is required')
    .oneOf([RWANDA_CURRENCY], 'Currency must be RWF'),
  financialYearStartMonth: yup
    .number()
    .required()
    .min(1, 'Month must be 1–12')
    .max(12, 'Month must be 1–12'),
  monthlyContributionAmount: yup
    .string()
    .trim()
    .required('Monthly contribution is required')
    .matches(/^\d+(\.\d{1,4})?$/, 'Enter a valid amount'),
  contributionDueDay: yup
    .number()
    .required('Contribution due day is required')
    .min(MIN_CONTRIBUTION_DUE_DAY, 'Due day must be 1–28')
    .max(MAX_CONTRIBUTION_DUE_DAY, 'Due day must be 1–28'),
  registrationDate: yup
    .string()
    .trim()
    .required('Registration date is required')
    .test(
      'registrationDate',
      `Date must be between ${MIN_REGISTRATION_DATE} and today`,
      (v) => Boolean(v && isValidRegistrationDate(v)),
    ),
  startTrial: yup.boolean().required(),
  assignPresidentNow: yup.boolean().required(),
  presidentMode: yup.mixed<PresidentAssignmentMode>().oneOf(['new', 'existing']).required(),
  presidentUserId: yup.string().trim().default(''),
  presidentUsername: yup.string().trim().default(''),
  presidentEmail: yup.string().trim().default(''),
  presidentFirstName: yup.string().trim().default(''),
  presidentLastName: yup.string().trim().default(''),
  presidentPhone: yup.string().trim().default(''),
  presidentTemporaryPassword: yup.string().default(''),
}).test('president-required-when-assigning', function (values) {
  if (!values.assignPresidentNow) return true
  if (values.presidentMode === 'existing') {
    if (!values.presidentUserId?.trim()) {
      return this.createError({
        path: 'presidentUserId',
        message: 'Enter the existing user ID',
      })
    }
    return true
  }
  if (!values.presidentUsername?.trim()) {
    return this.createError({ path: 'presidentUsername', message: 'Username is required' })
  }
  if (!values.presidentEmail?.trim() || !isValidCooperativeEmail(values.presidentEmail)) {
    return this.createError({ path: 'presidentEmail', message: 'Enter a valid email address' })
  }
  if (!values.presidentFirstName?.trim()) {
    return this.createError({ path: 'presidentFirstName', message: 'First name is required' })
  }
  if (!values.presidentLastName?.trim()) {
    return this.createError({ path: 'presidentLastName', message: 'Last name is required' })
  }
  return true
})

export function toCooperativePayload(
  values: CooperativeFormValues,
  mode: 'create' | 'edit' = 'create',
): CooperativeCreateRequest {
  const payload: CooperativeCreateRequest = {
    name: values.name.trim(),
    description: values.description.trim() || undefined,
    registrationNumber: normalizeRegistrationNumber(values.registrationNumber),
    contactEmail: values.contactEmail.trim().toLowerCase(),
    contactPhone: normalizeRwandanPhone(values.contactPhone),
    address: values.address.trim() || undefined,
    currency: RWANDA_CURRENCY,
    financialYearStartMonth: Number(values.financialYearStartMonth),
    monthlyContributionAmount: values.monthlyContributionAmount.trim(),
    contributionDueDay: Number(values.contributionDueDay),
    registrationDate: values.registrationDate.trim(),
  }
  if (mode !== 'create') {
    return payload
  }
  payload.subscriptionInitialization = values.startTrial ? 'START_TRIAL' : 'NONE'
  if (values.assignPresidentNow) {
    payload.president =
      values.presidentMode === 'existing'
        ? { userId: values.presidentUserId.trim() }
        : {
            username: values.presidentUsername.trim(),
            email: values.presidentEmail.trim().toLowerCase(),
            firstName: values.presidentFirstName.trim(),
            lastName: values.presidentLastName.trim(),
            phone: values.presidentPhone.trim() || undefined,
            temporaryPassword: values.presidentTemporaryPassword.trim() || undefined,
          }
  }
  return payload
}

export { todayInKigaliIso }
