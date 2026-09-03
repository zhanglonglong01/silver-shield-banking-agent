<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ChartNoAxesCombined, Clock3, ShieldCheck } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import StatePanel from '@/components/StatePanel.vue'
import { apiRequest } from '@/services/http'
import type { WealthProduct } from '@/types/api'
import { money } from '@/utils/format'
const products = ref<WealthProduct[]>([]); const loading = ref(true); const error = ref('')
onMounted(async () => { try { products.value = await apiRequest<WealthProduct[]>('/api/v1/wealth-products') } catch (e) { error.value = e instanceof Error ? e.message : '读取失败' } finally { loading.value = false } })
</script>

<template><div class="page"><PageHeader eyebrow="SUITABILITY BEFORE ACTION" title="理财产品" description="预期收益不代表承诺；购买前必须完成适当性、余额与风险复核。" />
  <StatePanel :loading="loading" :error="error" :empty="!products.length"><section class="product-grid"><article v-for="product in products" :key="product.id" class="product-card"><header><span class="risk-level">{{ product.riskLevel }}</span><span>{{ product.productCode }}</span></header><h2>{{ product.name }}</h2><p>{{ product.productType }} · {{ product.currency }}</p><div class="yield"><strong>{{ product.expectedYieldText }}</strong><span>业绩比较基准</span></div><dl><div><dt><Clock3 />期限</dt><dd>{{ product.termDays }} 天</dd></div><div><dt><ChartNoAxesCombined />起购</dt><dd>{{ money(product.minPurchase) }}</dd></div><div><dt><ShieldCheck />销售状态</dt><dd>{{ product.saleStatus }}</dd></div></dl><RouterLink class="button ghost wide" :to="`/agent?prompt=评估并购买${encodeURIComponent(product.name)}，金额1000元`">让智能体评估</RouterLink></article></section></StatePanel></div></template>
