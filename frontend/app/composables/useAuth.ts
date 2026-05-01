import { useAuthStore } from '~/stores/useAuthStore'

export function useAuth() {
  const store = useAuthStore()
  const router = useRouter()

  function login() {
    window.location.href = '/api/v1/auth/login'
  }

  async function logout() {
    await $fetch('/api/v1/auth/logout', {
      method: 'POST',
      credentials: 'include',
    }).catch(() => {})
    store.clearUser()
    await router.push('/')
  }

  async function refreshSession() {
    await $fetch('/api/v1/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    })
  }

  async function handleSessionExpired() {
    store.clearUser()
    await router.push('/login?sessionExpired=true')
  }

  return { login, logout, refreshSession, handleSessionExpired }
}
