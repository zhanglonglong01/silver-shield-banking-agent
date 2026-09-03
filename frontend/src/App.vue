<script setup lang="ts">
import { RouterView, useRoute, useRouter } from 'vue-router'
import { Bot, CreditCard, Landmark, LogOut, ReceiptText, ShieldCheck, Sparkles } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const nav = [
  { to: '/agent', label: '智能体工作台', icon: Bot },
  { to: '/accounts', label: '账户与账单', icon: Landmark },
  { to: '/cards', label: '卡片管理', icon: CreditCard },
  { to: '/subscriptions', label: '订阅与代扣', icon: ReceiptText },
  { to: '/wealth', label: '理财产品', icon: Sparkles },
  { to: '/security', label: '安全与沙箱', icon: ShieldCheck }
]

async function logout() {
  await auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <RouterView v-if="route.meta.public" />
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark"><ShieldCheck :size="22" /></span>
        <span><strong>银盾智行</strong><small>AI Banking Agent</small></span>
      </div>
      <nav aria-label="主导航">
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to" class="nav-link">
          <component :is="item.icon" :size="18" />{{ item.label }}
        </RouterLink>
      </nav>
      <div class="sidebar-foot">
        <div class="mock-label">仅限 Mock Sandbox</div>
        <button class="user-button" type="button" @click="logout">
          <span><strong>{{ auth.user?.displayName }}</strong><small>{{ auth.user?.username }}</small></span>
          <LogOut :size="17" aria-label="退出登录" />
        </button>
      </div>
    </aside>
    <main class="main-content">
      <header class="topbar">
        <div><span class="environment-dot"></span> 银行沙箱在线</div>
        <div class="assurance-badge" :class="auth.assuranceLevel.toLowerCase()">
          <ShieldCheck :size="15" />{{ auth.assuranceLevel }} 会话
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>
