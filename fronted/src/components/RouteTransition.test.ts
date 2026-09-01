import { defineComponent, nextTick, onMounted, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import RouteTransition from './RouteTransition.vue'

describe('RouteTransition', () => {
  it('路径改变时会重新挂载复用的路由组件', async () => {
    const mounts = ref(0)
    const SharedView = defineComponent({
      setup() { onMounted(() => { mounts.value += 1 }) },
      template: '<section>管理模块</section>',
    })
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/admin/:module', component: SharedView }],
    })
    await router.push('/admin/users')
    await router.isReady()
    mount(RouteTransition, { global: { plugins: [router] } })
    await nextTick()
    expect(mounts.value).toBe(1)

    await router.push('/admin/whitelist')
    await nextTick()
    expect(mounts.value).toBe(2)
  })

  it('保留路由配置传入的页面参数', async () => {
    const RoutedView = defineComponent({
      props: { module: { type: String, required: true } },
      template: '<section>{{ module }}</section>',
    })
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/admin/:module', component: RoutedView, props: true }],
    })
    await router.push('/admin/whitelist')
    await router.isReady()
    const wrapper = mount(RouteTransition, { global: { plugins: [router] } })

    expect(wrapper.text()).toContain('whitelist')
  })

  it('页面有多个根节点时仍完整渲染标题与内容', async () => {
    const FragmentView = defineComponent({ template: '<h1>标题</h1><section>实际内容</section>' })
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/student/profile', component: FragmentView }],
    })
    await router.push('/student/profile')
    await router.isReady()
    const wrapper = mount(RouteTransition, { global: { plugins: [router] } })

    expect(wrapper.find('.route-transition-shell').exists()).toBe(true)
    expect(wrapper.text()).toContain('标题')
    expect(wrapper.text()).toContain('实际内容')
  })
})
