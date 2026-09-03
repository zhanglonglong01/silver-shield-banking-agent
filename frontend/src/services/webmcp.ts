import type { AgentChatResult } from '@/types/api'

interface WebMcpTool {
  name: string
  title: string
  description: string
  inputSchema: Record<string, unknown>
  annotations: { readOnlyHint: boolean; untrustedContentHint: boolean }
  execute(input: unknown): Promise<unknown>
}

interface WebMcpContext {
  registerTool(tool: WebMcpTool, options?: { signal?: AbortSignal }): void | Promise<void>
}

declare global {
  interface Document { readonly modelContext?: WebMcpContext }
}

export function registerAgentWebMcp(
  send: (message: string) => Promise<AgentChatResult | null>,
  signal: AbortSignal
) {
  const context = document.modelContext
  if (!context?.registerTool) return false
  const tool: WebMcpTool = {
    name: 'start_banking_agent_request',
    title: '发起受控银行智能体请求',
    description: '在当前页面发起银行智能体请求。写操作最多只会生成待确认预览；此工具不能完成确认或提交资金、卡片、订阅、理财变更。',
    inputSchema: {
      type: 'object',
      properties: { message: { type: 'string', minLength: 1, maxLength: 4000 } },
      required: ['message'],
      additionalProperties: false
    },
    annotations: { readOnlyHint: false, untrustedContentHint: true },
    async execute(input) {
      const message = exactMessage(input)
      const result = await send(message)
      return result
        ? { status: result.status, conversationId: result.conversationId, confirmationRequired: result.pendingActions.length > 0 }
        : { status: 'REQUEST_FAILED', confirmationRequired: false }
    }
  }
  try {
    void Promise.resolve(context.registerTool(tool, { signal })).catch(() => undefined)
    return true
  } catch { return false }
}

function exactMessage(input: unknown) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw new Error('INVALID_WEBMCP_INPUT')
  const record = input as Record<string, unknown>
  if (Object.keys(record).length !== 1 || typeof record.message !== 'string') throw new Error('INVALID_WEBMCP_INPUT')
  const message = record.message.trim()
  if (!message || message.length > 4000) throw new Error('INVALID_WEBMCP_INPUT')
  return message
}
