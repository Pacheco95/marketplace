<script setup lang="ts">
import { useAuthStore } from '~/stores/useAuthStore'
import { Card, CardContent } from '~/components/ui/card'
import { Avatar, AvatarImage, AvatarFallback } from '~/components/ui/avatar'

definePageMeta({ middleware: 'auth' })

const { t } = useI18n()
const store = useAuthStore()
</script>

<template>
  <div class="flex min-h-screen flex-col items-center justify-center bg-background px-4">
    <Card class="w-full max-w-md">
      <CardContent class="flex flex-col items-center space-y-4 pt-6">
        <Avatar class="h-20 w-20 text-2xl">
          <AvatarImage
            v-if="store.profilePictureUrl"
            :src="store.profilePictureUrl"
            :alt="store.displayName ?? ''"
          />
          <AvatarFallback>{{ store.initials }}</AvatarFallback>
        </Avatar>

        <div class="text-center">
          <h1 class="text-2xl font-bold">
            {{ t('auth.profile.welcome', { name: store.displayName }) }}
          </h1>
          <p class="text-sm text-muted-foreground">
            {{ t('auth.profile.subline') }}
          </p>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
