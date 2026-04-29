# Quickstart: Frontend Setup (Nuxt 4)

## Prerequisites
- [Bun](https://bun.sh/) installed.
- Claude Code or similar AI client with MCP support.

## Initialization

1. **Create the Nuxt project**:
    This step requires the user to interactively select the options.
    ```bash
    bun create nuxt@latest frontend
    ```
    *Select `minimal` template when prompted.*
    *Select `bun` as the package manager when prompted.*
    *Select `No` to not initialize a git repository when prompted.*
    *Select `yes` to browse and install modules. Then pick `@nuxtjs/i18n`, `@pinia/nuxt` and `shadcn-nuxt` when prompted.*

2. **Initialize MCP (Optional but Recommended)**:
    ```bash
    npx shadcn-vue@latest mcp init --client claude
    ```

## Development
```bash
bun --bun run dev
```

## Architecture Reminders (Nuxt 4)
- App source code lives in `app/`.
- Shared logic (client/server) lives in `shared/`.
- Internationalization config and locales live in `i18n/`.
- Logic goes in `app/composables/`.
- UI components in `app/components/` must be stateless.
- Global state (Pinia) in `app/stores/`.
- Use ShadCN components via `bunx shadcn-vue@latest add [component]`.
