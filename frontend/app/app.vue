<script setup lang="ts">
import { useAuthStore } from '~/stores/useAuthStore'
import { useGoogleOneTap } from '~/composables/useGoogleOneTap'

const store = useAuthStore()
const { prompt } = useGoogleOneTap()

onMounted(async () => {
  await store.fetchCurrentUser()
  if (!store.isAuthenticated) {
    prompt()
  }
})

useHead({
  htmlAttrs: { lang: 'en' },
  link: [
    { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
    { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
    {
      rel: 'stylesheet',
      href: 'https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&display=swap',
      media: 'print',
      onload: "this.media='all'",
    },
  ],
})
</script>

<template>
  <NuxtRouteAnnouncer />
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
  <ClientOnly>
    <SharedCookieBanner />
  </ClientOnly>
</template>
