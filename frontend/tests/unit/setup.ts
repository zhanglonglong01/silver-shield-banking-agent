import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/vue'

afterEach(() => {
  cleanup()
  sessionStorage.clear()
  vi.restoreAllMocks()
})
