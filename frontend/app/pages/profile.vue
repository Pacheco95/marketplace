<script setup lang="ts">
import { useAuthStore } from '~/stores/useAuthStore'

definePageMeta({ middleware: 'auth' })

const { t } = useI18n()
const store = useAuthStore()
</script>

<template>
  <div class="flex min-h-screen flex-col items-center justify-center bg-background px-4">
    <div class="w-full max-w-md space-y-6 rounded-lg border bg-card p-8 shadow-sm">
      <div class="flex flex-col items-center space-y-4">
        <div v-if="store.profilePictureUrl" class="h-20 w-20 overflow-hidden rounded-full">
          <img
            :src="store.profilePictureUrl"
            :alt="store.displayName"
            class="h-full w-full object-cover"
          />
        </div>
        <div
          v-else-if="store.initials"
          class="flex h-20 w-20 items-center justify-center rounded-full bg-primary text-2xl font-bold text-primary-foreground"
        >
          {{ store.initials }}
        </div>
        <div
          v-else
          class="flex h-20 w-20 items-center justify-center rounded-full bg-muted text-muted-foreground"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-10 w-10"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
            />
          </svg>
        </div>

        <div class="text-center">
          <h1 class="text-2xl font-bold">
            {{ t('auth.profile.welcome', { name: store.displayName }) }}
          </h1>
          <p class="text-sm text-muted-foreground">
            {{ t('auth.profile.subline') }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
