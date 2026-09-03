import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/agent' },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    { path: '/agent', name: 'agent', component: () => import('@/views/AgentView.vue') },
    { path: '/accounts', name: 'accounts', component: () => import('@/views/AccountsView.vue') },
    { path: '/cards', name: 'cards', component: () => import('@/views/CardsView.vue') },
    { path: '/subscriptions', name: 'subscriptions', component: () => import('@/views/SubscriptionsView.vue') },
    { path: '/wealth', name: 'wealth', component: () => import('@/views/WealthView.vue') },
    { path: '/security', name: 'security', component: () => import('@/views/SecurityView.vue') }
  ]
})

router.beforeEach(to => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.authenticated) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.name === 'login' && auth.authenticated) return { name: 'agent' }
})
