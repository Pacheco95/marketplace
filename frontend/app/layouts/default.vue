<template>
  <div class="min-h-screen bg-background text-foreground">
    <header
      class="sticky top-0 z-30 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60"
    >
      <div class="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6 lg:px-8">
        <NuxtLink to="/" class="text-lg font-bold tracking-tight text-foreground hover:opacity-80">
          Marketplace
        </NuxtLink>
        <SharedUserMenu />
      </div>
    </header>
    <main>
      <slot />
    </main>
  </div>
</template>
