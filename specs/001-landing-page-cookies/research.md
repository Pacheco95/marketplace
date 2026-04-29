# Research: Nuxt 4, Bun, ShadCN Vue & Pinia Integration

## Decision: Project Initialization
- **Tool**: Nuxt 4 (v4.4.0) with Bun.
- **Rationale**: User requested the latest version. Nuxt 4 introduces improved performance (28x faster route generation), accessibility features (`useAnnouncer`), and a more robust directory structure.
- **Command**: `bun create nuxt@latest frontend`.

## Decision: UI Framework & Design System
- **Tool**: ShadCN Vue via `shadcn-nuxt` module.
- **Rationale**: Nuxt 4 compatibility. Ensure `componentDir` in `nuxt.config.ts` points to the new `app/components/ui` if using the Nuxt 4 layout.
- **Setup Flow**:
  Nuxt 4 CLI has the feature to add dependencies during the initialization command.
  The user must choose to browse the dependencies and pick `@nuxtjs/tailwindcss`, `@nuxtjs/i18n`, `@pinia/nuxt` and `shadcn-nuxt` when prompted.

## Decision: State Management & Logic Separation
- **Pattern**: Pinia (Setup Stores), Composables, Stateless Components.
- **Nuxt 4 Specifics**:
  - Use the new `shared/` directory for logic that might be needed in both `app/` and `server/` (e.g., cookie consent validation logic).
  - Use `createUseFetch` for standardized API calling patterns as recommended in Nuxt 4.
  - Pinia stores should be placed in `app/stores/`.

## Decision: Internationalization (i18n)
- **Tool**: `@nuxtjs/i18n` (v10.x for Nuxt 4 compatibility).
- **Rationale**: Constitution Principle VII requires i18n by default.
- **Setup Flow**:
  1. Ensure `@nuxtjs/i18n` was correctly installed after the nuxt project generation. (The user will pick this dependency at initialization time)
  2. Create `i18n/` directory at root if not created already.
  3. Configure `nuxt.config.ts` with `lazy: true` and `langDir: 'locales'`.
- **Best Practice**: Use `i18n/i18n.config.ts` for Vue I18n settings and separate JSON files in `i18n/locales/` for translations.

## Decision: Testing Strategy
- **Unit Testing**: Vitest with `@nuxt/test-utils`.
- **E2E Testing**: Playwright.
- **Rationale**: Playwright remains the recommendation for Nuxt 4 due to its superior handling of SSR and hydration.

## Unresolved Clarifications
- None.

## References
- [Nuxt 4 Release Notes (v4.4.0)](https://github.com/nuxt/nuxt/releases/tag/v4.4.0)
