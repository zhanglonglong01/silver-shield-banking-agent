import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  server: {
    port: 5173,
    proxy: { '/api': 'http://127.0.0.1:8080', '/actuator': 'http://127.0.0.1:8080' }
  },
  test: {
    environment: 'happy-dom',
    setupFiles: ['./tests/unit/setup.ts'],
    include: ['./tests/unit/**/*.test.ts']
  }
})
