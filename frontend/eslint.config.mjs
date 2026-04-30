import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'

export default [
  {
    ignores: [
      '**/.output/**',
      '**/.nuxt/**',
      '**/dist/**',
      '**/node_modules/**',
      'app/components/ui/**',
      'coverage/**',
    ],
  },

  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
        ...globals.es2021,
        // Nuxt auto-imports
        useHead: 'readonly',
        useI18n: 'readonly',
        useSeoMeta: 'readonly',
        ref: 'readonly',
        onMounted: 'readonly',
      },
      parserOptions: {
        parser: '@typescript-eslint/parser',
      },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'no-undef': 'error',
    },
  },
]
