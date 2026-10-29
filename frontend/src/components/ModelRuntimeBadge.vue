<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { apiRequest } from '@/services/http'

interface RuntimeInfo { mode: string; provider: string; model: string; configured: boolean }
const info = ref<RuntimeInfo | null>(null)
const failed = ref(false)
const label = computed(() => {
  if (failed.value) return '模型配置暂不可读'
  if (!info.value) return '读取模型配置…'
  if (info.value.mode === 'OFFLINE_DEMO') return '离线演示模式'
  return info.value.configured ? `${info.value.provider} · ${info.value.model}` : '模型尚未配置'
})
onMounted(async () => {
  try {
    const result = await apiRequest<RuntimeInfo>('/api/v1/agent/runtime')
    if (!['LIVE_MODEL', 'OFFLINE_DEMO', 'UNAVAILABLE'].includes(result?.mode)
        || typeof result?.configured !== 'boolean') throw new Error('Invalid model metadata')
    info.value = result
  } catch { failed.value = true }
})
</script>

<template>
  <span class="model-runtime" data-testid="model-runtime" title="当前服务端模型配置；不是实时连通性检测">{{ label }}</span>
</template>

<style scoped>
.model-runtime { padding: 6px 11px; border: 1px solid var(--line); border-radius: 6px; background: var(--paper); color: var(--teal-dark); font-size: 12px; }
@media (max-width: 760px) { .model-runtime { max-width: 45vw; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 10px; } }
</style>
