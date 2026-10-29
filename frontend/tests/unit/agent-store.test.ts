import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setAccessToken } from '@/services/http'
import { useAgentStore } from '@/stores/agent'
import type { PendingAction } from '@/types/api'

const action: PendingAction = {
  operationId: '11111111-1111-1111-1111-111111111111', status: 'PENDING_CONFIRMATION', requiredAal: 'AAL1',
  expiresAt: '2026-08-31T12:05:00Z', challengeId: '22222222-2222-2222-2222-222222222222',
  confirmEndpoint: '/api/v1/operations/11111111-1111-1111-1111-111111111111/confirm', summary: { amount: '500.00' }
}

describe('agent store', () => {
  beforeEach(() => { setActivePinia(createPinia()); setAccessToken('demo-token') })

  it('reuses conversation id for multi-turn messages', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(JSON.stringify({ answer: '第一轮', conversationId: 'conversation-1', status: 'SUCCESS', usedTools: [], evidenceIds: [], executionSummary: 'ok', pendingActions: [], taskPlan: null }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ answer: '第二轮', conversationId: 'conversation-1', status: 'SUCCESS', usedTools: [], evidenceIds: [], executionSummary: 'ok', pendingActions: [], taskPlan: null }), { status: 200 }))
    const store = useAgentStore()
    await store.send('查询账户')
    await store.send('再看余额')
    const secondBody = JSON.parse(String(fetchMock.mock.calls[1][1]?.body))
    expect(secondBody.conversationId).toBe('conversation-1')
  })

  it('submits an exact confirmation body to the independent operation endpoint', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ status: 'SUCCEEDED' }), { status: 200 }))
    const store = useAgentStore()
    await store.decide(action, true)
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/v1/operations/11111111-1111-1111-1111-111111111111/confirm')
    expect(JSON.parse(String(init?.body))).toEqual({ challengeId: action.challengeId, confirmed: true })
  })

  it('supports explicit rejection without changing the challenge binding', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ status: 'CANCELLED' }), { status: 200 }))
    const store = useAgentStore()
    await store.decide(action, false)
    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual({ challengeId: action.challengeId, confirmed: false })
  })

  it('keeps the pending action visible when confirmation fails closed', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ code: 'AAL2_REQUIRED', message: '认证强度不足' }), { status: 403 }))
    const store = useAgentStore()
    store.lastResult = { answer: '', conversationId: 'c1', status: 'SUCCESS', usedTools: [], evidenceIds: [], executionSummary: '', pendingActions: [action], taskPlan: null }
    await store.decide(action, true)
    expect(store.lastResult.pendingActions).toHaveLength(1)
    expect(store.decisionError).toBe('认证强度不足')
  })

  it('uses the server outcome instead of claiming success for a denied HTTP 200 confirmation', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ outcome: 'DENIED',
      reasonCode: 'AAL2_REQUIRED', explanation: '请完成加强认证', operation: { status: 'AWAITING_STEP_UP' } }), { status: 200 }))
    const store = useAgentStore()
    store.lastResult = { answer: '', conversationId: 'c1', status: 'SUCCESS', usedTools: [], evidenceIds: [], executionSummary: '', pendingActions: [action], taskPlan: null }
    await store.decide(action, true)
    expect(store.messages.at(-1)?.status).toBe('DENIED')
    expect(store.messages.at(-1)?.content).toContain('操作未完成')
    expect(store.lastResult.pendingActions).toHaveLength(1)
  })

  it('shows final success only when the server returns a succeeded outcome', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ outcome: 'SUCCEEDED', receipt: { status: 'SUCCEEDED' } }), { status: 200 }))
    const store = useAgentStore()
    await store.decide(action, true)
    expect(store.messages.at(-1)?.status).toBe('SUCCEEDED')
    expect(store.messages.at(-1)?.content).toContain('成功回执')
  })
})
