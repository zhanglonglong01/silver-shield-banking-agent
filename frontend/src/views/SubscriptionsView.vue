<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CalendarClock, ReceiptText } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import StatePanel from '@/components/StatePanel.vue'
import { apiRequest } from '@/services/http'
import type { Subscription } from '@/types/api'
import { money } from '@/utils/format'
const items = ref<Subscription[]>([]); const loading = ref(true); const error = ref('')
const monthly = computed(() => items.value.filter(x => x.status === 'ACTIVE').reduce((sum, x) => sum + Number(x.amount) / (x.billingCycle === 'YEARLY' ? 12 : 1), 0))
onMounted(async () => { try { items.value = await apiRequest<Subscription[]>('/api/v1/subscriptions') } catch (e) { error.value = e instanceof Error ? e.message : '读取失败' } finally { loading.value = false } })
</script>

<template><div class="page"><PageHeader eyebrow="RECURRING PAYMENT VISIBILITY" title="订阅与代扣" description="先看清周期成本和取消影响，再由安全执行链处理变更。"><RouterLink class="button secondary" to="/agent?prompt=分析我的订阅成本并找出可以取消的订阅">让智能体分析</RouterLink></PageHeader>
  <StatePanel :loading="loading" :error="error" :empty="!items.length"><section class="metric-row"><article><span>活跃订阅</span><strong>{{ items.filter(x => x.status === 'ACTIVE').length }}</strong><small>项固定沙箱服务</small></article><article><span>折算月成本</span><strong>{{ money(monthly) }}</strong><small>由前端仅作展示汇总</small></article><article><span>数据基准日</span><strong>2026.08.31</strong><small>可复现数据集</small></article></section>
  <section class="data-card subscription-list"><header><div><span class="section-kicker">MANDATES</span><h2>全部订阅</h2></div><ReceiptText /></header><article v-for="item in items" :key="item.id"><span class="service-icon">{{ item.serviceName.slice(0, 1) }}</span><div><h3>{{ item.serviceName }}<span>{{ item.status }}</span></h3><p>{{ item.merchantName }} · {{ item.billingCycle }}</p></div><div><strong>{{ money(item.amount) }}</strong><small><CalendarClock :size="13" />{{ item.nextChargeDate }}</small></div><RouterLink :to="`/agent?prompt=取消我的${encodeURIComponent(item.serviceName)}订阅`">审查取消</RouterLink></article></section></StatePanel></div></template>
