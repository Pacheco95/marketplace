<script setup lang="ts">
import { Button } from '~/components/ui/button'

const { t } = useI18n()
</script>

<template>
  <section class="py-16 sm:py-24 px-4 text-center">
    <div class="container mx-auto max-w-4xl">
      <h1
        class="text-3xl font-bold tracking-tight sm:text-5xl xl:text-6xl mb-6"
      >
        {{ t('hero.headline') }}
      </h1>
      <p
        class="text-lg text-muted-foreground sm:text-xl max-w-2xl mx-auto mb-8"
      >
        {{ t('hero.subheadline') }}
      </p>
      <Button size="lg">
        {{ t('hero.cta') }}
      </Button>
    </div>
  </section>
</template>
