<script setup lang="ts">
import { useAuthStore } from '~/stores/useAuthStore'
import { useAuth } from '~/composables/useAuth'
import { useGoogleOneTap } from '~/composables/useGoogleOneTap'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '~/components/ui/card'
import { Button } from '~/components/ui/button'

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
    <Card class="w-full max-w-sm">
      <CardHeader class="space-y-1 text-center">
        <CardTitle class="text-2xl">
          {{ t('auth.login.title') }}
        </CardTitle>
        <CardDescription>
          {{ t('auth.login.subtitle') }}
        </CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <AuthErrorBanner v-if="errorMessage" :message="errorMessage" @dismiss="dismissError" />
        <Button class="w-full" @click="login">
          {{ t('auth.login.button') }}
        </Button>
      </CardContent>
    </Card>
  </div>
</template>
