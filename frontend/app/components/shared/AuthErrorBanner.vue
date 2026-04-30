<script setup lang="ts">
defineProps<{
  message: string
}>()

defineEmits<{
  dismiss: []
}>()
</script>

<template>
  <div
    v-if="message"
    role="alert"
    class="flex items-start justify-between gap-3 rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive"
  >
    <span>{{ message }} Please try again.</span>
    <button
      class="ml-auto shrink-0 rounded p-0.5 hover:bg-destructive/20 focus:outline-none focus:ring-2 focus:ring-destructive"
      :aria-label="'Dismiss error'"
      @click="$emit('dismiss')"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        class="h-4 w-4"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M6 18L18 6M6 6l12 12"
        />
      </svg>
    </button>
  </div>
</template>
