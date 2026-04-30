import { useAuthStore } from '~/stores/useAuthStore'

export default defineNuxtRouteMiddleware(async (to) => {
  const store = useAuthStore()
  if (!store.isAuthenticated) {
    await store.fetchCurrentUser()
  }
  if (!store.isAuthenticated) {
    return navigateTo(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }
})
