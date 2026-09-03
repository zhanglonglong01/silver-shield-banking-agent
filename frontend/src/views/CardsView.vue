<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { CreditCard, Globe2, Nfc, Wifi } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import StatePanel from '@/components/StatePanel.vue'
import { apiRequest } from '@/services/http'
import type { Card } from '@/types/api'
import { money } from '@/utils/format'

const cards = ref<Card[]>([]); const loading = ref(true); const error = ref('')
onMounted(async () => { try { cards.value = await apiRequest<Card[]>('/api/v1/cards') } catch (e) { error.value = e instanceof Error ? e.message : '读取失败' } finally { loading.value = false } })
</script>

<template><div class="page"><PageHeader eyebrow="CARD CONTROL CENTER" title="卡片管理" description="状态与权限只读展示；任何变更必须经智能体准备并由你独立确认。" />
  <StatePanel :loading="loading" :error="error" :empty="!cards.length"><section class="cards-grid"><article v-for="card in cards" :key="card.id" class="bank-card" :class="card.cardType.toLowerCase()"><header><span>银盾银行 · MOCK</span><Nfc /></header><div class="chip"></div><h2>{{ card.cardMasked }}</h2><footer><span>{{ card.expiryMonth }}<small>VALID THRU</small></span><strong>{{ card.cardType }}</strong></footer></article>
  <article v-for="card in cards" :key="`${card.id}-controls`" class="control-card"><header><CreditCard /><div><h2>{{ card.cardMasked }}</h2><p><span class="status-dot"></span>{{ card.status }}</p></div><RouterLink :to="`/agent?prompt=管理银行卡${card.cardMasked}`">通过智能体管理</RouterLink></header><div class="control-list"><span><Wifi />线上支付<strong>{{ card.onlineEnabled ? '已开启' : '已关闭' }}</strong></span><span><Globe2 />境外支付<strong>{{ card.overseasEnabled ? '已开启' : '已关闭' }}</strong></span><span>单笔限额<strong>{{ money(card.singleTxLimit) }}</strong></span><span>每日限额<strong>{{ money(card.dailyLimit) }}</strong></span></div></article></section></StatePanel></div></template>
