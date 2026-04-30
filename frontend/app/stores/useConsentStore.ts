import { defineStore } from 'pinia'
import { COOKIE_POLICY_VERSION } from '~/config/cookiePolicy'

const COOKIE_KEY = 'cookie_consent'

interface ConsentRecord {
  accepted: boolean
  timestamp: string
  version: string
}

function parseCookie(): ConsentRecord | null {
  if (import.meta.server) return null
  try {
    const raw = document.cookie
      .split('; ')
      .find((row) => row.startsWith(`${COOKIE_KEY}=`))
      ?.split('=')
      .slice(1)
      .join('=')
    return raw ? JSON.parse(decodeURIComponent(raw)) : null
  } catch {
    return null
  }
}

// Used only for unit tests — runtime uses the plugin-provided singleton
export const useConsentStore = defineStore('consent', () => {
  const hasConsented = ref(false)

  function initialize() {
    if (import.meta.server) return
    const record = parseCookie()
    hasConsented.value = !!(record?.accepted && record.version === COOKIE_POLICY_VERSION)
  }

  function accept() {
    if (import.meta.server) return
    const record: ConsentRecord = {
      accepted: true,
      timestamp: new Date().toISOString(),
      version: COOKIE_POLICY_VERSION,
    }
    document.cookie = `${COOKIE_KEY}=${encodeURIComponent(JSON.stringify(record))}; max-age=${60 * 60 * 24 * 365}; path=/; SameSite=Lax`
    hasConsented.value = true
  }

  return { hasConsented, initialize, accept }
})
