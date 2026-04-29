<script setup lang="ts">
import { Card, CardHeader, CardTitle, CardContent } from '~/components/ui/card'

const { t } = useI18n()

const featureKeys = ['transparent', 'scalable', 'trusted'] as const
</script>

<template>
  <section class="py-12 sm:py-16 px-4 bg-muted/40">
    <div class="container mx-auto max-w-5xl">
      <h2 class="text-2xl font-bold text-center mb-10 sm:text-3xl">
        {{ t('features.title') }}
      </h2>
      <div class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        <Card v-for="key in featureKeys" :key="key">
          <CardHeader>
            <CardTitle>{{ t(`features.items.${key}.title`) }}</CardTitle>
          </CardHeader>
          <CardContent>
            <p class="text-muted-foreground text-sm leading-relaxed">
              {{ t(`features.items.${key}.description`) }}
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  </section>
</template>
