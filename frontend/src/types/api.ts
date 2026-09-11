export type AssuranceLevel = 'AAL1' | 'AAL2'

export interface CurrentUser {
  id: string
  username: string
  displayName: string
  role: 'CUSTOMER' | 'ADMIN'
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
  assuranceLevel: AssuranceLevel
  user: CurrentUser
}

export interface MeResponse {
  user: CurrentUser
  assuranceLevel: AssuranceLevel
  aal2Available: boolean
}

export interface Account {
  id: string
  alias: string
  accountMasked: string
  accountType: string
  currency: string
  ledgerBalance: string
  availableBalance: string
  status: string
  dailyTransferLimit: string
}

export interface Transaction {
  id: string
  bookingTime: string
  direction: 'IN' | 'OUT'
  amount: string
  currency: string
  categoryCode: string
  merchantName: string | null
  description: string
  counterpartyMasked: string | null
  status: string
  balanceAfter: string
}

export interface Card {
  id: string
  linkedAccountId: string
  cardMasked: string
  cardType: string
  status: string
  onlineEnabled: boolean
  overseasEnabled: boolean
  singleTxLimit: string
  dailyLimit: string
  expiryMonth: string
}

export interface Subscription {
  id: string
  chargeAccountId: string
  merchantName: string
  serviceName: string
  amount: string
  currency: string
  billingCycle: string
  nextChargeDate: string
  status: string
  cancellationEffect: string
}

export interface WealthProduct {
  id: string
  productCode: string
  name: string
  productType: string
  riskLevel: string
  currency: string
  minPurchase: string
  incrementAmount: string
  expectedYieldText: string
  termDays: number
  saleStatus: string
  validFrom: string
  validTo: string
}

export type AgentStatus =
  | 'SUCCESS' | 'CAPABILITY_UNAVAILABLE' | 'TOOL_DENIED' | 'PROVIDER_UNAVAILABLE'
  | 'PROVIDER_TIMEOUT' | 'TOOL_TIMEOUT' | 'MALFORMED_MODEL_RESPONSE'
  | 'REPEATED_TOOL_CALL' | 'LIMIT_EXCEEDED' | 'CONTEXT_LIMIT_EXCEEDED'
  | 'GROUNDING_FAILED' | 'NEEDS_INPUT' | 'PLAN_PAUSED' | 'PLAN_REJECTED' | 'PLAN_CANCELLED'

export interface ToolId { name: string; version: string }

export interface PlanStep {
  stepId: string
  tool: ToolId
  accessMode: 'READ_ONLY' | 'PREPARE_WRITE'
  dependsOn: string[]
  status: 'PENDING' | 'COMPLETED' | 'FAILED'
  evidenceIds: string[]
  reasonCode: string | null
}

export interface TaskPlan {
  planId: string
  conversationId: string
  goal: string
  status: string
  steps: PlanStep[]
  missingSlots: string[]
  statusReason: string
  updatedAt: string
}

export interface PendingAction {
  operationId: string
  status: string
  requiredAal: AssuranceLevel
  expiresAt: string
  challengeId: string
  confirmEndpoint: string
  summary: Record<string, unknown>
}

export interface AgentChatResult {
  answer: string
  conversationId: string
  status: AgentStatus
  usedTools: string[]
  evidenceIds: string[]
  executionSummary: string
  pendingActions: PendingAction[]
  taskPlan: TaskPlan | null
}

export interface ApiErrorBody { code: string; message: string; timestamp: string }
