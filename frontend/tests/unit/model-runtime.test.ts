import { mount, flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import ModelRuntimeBadge from '@/components/ModelRuntimeBadge.vue'

describe('model runtime badge', () => {
  it('shows the server-selected live provider and model', async () => {
    const request = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ mode: 'LIVE_MODEL', provider: 'DeepSeek', model: 'deepseek-flash', configured: true }), { status: 200 }))
    const wrapper = mount(ModelRuntimeBadge)
    await flushPromises()
    expect(request.mock.calls[0][0]).toBe('/api/v1/agent/runtime')
    expect(wrapper.text()).toBe('DeepSeek · deepseek-flash')
  })
  it('labels an offline instance explicitly', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ mode: 'OFFLINE_DEMO', provider: '离线演示', model: 'deterministic', configured: false }), { status: 200 }))
    const wrapper = mount(ModelRuntimeBadge)
    await flushPromises()
    expect(wrapper.text()).toBe('离线演示模式')
  })
  it('does not invent a provider when metadata cannot be fetched', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('Unavailable'))
    const wrapper = mount(ModelRuntimeBadge)
    await flushPromises()
    expect(wrapper.text()).toBe('模型配置暂不可读')
  })
  it('does not claim an unconfigured model is live', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ mode: 'UNAVAILABLE', provider: 'DeepSeek', model: 'deepseek-flash', configured: false }), { status: 200 }))
    const wrapper = mount(ModelRuntimeBadge)
    await flushPromises()
    expect(wrapper.text()).toBe('模型尚未配置')
  })
})
