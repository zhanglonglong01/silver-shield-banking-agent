<script setup lang="ts">
import { LockKeyhole, X } from 'lucide-vue-next'
import type { AgentChatResult } from '@/types/api'
import { reasonLabel } from '@/utils/format'
defineProps<{ open: boolean; result: AgentChatResult | null }>()
defineEmits<{ close: [] }>()
</script>

<template>
  <div v-if="open" class="drawer-backdrop" @click.self="$emit('close')">
    <aside class="explanation-drawer" aria-label="安全执行解释" data-testid="explanation-drawer">
      <header><div><span class="section-kicker">可解释安全决策</span><h2>为什么允许 / 拒绝</h2></div><button class="icon-button" aria-label="关闭" @click="$emit('close')"><X /></button></header>
      <template v-if="result">
        <section><h3>最终状态</h3><div class="decision-box" :class="result.status === 'SUCCESS' ? 'allow' : 'deny'"><strong>{{ result.status }}</strong><span>{{ result.executionSummary }}</span></div></section>
        <section><h3>策略原因</h3><p>{{ reasonLabel(result.taskPlan?.statusReason ?? result.status) }}</p><code>{{ result.taskPlan?.statusReason ?? result.status }}</code></section>
        <section><h3>实际调用的白名单 Tool</h3><div class="tag-list"><span v-for="tool in result.usedTools" :key="tool">{{ tool }}</span><em v-if="!result.usedTools.length">无 Tool 被执行</em></div></section>
        <section><h3>证据链 ID</h3><ul class="evidence-list"><li v-for="id in result.evidenceIds" :key="id">{{ id }}</li><li v-if="!result.evidenceIds.length">没有生成执行证据</li></ul></section>
        <section class="boundary-note"><LockKeyhole :size="18" /><p><strong>模型不是执行边界</strong><br />Tool 名称、参数、资源归属、权限与风险均由服务端再次校验。</p></section>
      </template>
      <p v-else>完成一次智能体请求后，可在这里查看原因码、实际 Tool 和证据 ID。</p>
    </aside>
  </div>
</template>
