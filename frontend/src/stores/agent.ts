import { defineStore } from 'pinia'
import { apiRequest } from '@/services/http'
import type { AgentChatResult, PendingAction } from '@/types/api'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  status?: string
}

export const useAgentStore = defineStore('agent', {
  state: () => ({
    messages: [{
      id: 'welcome', role: 'assistant' as const,
      content: '你好，我是银盾智行。你可以查询账户与账单，也可以发起转账、卡片、订阅或理财任务。任何写操作都会先生成不可变摘要，等你独立确认。'
    }] as ChatMessage[],
    conversationId: null as string | null,
    lastResult: null as AgentChatResult | null,
    loading: false,
    decisionLoading: false,
    decisionError: '',
    operationResult: null as Record<string, unknown> | null
  }),
  actions: {
    async send(message: string) {
      const content = message.trim()
      if (!content || this.loading) return null
      this.messages.push({ id: crypto.randomUUID(), role: 'user', content })
      this.loading = true
      try {
        const result = await apiRequest<AgentChatResult>('/api/v1/agent/chat', {
          method: 'POST',
          body: JSON.stringify({ message: content, conversationId: this.conversationId })
        })
        this.conversationId = result.conversationId
        this.lastResult = result
        this.messages.push({ id: crypto.randomUUID(), role: 'assistant', content: result.answer, status: result.status })
        return result
      } catch (error) {
        this.messages.push({
          id: crypto.randomUUID(), role: 'system', status: 'REQUEST_FAILED',
          content: error instanceof Error ? error.message : '请求失败，请稍后重试。'
        })
        return null
      } finally { this.loading = false }
    },
    async decide(action: PendingAction, confirmed: boolean) {
      if (this.decisionLoading) return
      this.decisionLoading = true
      this.decisionError = ''
      try {
        this.operationResult = await apiRequest<Record<string, unknown>>(
          `/api/v1/operations/${encodeURIComponent(action.operationId)}/confirm`,
          { method: 'POST', body: JSON.stringify({ challengeId: action.challengeId, confirmed }) }
        )
        this.messages.push({
          id: crypto.randomUUID(), role: 'system',
          content: confirmed ? '独立确认已提交。操作结果已由安全执行链返回。' : '你已拒绝该操作，未提交业务变更。',
          status: confirmed ? 'CONFIRMED' : 'CANCELLED'
        })
        if (this.lastResult) this.lastResult.pendingActions = []
      } catch (error) {
        this.decisionError = error instanceof Error ? error.message : '确认失败，请重新检查操作状态。'
        this.messages.push({ id: crypto.randomUUID(), role: 'system', content: this.decisionError, status: 'CONFIRMATION_FAILED' })
      } finally { this.decisionLoading = false }
    },
    resetConversation() {
      this.$reset()
    }
  }
})
