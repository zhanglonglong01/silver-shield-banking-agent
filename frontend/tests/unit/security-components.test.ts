import { createPinia } from 'pinia'
import { render } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'
import ExplanationDrawer from '@/components/ExplanationDrawer.vue'
import TaskPlanPanel from '@/components/TaskPlanPanel.vue'
import type { AgentChatResult, TaskPlan } from '@/types/api'

const plan: TaskPlan = {
  planId: 'p1', conversationId: 'c1', goal: '账单与订阅联动', status: 'REJECTED', missingSlots: [],
  statusReason: 'TOOL_NOT_IN_ALLOWED_SET', updatedAt: '2026-08-31T12:00:00Z',
  steps: [{ stepId: 'step-1', tool: { name: 'list_transactions', version: '1.0' }, accessMode: 'READ_ONLY', dependsOn: [], status: 'FAILED', evidenceIds: [], reasonCode: 'TOOL_NOT_IN_ALLOWED_SET' }]
}

describe('security explanation components', () => {
  it('renders model-controlled text as text, never executable HTML', () => {
    const result: AgentChatResult = {
      answer: '<img src=x onerror=alert(1)>', conversationId: 'c1', status: 'TOOL_DENIED', usedTools: ['list_transactions'], evidenceIds: [],
      executionSummary: '<script>steal()</script>', pendingActions: [], taskPlan: plan
    }
    const view = render(ExplanationDrawer, { props: { open: true, result }, global: { plugins: [createPinia()] } })
    expect(view.container.querySelector('script')).toBeNull()
    expect(view.container.querySelector('img')).toBeNull()
    expect(view.getByText('<script>steal()</script>')).toBeTruthy()
  })

  it('shows failed plan reason instead of a fabricated success', () => {
    const view = render(TaskPlanPanel, { props: { plan } })
    expect(view.getByText(/工具不在本轮允许集合/)).toBeTruthy()
    expect(view.getByText('list_transactions')).toBeTruthy()
  })
})
