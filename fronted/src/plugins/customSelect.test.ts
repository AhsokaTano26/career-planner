import { afterEach, describe, expect, it } from 'vitest'
import { enhanceSelect } from './customSelect'

afterEach(() => { document.body.replaceChildren() })

describe('enhanceSelect', () => {
  it('支持键盘选择，并能清理创建的监听和界面', () => {
    document.body.innerHTML = '<select><option value="a">甲</option><option value="b">乙</option></select>'
    const select = document.querySelector('select')!
    const cleanup = enhanceSelect(select)
    const root = document.querySelector<HTMLElement>('.native-select-renderer')!
    const trigger = root.querySelector<HTMLButtonElement>('.native-select-trigger')!

    expect(trigger.getAttribute('aria-expanded')).toBe('false')
    trigger.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }))
    expect(trigger.getAttribute('aria-expanded')).toBe('true')
    trigger.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }))
    expect(select.value).toBe('b')
    trigger.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    expect(trigger.getAttribute('aria-expanded')).toBe('false')

    cleanup()
    expect(document.querySelector('.native-select-renderer')).toBeNull()
    expect(select.classList.contains('native-select-source')).toBe(false)
  })
})
