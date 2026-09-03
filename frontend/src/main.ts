import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { useAuthStore } from './stores/auth'
import './styles.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)

const auth = useAuthStore(pinia)
window.addEventListener('silver-shield:unauthorized', () => {
  auth.clear()
  void router.replace({ name: 'login' })
})
auth.bootstrap().finally(() => app.mount('#app'))
