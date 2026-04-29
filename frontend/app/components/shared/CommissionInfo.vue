<script setup lang="ts">
const { t } = useI18n()
</script>

<template>
  <section class="py-12 sm:py-16 px-4">
    <div class="container mx-auto max-w-2xl text-center">
      <h2 class="text-2xl font-bold mb-4 sm:text-3xl">
        {{ t('commissionInfo.title') }}
      </h2>
      <p class="text-muted-foreground mb-6 leading-relaxed">
        {{ t('commissionInfo.description') }}
      </p>
      <div class="inline-block rounded-xl border border-border bg-card px-8 py-6 shadow-sm">
        <p class="text-3xl font-bold text-primary">{{ t('commissionInfo.rate') }}</p>
        <p class="text-sm text-muted-foreground mt-2">{{ t('commissionInfo.note') }}</p>
      </div>
    </div>
  </section>
</template>
