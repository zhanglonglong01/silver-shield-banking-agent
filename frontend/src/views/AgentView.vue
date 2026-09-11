<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bot, ChevronRight, CirclePlus, Info, Send, ShieldCheck, User } from 'lucide-vue-next'
import ConfirmationCard from '@/components/ConfirmationCard.vue'
import ExplanationDrawer from '@/components/ExplanationDrawer.vue'
import PageHeader from '@/components/PageHeader.vue'
import TaskPlanPanel from '@/components/TaskPlanPanel.vue'
import { useAgentStore } from '@/stores/agent'
import { registerAgentWebMcp } from '@/services/webmcp'

const agent = useAgentStore()
const route = useRoute()
const router = useRouter()
const input = ref('')
const explainOpen = ref(false)
const chatEnd = ref<HTMLElement | null>(null)
const scenarios = [
  { label: '智能转账', prompt: '从日常账户给张伟转账500元' },
  { label: '账单分析', prompt: '分析尾号8899账户最近30天的账单、收支与大额交易' },
  { label: '理财操作', prompt: '用日常账户购买银盾稳享存款理财产品1000元' },
  { label: '卡片管理', prompt: '冻结我的尾号1001银行卡' },
  { label: '订阅管理', prompt: '取消我的星云视频订阅' },
  { label: '跨场景联动', prompt: '分析尾号8899账户最近30天账单，然后取消视频订阅' }
]

const webMcpLifecycle = new AbortController()
onMounted(() => {
  if (typeof route.query.prompt === 'string') input.value = route.query.prompt
  registerAgentWebMcp(message => agent.send(message), webMcpLifecycle.signal)
})
onUnmounted(() => webMcpLifecycle.abort())
watch(() => agent.messages.length, async () => { await nextTick(); chatEnd.value?.scrollIntoView({ behavior: 'smooth' }) })

async function send(message = input.value) {
  input.value = ''
  if (route.query.prompt) await router.replace({ query: {} })
  await agent.send(message)
}
</script>

<template>
  <div class="page agent-page">
    <PageHeader eyebrow="SECURE AGENT CONSOLE" title="智能体工作台" description="每轮动态授权、每次调用重新校验、每项结果关联证据。">
      <button class="button ghost" @click="agent.resetConversation"><CirclePlus :size="17" />新对话</button>
      <button class="button secondary" data-testid="explain-button" @click="explainOpen = true"><ShieldCheck :size="17" />查看安全解释</button>
    </PageHeader>

    <div class="agent-grid">
      <section class="chat-card">
        <div class="scenario-strip" aria-label="六类演示场景">
          <button v-for="scenario in scenarios" :key="scenario.label" @click="send(scenario.prompt)">{{ scenario.label }}<ChevronRight :size="14" /></button>
        </div>
        <div class="messages" aria-live="polite">
          <article v-for="message in agent.messages" :key="message.id" class="message" :class="message.role">
            <span class="avatar"><User v-if="message.role === 'user'" :size="16" /><Bot v-else :size="16" /></span>
            <div><span class="message-role">{{ message.role === 'user' ? '你' : message.role === 'system' ? '安全执行链' : '银盾智行' }}<em v-if="message.status">{{ message.status }}</em></span><p>{{ message.content }}</p></div>
          </article>
          <article v-if="agent.loading" class="message assistant"><span class="avatar"><Bot :size="16" /></span><div class="thinking"><i></i><i></i><i></i><span>正在计算本轮 Capability…</span></div></article>
          <div ref="chatEnd"></div>
        </div>
        <form class="composer" @submit.prevent="send()">
          <textarea v-model="input" maxlength="4000" rows="2" aria-label="向银行智能体发送消息" placeholder="描述你的银行任务，例如：分析本月账单，然后找出可以取消的订阅…" @keydown.enter.exact.prevent="send()"></textarea>
          <button class="send-button" :disabled="agent.loading || !input.trim()" aria-label="发送"><Send :size="19" /></button>
          <small><Info :size="13" />模型无权直接修改银行数据；确认按钮调用独立安全接口。</small>
        </form>
      </section>

      <aside class="agent-rail">
        <div v-if="!agent.lastResult" class="rail-placeholder"><ShieldCheck /><h2>安全执行状态</h2><p>发起任务后，这里会展示计划、风险提示和独立确认卡。</p></div>
        <template v-else>
          <div class="result-banner" :class="agent.lastResult.status === 'SUCCESS' ? 'success' : 'warning'">
            <span>本轮结果</span><strong>{{ agent.lastResult.status }}</strong><p>{{ agent.lastResult.executionSummary }}</p>
          </div>
          <TaskPlanPanel v-if="agent.lastResult.taskPlan" :plan="agent.lastResult.taskPlan" />
          <ConfirmationCard v-for="action in agent.lastResult.pendingActions" :key="action.operationId" :action="action" :loading="agent.decisionLoading" @decide="agent.decide(action, $event)" />
          <p v-if="agent.decisionError" class="form-alert" role="alert">{{ agent.decisionError }}</p>
          <section v-if="agent.operationResult" class="operation-result"><span class="section-kicker">数据库提交结果</span><pre>{{ JSON.stringify(agent.operationResult, null, 2) }}</pre></section>
        </template>
      </aside>
    </div>
    <ExplanationDrawer :open="explainOpen" :result="agent.lastResult" @close="explainOpen = false" />
  </div>
</template>
