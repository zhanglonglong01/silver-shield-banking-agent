<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowDownLeft, ArrowUpRight, Landmark, WalletCards } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import StatePanel from '@/components/StatePanel.vue'
import { apiRequest } from '@/services/http'
import type { Account, Transaction } from '@/types/api'
import { dateTime, money } from '@/utils/format'

const accounts = ref<Account[]>([])
const transactions = ref<Transaction[]>([])
const selected = ref('')
const loading = ref(true)
const error = ref('')
const period = ref('recent')
const transactionLoading = ref(false)
const transactionError = ref('')
const transactionRange = ref({ from: '', to: '' })
const periodLabel = computed(() => period.value === 'baseline' ? '基线账单' : '最近交易')
let transactionRequest = 0

async function load() {
  loading.value = true; error.value = ''
  try {
    accounts.value = await apiRequest<Account[]>('/api/v1/accounts')
    if (!accounts.value.some(account => account.id === selected.value)) {
      selected.value = accounts.value[0]?.id ?? ''
    }
    if (selected.value) await loadTransactions()
  } catch (reason) { error.value = reason instanceof Error ? reason.message : '读取失败' }
  finally { loading.value = false }
}

async function loadTransactions() {
  if (!selected.value) return
  const request = ++transactionRequest
  transactionLoading.value = true
  transactionError.value = ''
  transactions.value = []
  const today = new Date()
  const from = new Date(today)
  from.setUTCDate(from.getUTCDate() - 365)
  const range = period.value === 'baseline'
    ? { from: '2026-08-01', to: '2026-08-31' }
    : { from: from.toISOString().slice(0, 10), to: today.toISOString().slice(0, 10) }
  transactionRange.value = range
  try {
    const rows = await apiRequest<Transaction[]>(`/api/v1/accounts/${encodeURIComponent(selected.value)}/transactions?from=${range.from}&to=${range.to}&limit=20`)
    if (request === transactionRequest) transactions.value = rows
  } catch (reason) {
    if (request === transactionRequest) transactionError.value = reason instanceof Error ? reason.message : '读取交易失败'
  } finally {
    if (request === transactionRequest) transactionLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader eyebrow="OWNERSHIP-SCOPED RESOURCES" title="账户与账单" description="只返回当前登录用户拥有的账户；越权资源统一表现为不可用。" />
    <StatePanel :loading="loading" :error="error" :empty="!accounts.length">
      <section class="account-grid">
        <article v-for="account in accounts" :key="account.id" class="account-card" :class="{ selected: selected === account.id }" @click="selected = account.id; loadTransactions()">
          <span class="account-icon"><WalletCards /></span><span class="status-dot">{{ account.status }}</span>
          <h2>{{ account.alias }}</h2><p>{{ account.accountMasked }} · {{ account.accountType }}</p>
          <strong>{{ money(account.availableBalance, account.currency) }}</strong><small>可用余额 · 日限额 {{ money(account.dailyTransferLimit) }}</small>
        </article>
      </section>
      <section class="data-card">
        <header><div><span class="section-kicker">{{ transactionRange.from }} — {{ transactionRange.to }} · 最新 20 笔</span><h2>{{ periodLabel }}</h2></div><Landmark /></header>
        <div class="transaction-toolbar">
          <label>交易范围 <select v-model="period" aria-label="交易范围" @change="loadTransactions"><option value="recent">最近一年（包含今天）</option><option value="baseline">2026 年 8 月基线账单</option></select></label>
          <button class="button ghost" :disabled="loading || transactionLoading" @click="load">刷新余额与交易</button>
        </div>
        <StatePanel :loading="transactionLoading" :error="transactionError" :empty="!transactions.length" empty-text="所选账户在此期间暂无交易">
        <div class="table-wrap"><table><thead><tr><th>时间 / 摘要</th><th>分类</th><th>状态</th><th>金额</th><th>余额</th></tr></thead>
          <tbody><tr v-for="item in transactions" :key="item.id"><td><span class="tx-title"><component :is="item.direction === 'IN' ? ArrowDownLeft : ArrowUpRight" :size="16" />{{ item.merchantName || item.description }}</span><small>{{ dateTime(item.bookingTime) }}</small></td><td>{{ item.categoryCode }}</td><td><span class="status-pill">{{ item.status }}</span></td><td :class="item.direction === 'IN' ? 'positive' : 'negative'">{{ item.direction === 'IN' ? '+' : '-' }}{{ money(item.amount) }}</td><td>{{ money(item.balanceAfter) }}</td></tr></tbody>
        </table></div>
        </StatePanel>
      </section>
    </StatePanel>
  </div>
</template>

<style scoped>
.transaction-toolbar { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; margin: 16px 0; }
.transaction-toolbar label { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.transaction-toolbar select { max-width: 100%; padding: 8px; border: 1px solid var(--line); border-radius: 7px; background: var(--paper); color: var(--ink); font: inherit; }
</style>
