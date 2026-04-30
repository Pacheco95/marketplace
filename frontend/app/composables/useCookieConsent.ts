import { COOKIE_POLICY_VERSION } from '~/config/cookiePolicy'
import { useConsentStore } from '~/stores/useConsentStore'

// Thin composable that wraps the Pinia store for components that need cookie consent state.
// CookieBanner manages its own state directly; this is available for other use cases.
export function useCookieConsent() {
  const store = useConsentStore()

  const hasConsented = computed(() => store.hasConsented)

  const isOutdated = computed(() => {
    if (import.meta.server) return false
    try {
      const raw = document.cookie
        .split('; ')
        .find((row) => row.startsWith('cookie_consent='))
        ?.split('=')
        .slice(1)
        .join('=')
      if (!raw) return false
      const record = JSON.parse(decodeURIComponent(raw))
      return record.version !== COOKIE_POLICY_VERSION
    } catch {
      return false
    }
  })

  function accept() {
    store.accept()
  }

  return { hasConsented, isOutdated, accept }
}
