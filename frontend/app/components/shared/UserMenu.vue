<script setup lang="ts">
import { useAuthStore } from '~/stores/useAuthStore'
import { useAuth } from '~/composables/useAuth'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '~/components/ui/dropdown-menu'
import { Avatar, AvatarImage, AvatarFallback } from '~/components/ui/avatar'
import { Button } from '~/components/ui/button'

const { t } = useI18n()
const store = useAuthStore()
const { login, logout } = useAuth()
</script>

<template>
  <DropdownMenu v-if="store.isAuthenticated">
    <DropdownMenuTrigger as-child>
      <Button
        variant="ghost"
        class="h-10 w-10 rounded-full p-0"
        :aria-label="t('auth.menu.welcome', { name: store.displayName })"
      >
        <Avatar class="h-9 w-9">
          <AvatarImage
            v-if="store.profilePictureUrl"
            :src="store.profilePictureUrl"
            :alt="store.displayName ?? ''"
          />
          <AvatarFallback>{{ store.initials }}</AvatarFallback>
        </Avatar>
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" class="w-48">
      <DropdownMenuLabel>
        {{ t('auth.menu.welcome', { name: store.displayName }) }}
      </DropdownMenuLabel>
      <DropdownMenuSeparator />
      <DropdownMenuItem as-child>
        <NuxtLink to="/profile">
          {{ t('auth.menu.profile') }}
        </NuxtLink>
      </DropdownMenuItem>
      <DropdownMenuItem @click="logout">
        {{ t('auth.menu.logout') }}
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>

  <Button v-else variant="outline" size="sm" @click="login">
    {{ t('auth.menu.login') }}
  </Button>
</template>
