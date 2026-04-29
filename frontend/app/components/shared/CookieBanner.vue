<script setup lang="ts">
import { COOKIE_POLICY_VERSION } from '~/config/cookiePolicy'
import { Button } from '~/components/ui/button'

const { t } = useI18n()

const COOKIE_KEY = 'cookie_consent'

function parseCookie() {
  try {
    const raw = document.cookie
      .split('; ')
      .find(row => row.startsWith(`${COOKIE_KEY}=`))
      ?.split('=')
      .slice(1)
      .join('=')
    return raw ? JSON.parse(decodeURIComponent(raw)) : null
  } catch {
    return null
  }
}

const hasConsented = ref(false)

onMounted(() => {
  const record = parseCookie()
  hasConsented.value = !!(record?.accepted && record.version === COOKIE_POLICY_VERSION)
})

function accept() {
  const record = { accepted: true, timestamp: new Date().toISOString(), version: COOKIE_POLICY_VERSION }
  document.cookie = `${COOKIE_KEY}=${encodeURIComponent(JSON.stringify(record))}; max-age=${60 * 60 * 24 * 365}; path=/; SameSite=Lax`
  hasConsented.value = true
}
</script>

<template>
  <Transition name="cookie-banner">
    <div
      v-if="!hasConsented"
      class="fixed bottom-0 left-0 right-0 z-50 border-t border-border bg-background/95 backdrop-blur-sm p-4 shadow-lg"
      role="banner"
      aria-label="Cookie consent"
    >
      <div class="container mx-auto flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-sm text-foreground">
          {{ t('cookieBanner.message') }}
          <NuxtLink to="/cookie-policy" class="underline hover:text-primary ml-1">
            {{ t('cookieBanner.policyLink') }}
          </NuxtLink>
        </p>
        <Button size="sm" class="shrink-0" @click="accept">
          {{ t('cookieBanner.agreeButton') }}
        </Button>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.cookie-banner-enter-active,
.cookie-banner-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.cookie-banner-enter-from,
.cookie-banner-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>
