import type { ApiErrorBody } from '@/types/api'

const TOKEN_KEY = 'silver-shield.session-token'
let accessToken = typeof sessionStorage === 'undefined' ? null : sessionStorage.getItem(TOKEN_KEY)

export class ApiClientError extends Error {
  constructor(public readonly status: number, public readonly code: string, message: string) {
    super(message)
    this.name = 'ApiClientError'
  }
}

export function currentAccessToken() { return accessToken }

export function setAccessToken(token: string | null) {
  accessToken = token
  if (typeof sessionStorage === 'undefined') return
  if (token) sessionStorage.setItem(TOKEN_KEY, token)
  else sessionStorage.removeItem(TOKEN_KEY)
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  headers.set('Accept', 'application/json')

  const response = await fetch(path, { ...init, headers })
  if (!response.ok) {
    let body: Partial<ApiErrorBody> = {}
    try { body = await response.json() as ApiErrorBody } catch { /* fail closed to generic message */ }
    if (response.status === 401 && path !== '/api/v1/auth/login') {
      setAccessToken(null)
      if (typeof window !== 'undefined') window.dispatchEvent(new Event('silver-shield:unauthorized'))
    }
    throw new ApiClientError(response.status, body.code ?? 'REQUEST_FAILED', body.message ?? '请求失败')
  }
  if (response.status === 204) return undefined as T
  const text = await response.text()
  if (!text) return undefined as T
  return JSON.parse(text) as T
}
