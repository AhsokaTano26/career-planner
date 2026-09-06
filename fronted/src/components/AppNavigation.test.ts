import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AppNavigation from './AppNavigation.vue'

const groups = [{ title: '个人档案', links: [['/student/overview', '档案概览'], ['/student/profile', '个人资料']] as [string, string][] }]

describe('AppNavigation', () => {
  it('marks the active route, emits navigation, and opens the compact menu', async () => {
    const wrapper = mount(AppNavigation, {
      props: { groups, activePath: '/student/overview', userName: '张同学', workspaceName: '学生工作台' },
    })

    expect(wrapper.get('[aria-current="page"]').text()).toContain('档案概览')
    await wrapper.get('[data-testid="mobile-nav-trigger"]').trigger('click')
    expect(wrapper.get('[data-testid="mobile-nav-trigger"]').attributes('aria-expanded')).toBe('true')
    await wrapper.get('button[data-path="/student/profile"]').trigger('click')
    expect(wrapper.emitted('navigate')).toEqual([['/student/profile']])
  })
})
