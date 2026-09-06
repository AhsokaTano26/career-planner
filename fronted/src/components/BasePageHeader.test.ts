import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BasePageHeader from './BasePageHeader.vue'

describe('BasePageHeader', () => {
  it('exposes slotted controls as a labelled page action region', () => {
    const wrapper = mount(BasePageHeader, {
      props: { eyebrow: '个人档案', title: '档案概览', description: '查看当前资料完成情况。' },
      slots: { actions: '<button type="button">编辑资料</button>' },
    })

    expect(wrapper.get('[data-testid="page-actions"]').attributes('aria-label')).toBe('页面操作')
    expect(wrapper.get('[data-testid="page-actions"]').text()).toContain('编辑资料')
  })
})
