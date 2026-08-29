import { defineStore } from 'pinia'
import { api, login as loginRequest, logout as logoutRequest } from '@/services/api'
import type { AuthUser, SiteMembership } from '@/types'
import { getSelectedSite, setSelectedSite } from '@/services/site-context'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: { authenticated: false, email: null, displayName: null, role: null } as AuthUser,
    loaded: false
  }),
  actions: {
    async load() {
      let user = await api<AuthUser>('/api/auth/me')

      // An administrator may keep a site key in localStorage after that
      // membership is revoked or the site is archived. The switcher endpoint
      // is intentionally recoverable, so retry /me with the first valid
      // membership instead of forcing a new login for an otherwise valid
      // session.
      if (!user.authenticated && getSelectedSite()) {
        try {
          const sites = await api<SiteMembership[]>('/api/admin/sites/available')
          const fallback = sites[0]
          if (fallback) {
            setSelectedSite(fallback.siteKey)
            user = await api<AuthUser>('/api/auth/me')
          }
        } catch {
          // A genuinely anonymous session is handled by the router as before.
        }
      }

      if (user.authenticated && user.siteKey) setSelectedSite(user.siteKey)
      this.user = user
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
