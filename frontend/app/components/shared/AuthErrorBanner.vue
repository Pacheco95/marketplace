<script setup lang="ts">
import { Alert, AlertDescription } from '~/components/ui/alert'
import { Button } from '~/components/ui/button'
import { X } from 'lucide-vue-next'

defineProps<{
  message: string
}>()

defineEmits<{
  dismiss: []
}>()
</script>

<template>
  <Alert v-if="message" variant="destructive" class="flex items-start gap-3">
    <AlertDescription class="flex-1"> {{ message }} Please try again. </AlertDescription>
    <Button
      variant="ghost"
      size="icon"
      class="-mr-1 -mt-1 h-7 w-7 shrink-0 text-destructive hover:bg-destructive/10 hover:text-destructive"
      :aria-label="'Dismiss error'"
      @click="$emit('dismiss')"
    >
      <X class="h-4 w-4" />
    </Button>
  </Alert>
</template>
