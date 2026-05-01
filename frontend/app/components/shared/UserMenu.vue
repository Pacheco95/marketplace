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
import { UserCircle } from 'lucide-vue-next'

const { t } = useI18n()
const store = useAuthStore()
const { login, logout } = useAuth()
</script>

<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button
        variant="ghost"
        class="h-10 w-10 rounded-full p-0"
        :aria-label="
          store.isAuthenticated
            ? t('auth.menu.welcome', { name: store.displayName })
            : t('auth.menu.login')
        "
      >
        <Avatar v-if="store.isAuthenticated" class="h-9 w-9">
          <AvatarImage
            v-if="store.profilePictureUrl"
            :src="store.profilePictureUrl"
            :alt="store.displayName ?? ''"
          />
          <AvatarFallback>{{ store.initials }}</AvatarFallback>
        </Avatar>
        <UserCircle v-else class="h-6 w-6 text-muted-foreground" />
      </Button>
    </DropdownMenuTrigger>

    <DropdownMenuContent align="end" class="w-48">
      <template v-if="store.isAuthenticated">
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
      </template>
      <template v-else>
        <DropdownMenuItem @click="login">
          {{ t('auth.menu.login') }}
        </DropdownMenuItem>
      </template>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
