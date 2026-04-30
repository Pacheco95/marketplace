import { defineStore } from 'pinia'
import type { UserProfile } from '~/types/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as UserProfile | null,
    authError: null as string | null,
  }),

  getters: {
    isAuthenticated: (state) => state.user !== null,

    displayName: (state): string => {
      if (state.user?.displayName) return state.user.displayName
      if (state.user?.email) return state.user.email.split('@')[0] ?? ''
      return ''
    },

    initials: (state): string => {
      if (state.user?.displayName) {
        const words = state.user.displayName.trim().split(/\s+/)
        const letters = words
          .map((w) => w[0])
          .filter((c) => c && /[a-zA-Z]/.test(c))
          .map((c) => c!.toUpperCase())
        return letters.slice(0, 2).join('')
      }
      if (state.user?.email) {
        const first = state.user.email[0]
        return first ? first.toUpperCase() : ''
      }
      return ''
    },

    profilePictureUrl: (state) => state.user?.profilePictureUrl ?? null,
  },

  actions: {
    setUser(user: UserProfile) {
      this.user = user
    },

    clearUser() {
      this.user = null
    },

    setError(msg: string) {
      this.authError = msg
    },

    clearError() {
      this.authError = null
    },

    async fetchCurrentUser() {
      try {
        const data = await $fetch<UserProfile>('/api/v1/users/me', {
          credentials: 'include',
        })
        this.setUser(data)
      } catch (err: unknown) {
        const status = (err as { status?: number })?.status
        if (status === 401) {
          this.clearUser()
        } else {
          console.error('Failed to fetch current user:', err)
          this.clearUser()
        }
      }
    },
  },
})
