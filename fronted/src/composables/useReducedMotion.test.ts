import { effectScope, nextTick } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useReducedMotion } from './useReducedMotion'

type Listener = (event: MediaQueryListEvent) => void

function mockPreference(initial: boolean) {
  const listeners = new Set<Listener>()
  const media = {
    matches: initial,
    media: '(prefers-reduced-motion: reduce)',
    onchange: null,
    addEventListener: vi.fn((_: 'change', listener: Listener) => listeners.add(listener)),
    removeEventListener: vi.fn((_: 'change', listener: Listener) => listeners.delete(listener)),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }
  vi.stubGlobal('matchMedia', vi.fn(() => media))
  return {
    media,
    change(next: boolean) {
      media.matches = next
      listeners.forEach(listener => listener({ matches: next } as MediaQueryListEvent))
    },
  }
}

afterEach(() => vi.unstubAllGlobals())

describe('useReducedMotion', () => {
  it('读取并响应系统减弱动态偏好', async () => {
    const preference = mockPreference(true)
    const scope = effectScope()
    const reduced = scope.run(() => useReducedMotion())!

    expect(reduced.value).toBe(true)
    preference.change(false)
    await nextTick()
    expect(reduced.value).toBe(false)

    scope.stop()
    expect(preference.media.removeEventListener).toHaveBeenCalledTimes(1)
  })
})
