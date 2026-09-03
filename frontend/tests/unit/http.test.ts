import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, currentAccessToken, setAccessToken } from '@/services/http'

describe('secure HTTP client', () => {
  beforeEach(() => setAccessToken(null))

  it('attaches bearer token without placing it in the URL', async () => {
    setAccessToken('opaque-demo-token')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }))
    await apiRequest('/api/v1/accounts')
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/v1/accounts')
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer opaque-demo-token')
  })

  it('revokes local bearer material when server returns 401', async () => {
    setAccessToken('expired-token')
    const event = vi.fn()
    window.addEventListener('silver-shield:unauthorized', event, { once: true })
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ code: 'AUTH_REQUIRED', message: '需要登录后访问' }), { status: 401 }))
    await expect(apiRequest('/api/v1/accounts')).rejects.toMatchObject({ status: 401, code: 'AUTH_REQUIRED' })
    expect(currentAccessToken()).toBeNull()
    expect(event).toHaveBeenCalledOnce()
  })

  it('does not erase an existing session after a failed login attempt', async () => {
    setAccessToken('current-token')
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ code: 'AUTH_INVALID_CREDENTIALS', message: '用户名或密码错误' }), { status: 401 }))
    await expect(apiRequest('/api/v1/auth/login', { method: 'POST' })).rejects.toMatchObject({ status: 401 })
    expect(currentAccessToken()).toBe('current-token')
  })

  it('accepts an empty successful logout response', async () => {
    setAccessToken('current-token')
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }))
    await expect(apiRequest<void>('/api/v1/auth/logout', { method: 'POST' })).resolves.toBeUndefined()
  })
})
