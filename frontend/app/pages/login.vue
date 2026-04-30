<script setup lang="ts">
import { useAuthStore } from '~/stores/useAuthStore'
import { useAuth } from '~/composables/useAuth'
import { useGoogleOneTap } from '~/composables/useGoogleOneTap'

const { t } = useI18n()
const store = useAuthStore()
const { login } = useAuth()
const { prompt } = useGoogleOneTap()
const router = useRouter()
const route = useRoute()

const errorMessage = ref('')

onMounted(async () => {
  const redirectTo = (route.query.redirect as string | undefined) ?? '/profile'

  if (store.isAuthenticated) {
    await router.replace(redirectTo)
    return
  }

  const errorParam = route.query.error as string | undefined
  const sessionExpired = route.query.sessionExpired as string | undefined

  if (!store.isAuthenticated) {
    prompt()
  }

  if (sessionExpired === 'true') {
    errorMessage.value = t('auth.error.session_expired')
  } else if (errorParam) {
    const keyMap: Record<string, string> = {
      auth_failed: 'auth.error.auth_failed',
      google_credential_invalid: 'auth.error.google_credential_invalid',
      token_exchange_failed: 'auth.error.token_exchange_failed',
    }
    errorMessage.value = t(keyMap[errorParam] ?? 'auth.error.generic')
  }
})

watch(
  () => store.authError,
  (err) => {
    if (err) errorMessage.value = err
  },
)

function dismissError() {
  errorMessage.value = ''
  store.clearError()
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-background px-4">
    <div class="w-full max-w-sm space-y-6 rounded-lg border bg-card p-8 shadow-sm">
      <div class="space-y-2 text-center">
        <h1 class="text-2xl font-bold tracking-tight">
          {{ t('auth.login.title') }}
        </h1>
        <p class="text-sm text-muted-foreground">
          {{ t('auth.login.subtitle') }}
        </p>
      </div>

      <AuthErrorBanner v-if="errorMessage" :message="errorMessage" @dismiss="dismissError" />

      <button
        class="w-full rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
        @click="login"
      >
        {{ t('auth.login.button') }}
      </button>
    </div>
  </div>
</template>
