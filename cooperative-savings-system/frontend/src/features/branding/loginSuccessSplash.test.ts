import { describe, expect, it } from 'vitest'
import { ROUTES } from '@/shared/constants/routes'
import {
  isContinueKey,
  isLoginSuccessLocationState,
  OU_WEALTH_SPLASH_ENTRANCE_MS,
  OU_WEALTH_SPLASH_REDUCED_MOTION_READY_MS,
  resolvePostLoginDestination,
} from './loginSuccessSplash'

function key(keyName: string, extra: Partial<KeyboardEvent> = {}): KeyboardEvent {
  return { key: keyName, repeat: false, ...extra } as KeyboardEvent
}

describe('loginSuccessSplash helpers', () => {
  it('caps the normal-motion ready gate at 4000ms and keeps a short reduced-motion gate', () => {
    expect(OU_WEALTH_SPLASH_ENTRANCE_MS).toBeLessThanOrEqual(4000)
    expect(OU_WEALTH_SPLASH_ENTRANCE_MS).toBeGreaterThanOrEqual(3800)
    expect(OU_WEALTH_SPLASH_REDUCED_MOTION_READY_MS).toBe(50)
    expect(OU_WEALTH_SPLASH_REDUCED_MOTION_READY_MS).toBeLessThan(OU_WEALTH_SPLASH_ENTRANCE_MS)
  })

  it('resolves the current post-login destination to Dashboard', () => {
    expect(resolvePostLoginDestination(undefined)).toBe(ROUTES.dashboard)
    expect(resolvePostLoginDestination(ROUTES.dashboard)).toBe(ROUTES.dashboard)
  })

  it('rejects unsafe or splash-loop next paths', () => {
    expect(resolvePostLoginDestination('https://evil.example')).toBe(ROUTES.dashboard)
    expect(resolvePostLoginDestination('//evil.example')).toBe(ROUTES.dashboard)
    expect(resolvePostLoginDestination(ROUTES.loginSuccess)).toBe(ROUTES.dashboard)
  })

  it('recognizes a successful-login location state', () => {
    expect(isLoginSuccessLocationState({ fromLogin: true, next: '/dashboard' })).toBe(true)
    expect(isLoginSuccessLocationState({ fromLogin: false })).toBe(false)
    expect(isLoginSuccessLocationState(null)).toBe(false)
  })

  it('accepts normal continue keys and rejects modifier-only / repeat', () => {
    expect(isContinueKey(key('Enter'))).toBe(true)
    expect(isContinueKey(key(' '))).toBe(true)
    expect(isContinueKey(key('Escape'))).toBe(true)
    expect(isContinueKey(key('a'))).toBe(true)
    expect(isContinueKey(key('ArrowDown'))).toBe(true)
    expect(isContinueKey(key('Shift'))).toBe(false)
    expect(isContinueKey(key('Control'))).toBe(false)
    expect(isContinueKey(key('Alt'))).toBe(false)
    expect(isContinueKey(key('Meta'))).toBe(false)
    expect(isContinueKey(key('Enter', { repeat: true }))).toBe(false)
  })
})
