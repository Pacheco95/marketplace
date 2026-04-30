import { useAuthStore } from '~/stores/useAuthStore'

let initialized = false

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: Record<string, unknown>) => void
          prompt: (
            callback?: (notification: {
              isNotDisplayed: () => boolean
              isSkippedMoment: () => boolean
            }) => void,
          ) => void
        }
      }
    }
  }
}

export function useGoogleOneTap() {
  const store = useAuthStore()
  const config = useRuntimeConfig()
  const router = useRouter()
  const { t } = useI18n()

  async function handleCredentialResponse(response: { credential: string }) {
    try {
      const user = await $fetch('/api/v1/auth/one-tap', {
        method: 'POST',
        credentials: 'include',
        body: { credential: response.credential },
      })
      store.setUser(user as Parameters<typeof store.setUser>[0])
      await router.push('/profile')
    } catch {
      store.setError(t('auth.error.google_credential_invalid'))
    }
  }

  function prompt() {
    if (!window.google) return

    if (!initialized) {
      window.google.accounts.id.initialize({
        client_id: config.public.googleClientId,
        callback: handleCredentialResponse,
        auto_select: true,
        cancel_on_tap_outside: false,
      })
      initialized = true
    }

    window.google.accounts.id.prompt()
  }

  return { prompt }
}
