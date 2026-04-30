import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { COOKIE_POLICY_VERSION } from '~/config/cookiePolicy'

const COOKIE_KEY = 'cookie_consent'

function setCookieRecord(version: string) {
  const record = JSON.stringify({
    accepted: true,
    timestamp: new Date().toISOString(),
    version,
  })
  document.cookie = `${COOKIE_KEY}=${encodeURIComponent(record)}; path=/`
}

function clearConsentCookie() {
  document.cookie = `${COOKIE_KEY}=; max-age=0; path=/`
}

describe('useConsentStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    clearConsentCookie()
  })

  it('shows banner when no record exists', async () => {
    const { useConsentStore } = await import('~/stores/useConsentStore')
    const store = useConsentStore()
    store.initialize()
    expect(store.hasConsented).toBe(false)
  })

  it('hides banner when valid record with matching version exists', async () => {
    setCookieRecord(COOKIE_POLICY_VERSION)
    const { useConsentStore } = await import('~/stores/useConsentStore')
    const store = useConsentStore()
    store.initialize()
    expect(store.hasConsented).toBe(true)
  })

  it('shows banner when stored version is outdated', async () => {
    setCookieRecord('0.9.0')
    const { useConsentStore } = await import('~/stores/useConsentStore')
    const store = useConsentStore()
    store.initialize()
    expect(store.hasConsented).toBe(false)
  })

  it('accept() sets hasConsented to true immediately', async () => {
    const { useConsentStore } = await import('~/stores/useConsentStore')
    const store = useConsentStore()
    store.initialize()
    expect(store.hasConsented).toBe(false)
    store.accept()
    expect(store.hasConsented).toBe(true)
  })
})
