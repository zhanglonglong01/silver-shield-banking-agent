import { afterEach, describe, expect, it, vi } from 'vitest'
import { registerAgentWebMcp } from '@/services/webmcp'

describe('WebMCP staging boundary', () => {
  afterEach(() => { Reflect.deleteProperty(document, 'modelContext') })

  it('registers only a start/stage action and never a confirmation tool', async () => {
    let registered: any
    Object.defineProperty(document, 'modelContext', { configurable: true, value: { registerTool: (tool: unknown) => { registered = tool } } })
    const send = vi.fn().mockResolvedValue({ status: 'SUCCESS', conversationId: 'c1', pendingActions: [{}] })
    expect(registerAgentWebMcp(send, new AbortController().signal)).toBe(true)
    expect(registered.name).toBe('start_banking_agent_request')
    expect(registered.description).toContain('不能完成确认')
    expect(registered.inputSchema.additionalProperties).toBe(false)
    await expect(registered.execute({ message: '冻结银行卡' })).resolves.toEqual({ status: 'SUCCESS', conversationId: 'c1', confirmationRequired: true })
  })

  it('rejects extra fields and does not call the agent', async () => {
    let registered: any
    Object.defineProperty(document, 'modelContext', { configurable: true, value: { registerTool: (tool: unknown) => { registered = tool } } })
    const send = vi.fn()
    registerAgentWebMcp(send, new AbortController().signal)
    await expect(registered.execute({ message: '转账', confirmed: true })).rejects.toThrow('INVALID_WEBMCP_INPUT')
    expect(send).not.toHaveBeenCalled()
  })
})
