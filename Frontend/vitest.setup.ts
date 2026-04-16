import { vi, beforeEach } from 'vitest'

// Create a mock localStorage that works reliably
const createLocalStorageMock = () => {
  const store: Record<string, string> = {}

  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = String(value)
    },
    removeItem: (key: string) => {
      delete store[key]
    },
    clear: () => {
      Object.keys(store).forEach(key => {
        delete store[key]
      })
    },
    key: (index: number) => {
      const keys = Object.keys(store)
      return keys[index] || null
    },
    length: Object.keys(store).length,
  }
}

// Override localStorage globally
Object.defineProperty(window, 'localStorage', {
  value: createLocalStorageMock(),
  writable: true,
})

// Reset localStorage before each test
beforeEach(() => {
  window.localStorage.clear()
})



