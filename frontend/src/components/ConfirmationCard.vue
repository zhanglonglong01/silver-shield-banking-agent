<script setup lang="ts">
import { computed, ref } from 'vue'
import { AlertTriangle, KeyRound, LockKeyhole } from 'lucide-vue-next'
import type { PendingAction } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{ action: PendingAction; loading: boolean }>()
const emit = defineEmits<{ decide: [confirmed: boolean] }>()
const auth = useAuthStore()
const code = ref('')
const stepUpError = ref('')
const steppingUp = ref(false)
const needsAal2 = computed(() => props.action.requiredAal === 'AAL2' && auth.assuranceLevel !== 'AAL2')

async function stepUp() {
  stepUpError.value = ''
  steppingUp.value = true
  try { await auth.stepUp(code.value); code.value = '' }
  catch (error) { stepUpError.value = error instanceof Error ? error.message : '验证失败' }
  finally { steppingUp.value = false }
}
</script>

<template>
  <section class="confirmation-card" data-testid="confirmation-card">
    <header><span><AlertTriangle :size="20" />需要独立确认</span><span class="risk-badge">{{ action.requiredAal }}</span></header>
    <p>以下摘要由后端安全执行链生成并绑定挑战。确认前数据库不会发生业务变更。</p>
    <dl class="summary-grid">
      <template v-for="(value, key) in action.summary" :key="key">
        <dt>{{ key }}</dt><dd>{{ typeof value === 'object' ? JSON.stringify(value) : value }}</dd>
      </template>
    </dl>
    <div v-if="needsAal2" class="step-up-box">
      <div><KeyRound :size="18" /><span><strong>需要升级至 AAL2</strong><small>演示环境验证码：246810</small></span></div>
      <form @submit.prevent="stepUp"><input v-model="code" aria-label="AAL2 验证码" maxlength="12" placeholder="输入验证码" /><button :disabled="steppingUp || code.length < 6">验证</button></form>
      <p v-if="stepUpError" class="field-error">{{ stepUpError }}</p>
    </div>
    <div class="confirm-meta"><LockKeyhole :size="14" />操作单 {{ action.operationId.slice(0, 8) }} · {{ action.status }}</div>
    <footer>
      <button class="button ghost" :disabled="loading" @click="emit('decide', false)">拒绝操作</button>
      <button class="button danger" :disabled="loading || needsAal2" @click="emit('decide', true)">确认并提交</button>
    </footer>
  </section>
</template>
