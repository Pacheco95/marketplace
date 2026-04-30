import { useAuthStore } from '~/stores/useAuthStore'

export default defineNuxtPlugin(() => {
  const store = useAuthStore()
  const router = useRouter()

  async function handleSessionExpired() {
    store.clearUser()
    await router.push('/login?sessionExpired=true')
  }

  const originalFetch = globalThis.$fetch

  globalThis.$fetch = new Proxy(originalFetch, {
    apply: async (target, thisArg, args: Parameters<typeof $fetch>) => {
      try {
        return await Reflect.apply(target, thisArg, args)
      } catch (err: unknown) {
        const status = (err as { status?: number })?.status
        if (status === 401) {
          try {
            await $fetch('/api/v1/auth/refresh', {
              method: 'POST',
              credentials: 'include',
            })
            return await Reflect.apply(target, thisArg, args)
          } catch {
            await handleSessionExpired()
            throw err
          }
        }
        throw err
      }
    },
  }) as typeof $fetch
})
