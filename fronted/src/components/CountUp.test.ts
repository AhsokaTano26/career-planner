import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import CountUp from './CountUp.vue'

afterEach(() => vi.unstubAllGlobals())

describe('CountUp', () => {
  it('系统要求减弱动态时直接显示目标数字', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({
      matches: true,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })))

    const wrapper = mount(CountUp, { props: { to: 42 } })

    expect(wrapper.text()).toBe('42')
  })
})
