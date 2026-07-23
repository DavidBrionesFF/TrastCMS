import { defineStore } from 'pinia'
import { api, login as loginRequest, logout as logoutRequest } from '@/services/api'
import type { AuthUser } from '@/types'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: { authenticated: false, email: null, displayName: null, role: null } as AuthUser,
    loaded: false
  }),
  actions: {
    async load() {
      this.user = await api<AuthUser>('/api/auth/me')
      this.loaded = true
    },
    async login(email: string, password: string) {
      await loginRequest(email, password)
      await this.load()
    },
    async logout() {
      await logoutRequest()
      this.user = { authenticated: false, email: null, displayName: null, role: null }
    }
  }
})
