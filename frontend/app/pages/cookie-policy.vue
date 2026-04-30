<script setup lang="ts">
import { COOKIE_POLICY_VERSION, COOKIE_POLICY_UPDATED_AT } from '~/config/cookiePolicy'

const { t } = useI18n()

useSeoMeta({
  title: () => t('cookiePolicy.title'),
  description: 'Learn about how we use cookies on our marketplace platform.',
  ogTitle: () => t('cookiePolicy.title'),
  ogDescription: 'Learn about how we use cookies on our marketplace platform.',
  robots: 'index, follow',
})
</script>

<template>
  <div class="container mx-auto px-4 py-12 max-w-3xl">
    <NuxtLink
      to="/"
      class="inline-flex items-center text-sm text-muted-foreground hover:text-primary mb-8"
    >
      &larr; {{ t('cookiePolicy.backLink') }}
    </NuxtLink>

    <h1 class="text-3xl font-bold mb-2">{{ t('cookiePolicy.title') }}</h1>
    <p class="text-sm text-muted-foreground mb-8">
      {{ t('cookiePolicy.version') }}: {{ COOKIE_POLICY_VERSION }} &mdash;
      {{ t('cookiePolicy.lastUpdated') }}: {{ COOKIE_POLICY_UPDATED_AT }}
    </p>

    <section class="mb-8">
      <h2 class="text-xl font-semibold mb-3">
        {{ t('cookiePolicy.sections.whatWeCollect.title') }}
      </h2>
      <p class="text-muted-foreground leading-relaxed">
        {{ t('cookiePolicy.sections.whatWeCollect.body') }}
      </p>
    </section>

    <section class="mb-8">
      <h2 class="text-xl font-semibold mb-3">{{ t('cookiePolicy.sections.purpose.title') }}</h2>
      <p class="text-muted-foreground leading-relaxed">
        {{ t('cookiePolicy.sections.purpose.body') }}
      </p>
    </section>

    <section class="mb-8">
      <h2 class="text-xl font-semibold mb-3">{{ t('cookiePolicy.sections.retention.title') }}</h2>
      <p class="text-muted-foreground leading-relaxed">
        {{ t('cookiePolicy.sections.retention.body') }}
      </p>
    </section>
  </div>
</template>
