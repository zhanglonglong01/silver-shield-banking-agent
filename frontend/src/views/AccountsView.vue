<script setup lang="ts">
import { onMounted, ref } from 'vue'
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

async function load() {
  loading.value = true; error.value = ''
  try {
    accounts.value = await apiRequest<Account[]>('/api/v1/accounts')
    selected.value = accounts.value[0]?.id ?? ''
    if (selected.value) await loadTransactions()
  } catch (reason) { error.value = reason instanceof Error ? reason.message : '读取失败' }
  finally { loading.value = false }
}

async function loadTransactions() {
  if (!selected.value) return
  transactions.value = await apiRequest<Transaction[]>(`/api/v1/accounts/${encodeURIComponent(selected.value)}/transactions?from=2026-08-01&to=2026-08-31&limit=20`)
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
        <header><div><span class="section-kicker">FIXED BASELINE · 2026-08</span><h2>最近交易</h2></div><Landmark /></header>
        <div class="table-wrap"><table><thead><tr><th>时间 / 摘要</th><th>分类</th><th>状态</th><th>金额</th><th>余额</th></tr></thead>
          <tbody><tr v-for="item in transactions" :key="item.id"><td><span class="tx-title"><component :is="item.direction === 'IN' ? ArrowDownLeft : ArrowUpRight" :size="16" />{{ item.merchantName || item.description }}</span><small>{{ dateTime(item.bookingTime) }}</small></td><td>{{ item.categoryCode }}</td><td><span class="status-pill">{{ item.status }}</span></td><td :class="item.direction === 'IN' ? 'positive' : 'negative'">{{ item.direction === 'IN' ? '+' : '-' }}{{ money(item.amount) }}</td><td>{{ money(item.balanceAfter) }}</td></tr></tbody>
        </table></div>
      </section>
    </StatePanel>
  </div>
</template>
