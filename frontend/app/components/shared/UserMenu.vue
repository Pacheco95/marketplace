<script setup lang="ts">
import { useAuthStore } from '~/stores/useAuthStore'
import { useAuth } from '~/composables/useAuth'

const { t } = useI18n()
const store = useAuthStore()
const { login, logout } = useAuth()

const isOpen = ref(false)

function toggle() {
  isOpen.value = !isOpen.value
}

function close() {
  isOpen.value = false
}

async function handleLogout() {
  close()
  await logout()
}
</script>

<template>
  <div class="relative">
    <!-- Authenticated trigger -->
    <button
      v-if="store.isAuthenticated"
      class="flex h-10 w-10 items-center justify-center overflow-hidden rounded-full focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
      :aria-label="t('auth.menu.welcome', { name: store.displayName })"
      @click="toggle"
    >
      <img
        v-if="store.profilePictureUrl"
        :src="store.profilePictureUrl"
        :alt="store.displayName"
        class="h-full w-full object-cover"
      />
      <span
        v-else-if="store.initials"
        class="flex h-full w-full items-center justify-center bg-primary text-sm font-semibold text-primary-foreground"
        >{{ store.initials }}</span
      >
      <svg
        v-else
        xmlns="http://www.w3.org/2000/svg"
        class="h-6 w-6 text-muted-foreground"
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
    </button>

    <!-- Unauthenticated trigger -->
    <button
      v-else
      class="rounded-md px-3 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground focus:outline-none focus:ring-2 focus:ring-ring"
      @click="login"
    >
      {{ t('auth.menu.login') }}
    </button>

    <!-- Dropdown menu -->
    <div
      v-if="isOpen && store.isAuthenticated"
      class="absolute right-0 top-12 z-50 min-w-48 rounded-md border bg-popover p-1 shadow-md"
    >
      <div class="px-3 py-2 text-sm font-medium text-foreground">
        {{ t('auth.menu.welcome', { name: store.displayName }) }}
      </div>
      <hr class="my-1 border-border" />
      <NuxtLink
        to="/profile"
        class="block rounded-sm px-3 py-2 text-sm hover:bg-accent hover:text-accent-foreground"
        @click="close"
      >
        {{ t('auth.menu.profile') }}
      </NuxtLink>
      <button
        class="w-full rounded-sm px-3 py-2 text-left text-sm hover:bg-accent hover:text-accent-foreground"
        @click="handleLogout"
      >
        {{ t('auth.menu.logout') }}
      </button>
    </div>

    <!-- Click-outside overlay -->
    <div v-if="isOpen" class="fixed inset-0 z-40" @click="close" />
  </div>
</template>
