<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, CheckCircle2, LockKeyhole, ShieldCheck } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const username = ref('demo.alice')
const password = ref('Demo@2026!')
const loading = ref(false)
const error = ref('')

function useDemo(user: 'customer' | 'admin') {
  username.value = user === 'customer' ? 'demo.alice' : 'demo.admin'
  password.value = user === 'customer' ? 'Demo@2026!' : 'Admin@2026!'
}

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    const target = typeof route.query.redirect === 'string' ? route.query.redirect : '/agent'
    await router.replace(target)
  } catch (reason) { error.value = reason instanceof Error ? reason.message : '登录失败' }
  finally { loading.value = false }
}
</script>

<template>
  <main class="login-page">
    <section class="login-story">
      <div class="brand large"><span class="brand-mark"><ShieldCheck /></span><span><strong>银盾智行</strong><small>安全可信银行智能体</small></span></div>
      <div class="story-copy">
        <p class="eyebrow">2026 FINTECHATHON · MOCK SANDBOX</p>
        <h1>让每一次智能决策<br /><em>有据可查</em></h1>
        <p>自然语言负责理解意图，确定性安全内核负责守住资金边界。</p>
      </div>
      <ul class="trust-list">
        <li><CheckCircle2 />动态 Tool 白名单</li>
        <li><CheckCircle2 />高风险操作独立确认</li>
        <li><CheckCircle2 />允许与拒绝均有证据</li>
      </ul>
    </section>
    <section class="login-panel">
      <form class="login-card" @submit.prevent="submit">
        <span class="login-icon"><LockKeyhole /></span>
        <p class="eyebrow">受保护的演示环境</p>
        <h2>登录银行沙箱</h2>
        <p class="muted">所有姓名、账户与交易均为固定种子生成的虚构数据。</p>
        <label>用户名<input v-model="username" autocomplete="username" required /></label>
        <label>密码<input v-model="password" type="password" autocomplete="current-password" required /></label>
        <p v-if="error" class="form-alert" role="alert">{{ error }}</p>
        <button class="button primary wide" :disabled="loading">{{ loading ? '正在验证…' : '安全登录' }}<ArrowRight :size="18" /></button>
        <div class="demo-users"><span>快速填入</span><button type="button" @click="useDemo('customer')">主演示客户</button><button type="button" @click="useDemo('admin')">沙箱管理员</button></div>
      </form>
    </section>
  </main>
</template>
