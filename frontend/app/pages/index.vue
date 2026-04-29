<script setup lang="ts">
const { t } = useI18n()

useSeoMeta({
  title: () => t('hero.headline'),
  description: () => t('hero.subheadline'),
  ogTitle: () => t('hero.headline'),
  ogDescription: () => t('hero.subheadline'),
  robots: 'index, follow',
})
</script>

<template>
  <div>
    <SharedHero />
    <SharedFeatures />
    <SharedCommissionInfo />
  </div>
</template>
