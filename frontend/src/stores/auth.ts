import { defineStore } from 'pinia'
import { apiRequest, currentAccessToken, setAccessToken } from '@/services/http'
import type { AssuranceLevel, CurrentUser, LoginResponse, MeResponse } from '@/types/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: currentAccessToken() as string | null,
    user: null as CurrentUser | null,
    assuranceLevel: 'AAL1' as AssuranceLevel,
    aal2Available: false,
    ready: false
  }),
  getters: { authenticated: state => Boolean(state.token && state.user) },
  actions: {
    async bootstrap() {
      if (!this.token) { this.ready = true; return }
      try { await this.refreshMe() } catch { this.clear() }
      finally { this.ready = true }
    },
    async login(username: string, password: string) {
      const result = await apiRequest<LoginResponse>('/api/v1/auth/login', {
        method: 'POST', body: JSON.stringify({ username, password })
      })
      setAccessToken(result.accessToken)
      this.token = result.accessToken
      this.user = result.user
      this.assuranceLevel = result.assuranceLevel
      await this.refreshMe()
    },
    async refreshMe() {
      const result = await apiRequest<MeResponse>('/api/v1/auth/me')
      this.user = result.user
      this.assuranceLevel = result.assuranceLevel
      this.aal2Available = result.aal2Available
    },
    async stepUp(code: string) {
      const result = await apiRequest<{ assuranceLevel: AssuranceLevel }>('/api/v1/auth/step-up', {
        method: 'POST', body: JSON.stringify({ code })
      })
      this.assuranceLevel = result.assuranceLevel
      this.aal2Available = false
    },
    async logout() {
      try { await apiRequest<void>('/api/v1/auth/logout', { method: 'POST' }) }
      finally { this.clear() }
    },
    clear() {
      setAccessToken(null)
      this.token = null
      this.user = null
      this.assuranceLevel = 'AAL1'
      this.aal2Available = false
    }
  }
})
