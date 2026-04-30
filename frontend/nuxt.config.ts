// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  future: { compatibilityVersion: 4 },
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  modules: ['@pinia/nuxt', '@nuxtjs/i18n', 'shadcn-nuxt', '@nuxt/eslint'],
  css: ['~/assets/css/main.css'],
  shadcn: {
    prefix: '',
    componentDir: './app/components/ui',
  },
  i18n: {
    locales: [{ code: 'en', language: 'en-US', name: 'English', file: 'en.json' }],
    defaultLocale: 'en',
    lazy: true,
    langDir: 'locales',
    strategy: 'prefix_except_default',
    vueI18n: './i18n/i18n.config.ts',
  },
  vite: {
    plugins: [(await import('@tailwindcss/vite')).default()],
  },
  eslint: {
    config: {
      typescript: true,
    },
  },
})
