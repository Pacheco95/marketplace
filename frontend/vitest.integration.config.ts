import { defineVitestConfig } from '@nuxt/test-utils/config'

export default defineVitestConfig({
  test: {
    environment: 'nuxt',
    include: ['tests/integration/**/*.test.ts'],
    environmentOptions: {
      nuxt: {
        rootDir: '.',
      },
    },
  },
})
