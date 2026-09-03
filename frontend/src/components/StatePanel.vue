<script setup lang="ts">
import { AlertTriangle, LoaderCircle } from 'lucide-vue-next'
defineProps<{ loading?: boolean; error?: string | null; empty?: boolean; emptyText?: string }>()
</script>

<template>
  <div v-if="loading" class="state-panel"><LoaderCircle class="spin" />正在读取安全沙箱…</div>
  <div v-else-if="error" class="state-panel error"><AlertTriangle />{{ error }}</div>
  <div v-else-if="empty" class="state-panel">{{ emptyText ?? '暂无数据' }}</div>
  <slot v-else />
</template>
