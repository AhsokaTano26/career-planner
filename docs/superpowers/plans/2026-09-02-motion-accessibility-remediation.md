# 动效与无障碍修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让全部动效在正常模式下完整触发，并在系统“减弱动态效果”下消除位移、缩放、扫光和数字滚动。

**Architecture:** 将系统动态偏好抽为一个可测试的组合式函数，`CountUp` 使用它决定是否直接显示目标值；CSS 为减弱动态提供仅透明度的覆盖规则。路由视图使用路径 key 保障复用的管理端模块也会重新触发过渡；原生下拉抽为可销毁的增强器，保证键盘和生命周期正确。

**Tech Stack:** Vue 3、Vue Router 4、TypeScript、Vitest、手写 CSS、motion-v。

**Spec:** `docs/superpowers/specs/2026-09-02-frontend-ux-animation-design.md`

## Global Constraints

- 正常模式：路由 240ms、下拉 120ms、Toast 220ms、数字 0.8s；位移不超过 8px。
- 减弱动态：只允许最长 80ms 的透明度过渡，禁止 transform、width、错峰、扫光和 JS 数字滚动。
- 不改变接口、认证或数据行为。
- 前端验证命令在 `fronted/` 执行。

---

### Task 1: 建立可测试的动态偏好基础

**Files:**
- Create: `fronted/src/composables/useReducedMotion.ts`
- Create: `fronted/src/composables/useReducedMotion.test.ts`
- Modify: `fronted/package.json`

**Interfaces:**
- Produces: `useReducedMotion(): Readonly<Ref<boolean>>`，基于 `(prefers-reduced-motion: reduce)` 并响应运行时变化。

- [ ] **Step 1: 写失败测试**

```ts
it('读取并响应系统减弱动态偏好', () => {
  const preference = useReducedMotion()
  expect(preference.value).toBe(true)
  mediaQuery.dispatch(false)
  expect(preference.value).toBe(false)
})
```

- [ ] **Step 2: 运行测试，确认因模块尚不存在而失败**

Run: `npm run test -- useReducedMotion.test.ts`

- [ ] **Step 3: 实现媒体查询监听，并在组件卸载时移除监听**

```ts
export function useReducedMotion() {
  const reduced = ref(window.matchMedia('(prefers-reduced-motion: reduce)').matches)
  const update = (event: MediaQueryListEvent) => { reduced.value = event.matches }
  window.matchMedia('(prefers-reduced-motion: reduce)').addEventListener('change', update)
  onScopeDispose(() => media.removeEventListener('change', update))
  return readonly(reduced)
}
```

- [ ] **Step 4: 运行测试确认通过，并运行 `npm run build`**

### Task 2: 修复减弱动态与数字滚动

**Files:**
- Modify: `fronted/src/components/CountUp.vue`
- Modify: `fronted/src/assets/styles/motion.css`

**Interfaces:**
- Consumes: `useReducedMotion()`。
- Produces: 当系统偏好为 reduce 时，`CountUp` 同步输出目标值；所有 CSS 动效只有透明度短过渡。

- [ ] **Step 1: 扩充失败测试，验证 reduce 状态下数字不启动动画**

```ts
it('减弱动态时直接显示目标数字', async () => {
  render(CountUp, { props: { to: 42 } })
  expect(screen.getByText('42')).toBeTruthy()
})
```

- [ ] **Step 2: 运行测试，确认当前实现显示初始 0 而失败**

- [ ] **Step 3: 在 `CountUp` 监听偏好，reduce 时停止控制器并同步赋值；在 CSS reduce 媒体查询中覆盖所有进入/离开 transform、grow/item-in/shimmer 动画**

```css
@media (prefers-reduced-motion: reduce) {
  .page-enter-from, .page-leave-to, .modal-enter-from .modal-card,
  .modal-leave-to .modal-card, .dropdown-enter-from, .dropdown-leave-to,
  .toast-enter-from, .toast-leave-to { transform: none; }
  .progress-line span, .weight-bars i b, .analysis-bars em, .path-bars em,
  .dimension-list em, .enhanced-list article, .record-list article,
  .advisor-student-table article, .attention-list article, .skeleton::after {
    animation: none !important;
  }
}
```

- [ ] **Step 4: 运行单元测试与构建确认通过**

### Task 3: 可靠触发所有工作台路由过渡

**Files:**
- Modify: `fronted/src/layouts/DefaultLayout.vue`
- Modify: `fronted/src/layouts/AdminLayout.vue`

**Interfaces:**
- Consumes: 当前 `useRoute().fullPath`。
- Produces: 路径变化时，内层页面组件拥有不同 vnode key，使 `<Transition name="page" mode="out-in">` 对管理端模块同样执行。

- [ ] **Step 1: 写组件渲染失败测试，断言 route key 会随完整路径变化**
- [ ] **Step 2: 运行测试，确认当前 RouterView 无 key 而失败**
- [ ] **Step 3: 使用 RouterView 插槽渲染，并绑定 `:key="route.fullPath"`**

```vue
<RouterView v-slot="{ Component }">
  <Transition name="page" mode="out-in">
    <component :is="Component" :key="route.fullPath" />
  </Transition>
</RouterView>
```

- [ ] **Step 4: 运行测试与构建确认通过**

### Task 4: 修复自定义原生下拉的交互与生命周期

**Files:**
- Modify: `fronted/src/plugins/customSelect.ts`
- Create: `fronted/src/plugins/customSelect.test.ts`

**Interfaces:**
- Produces: `enhance(select): () => void` 清理函数；触发器拥有 `aria-expanded`，支持 Enter、Space、ArrowUp、ArrowDown、Escape，菜单选项具有 `role="option"`/`aria-selected`。

- [ ] **Step 1: 写失败测试，覆盖键盘开合、方向键选择、关闭与 destroy 清理**
- [ ] **Step 2: 运行测试，确认当前插件未添加键盘处理和清理函数而失败**
- [ ] **Step 3: 实现可销毁增强器，并以单一 MutationObserver 管理页面新增/移除的 select**
- [ ] **Step 4: 运行测试与构建确认通过**

### Task 5: 补齐动效覆盖与键盘焦点

**Files:**
- Modify: `fronted/src/assets/styles/motion.css`

**Interfaces:**
- Produces: 权重卡和学生详情时间线进入错峰；input/select/textarea 与现有按钮拥有统一 CQU 蓝 `:focus-visible` 描边。

- [ ] **Step 1: 写 CSS 回归测试，断言权重/详情列表含错峰选择器，表单控件有 focus-visible 规则**
- [ ] **Step 2: 运行测试，确认当前 CSS 缺失选择器而失败**
- [ ] **Step 3: 增加 `.weight-records > article`、详情时间线/指导历史条目的错峰，并在减弱动态媒体查询中禁用；补齐表单焦点态**
- [ ] **Step 4: 运行全量测试与 `npm run build`，按九项清单人工核验**
