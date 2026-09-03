# 前端 UX 与动画全面优化 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `fronted/`(Vue 3 + TS,纯手写 CSS)做一次「克制精致」的全方位动效打磨:路由/弹窗/下拉/Toast 过渡、按钮/导航/卡片微交互、数字滚动、进度条生长、列表错峰入场、骨架屏,并支持 `prefers-reduced-motion`。

**Architecture:** 职责分工——`motion-v` 负责数字滚动(`CountUp` 组件)与 `MotionConfig` 降级;Vue 内置 `<Transition>` + 纯 CSS 负责路由/弹窗/下拉/Toast 进出场;纯 CSS 负责进度条生长、列表错峰、骨架屏。所有动效规则集中在新增的 `fronted/src/assets/styles/motion.css`(在 `cqu-theme.css` 之后引入,优先级最高)。

**Tech Stack:** Vue 3.5、Vue Router 4、TypeScript、`motion-v`(Motion for Vue,稳定 v1.x)、原生 CSS。

## Global Constraints

- 现有 5 个样式文件与各 `.vue` 组件的作用域样式保持不动,新增动效一律写入 `motion.css`(除非计划里明确说改某个文件)。
- 动画克制:时长 ≤250ms(进度条/数字 700–800ms 除外),位移 ≤8px,全部用 `var(--ease-out)`。
- `fronted/` 内命令在 `fronted/` 目录执行:`npm run build`(即 `vue-tsc -b && vite build`)。每个任务必须构建通过。
- 提交:仅 `git add` 本任务涉及文件;提交信息为中文、符合仓库风格、**不包含任何 Claude 署名**;**不 push**。
- 所有动画必须能被 `prefers-reduced-motion: reduce` 兜底(见 Task 1 的全局规则)。

---

### Task 1: 安装 `motion-v`,落地动效令牌与全局 reduced-motion

**Files:**
- Create: `fronted/src/assets/styles/motion.css`
- Modify: `fronted/src/main.ts`(引入 motion.css)
- Modify: `fronted/src/App.vue`(包 `MotionConfig`)

**Interfaces:**
- Produces: CSS 变量 `--ease-out`、`--dur-micro/base/route/grow`;全局 `@media (prefers-reduced-motion: reduce)` 兜底。后续所有任务都依赖这些令牌。
- Consumes: `motion-v` 包的 `MotionConfig` 组件(需 `npm install motion-v`)。

- [ ] **Step 1: 安装依赖**

Run: `cd /Users/tano/Documents/GitHub/career-planner/fronted && npm install motion-v`
Expected: 安装成功,`package.json` 增加 `"motion-v"` 依赖。

- [ ] **Step 2: 创建 motion.css**

Create `fronted/src/assets/styles/motion.css`:

```css
/* UX 动效令牌与全局过渡 — 克制精致基调。 */
:root {
  --ease-out: cubic-bezier(.16, 1, .3, 1);
  --dur-micro: 120ms;
  --dur-base: 200ms;
  --dur-route: 240ms;
  --dur-grow: 700ms;
}

/* 全局 reduced-motion 兜底:关闭位移/宽度动画,仅保留 ≤80ms 透明度过渡。 */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: .01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: .08s !important;
    transition-delay: 0s !important;
    scroll-behavior: auto !important;
  }
}
```

- [ ] **Step 3: main.ts 引入 motion.css(最后引入,保证优先级)**

In `fronted/src/main.ts`, after `import './assets/styles/cqu-theme.css'`(第 8 行)追加一行:

```ts
import './assets/styles/motion.css'
```

- [ ] **Step 4: App.vue 包 `MotionConfig`**

In `fronted/src/App.vue`:
1. 在 `<script setup>` 顶部(第 2 行 `import { watch } from 'vue'` 之后)加入:
```ts
import { MotionConfig } from 'motion-v'
```
2. 将 `<template>` 整体内容包进 `<MotionConfig :reducedMotion="'user'">`:

```vue
<template>
  <MotionConfig :reducedMotion="'user'">
    <RouterView />
    <PasswordChangeDialog
      v-if="forcePasswordChange"
      mandatory
      :saving="forcedPasswordSaving"
      @submit="handleForcedPasswordChange"
    />
    <AdvisorStudentDialog
      v-if="advisorDetail"
      :student="advisorDetail"
      :saving="guidanceSaving"
      @close="closeAdvisorDetail"
      @submit="sendGuidance"
    />
    <div v-if="toast" class="toast">✓ {{ toast }}</div>
  </MotionConfig>
</template>
```

> `reducedMotion="user"` 让 `motion-v` 生成的动画尊重系统「减弱动态效果」;纯 CSS 部分由 Step 2 的媒体查询兜底。

- [ ] **Step 5: 构建验证**

Run: `cd /Users/tano/Documents/GitHub/career-planner/fronted && npm run build`
Expected: `vue-tsc -b && vite build` 通过,无类型错误。若 `MotionConfig` 的 `reducedMotion` 类型报错,改为 `:reducedMotion="'user' as const"`。

- [ ] **Step 6: 提交**

```bash
cd /Users/tano/Documents/GitHub/career-planner && git add fronted/package.json fronted/package-lock.json fronted/src/assets/styles/motion.css fronted/src/main.ts fronted/src/App.vue && git commit -m "feat: 引入 motion-v 并落地动效令牌与全局 reduced-motion"
```

---

### Task 2: 路由 / 页面切换过渡

**Files:**
- Modify: `fronted/src/App.vue:36`(根 `<RouterView />` 包 Transition)
- Modify: `fronted/src/layouts/DefaultLayout.vue:23`
- Modify: `fronted/src/layouts/AdminLayout.vue:23`
- Modify: `fronted/src/assets/styles/motion.css`(追加 `.page-*` 规则)

**Interfaces:**
- Consumes: Task 1 的 `--ease-out`、`--dur-route`。

- [ ] **Step 1: App.vue 根 RouterView 包 Transition**

`fronted/src/App.vue` 模板中:
```vue
<RouterView />
```
改为:
```vue
<Transition name="page" mode="out-in">
  <RouterView />
</Transition>
```

- [ ] **Step 2: DefaultLayout 内层 RouterView 包 Transition 并去掉 fade-in**

`fronted/src/layouts/DefaultLayout.vue` 第 23 行:
```
...<section class="page fade-in"><RouterView/></section>
```
改为:
```
...<section class="page"><Transition name="page" mode="out-in"><RouterView/></Transition></section>
```

- [ ] **Step 3: AdminLayout 内层 RouterView 包 Transition 并去掉 fade-in**

`fronted/src/layouts/AdminLayout.vue` 第 23 行:
```
<section class="page fade-in"><div class="page-title">...<RouterView/></section>
```
改为(仅 `fade-in` 与 RouterView 两处):
```
<section class="page"><div class="page-title">...<Transition name="page" mode="out-in"><RouterView/></Transition></section>
```

- [ ] **Step 4: 追加 `.page-*` 过渡规则到 motion.css 末尾**

Append:
```css
/* ---- 路由 / 页面切换 ---- */
.page-enter-active, .page-leave-active { transition: opacity var(--dur-route) var(--ease-out), transform var(--dur-route) var(--ease-out); }
.page-enter-from { opacity: 0; transform: translateY(8px); }
.page-leave-to { opacity: 0; transform: translateY(-4px); }
```

- [ ] **Step 5: 构建验证**

Run: `cd /Users/tano/Documents/GitHub/career-planner/fronted && npm run build`
Expected: 通过。手动检查:工作台内页跳转、登录↔工作台切换均出现 240ms 淡入上移(降级下为纯淡入)。

- [ ] **Step 6: 提交**

```bash
cd /Users/tano/Documents/GitHub/career-planner && git add fronted/src/App.vue fronted/src/layouts/DefaultLayout.vue fronted/src/layouts/AdminLayout.vue fronted/src/assets/styles/motion.css && git commit -m "feat: 路由与页面切换过渡"
```

---

### Task 3: 弹窗 / 下拉 / Toast 过渡

**Files:**
- Modify: `fronted/src/components/BaseSelect.vue:16`(菜单包 Transition)
- Modify: `fronted/src/App.vue`(Toast 包 Transition)
- Modify(弹窗调用点全部包 `<Transition name="modal">`,共 6 文件 9 处):
  - `fronted/src/App.vue:37,43`(`PasswordChangeDialog`、`AdvisorStudentDialog`)
  - `fronted/src/views/student/StudentProfilePage.vue:15`
  - `fronted/src/views/student/StudentPrivacyPage.vue:12`
  - `fronted/src/views/student/StudentExperiencesPage.vue:18`(两处)
  - `fronted/src/views/advisor/AdvisorAccountPage.vue:20`
  - `fronted/src/views/admin/AdminWorkbenchView.vue:245-247`(三处)
- Modify: `fronted/src/assets/styles/motion.css`(追加 `.modal-*`、`.dropdown-*`、`.native-select-*`、`.toast-*` 规则)

**Interfaces:**
- Consumes: Task 1 的 `--ease-out`、`--dur-base`、`--dur-micro`。

- [ ] **Step 1: BaseSelect 菜单包 `<Transition name="dropdown">`**

`fronted/src/components/BaseSelect.vue` 模板:
- 开标签:`<div v-if="open" class="base-select-menu" role="listbox">` → `<Transition name="dropdown"><div v-if="open" class="base-select-menu" role="listbox">`
- 闭标签:`</button></div></div></template>` → `</button></div></Transition></div></template>`

- [ ] **Step 2: App.vue Toast 包 `<Transition name="toast">`**

`fronted/src/App.vue` 中:
```vue
<div v-if="toast" class="toast">✓ {{ toast }}</div>
```
改为:
```vue
<Transition name="toast">
  <div v-if="toast" class="toast">✓ {{ toast }}</div>
</Transition>
```

- [ ] **Step 3: 全部弹窗调用点包 `<Transition name="modal">`**

对下列每个 `<BaseModal ...>` / `<PasswordChangeDialog ...>` / `<AdvisorStudentDialog ...>` 调用,用其 `v-if` 条件定位,在其开标签前加 `<Transition name="modal">`,在其对应闭标签后加 `</Transition>`:

1. `App.vue`:`<PasswordChangeDialog v-if="forcePasswordChange" ... />` 与 `<AdvisorStudentDialog v-if="advisorDetail" ... />` 各自包一层。
2. `StudentProfilePage.vue`:`<BaseModal v-if="editing" @close="editing=false">` → 闭于 `</BaseModal></template>` 前。
3. `StudentPrivacyPage.vue`:`<BaseModal v-if="action" @close="action=null">` → 闭于 `</BaseModal></template>` 前。
4. `StudentExperiencesPage.vue`:
   - 第一个 `<BaseModal v-if="editing" @close="editing=false">`,闭于 `</form></BaseModal>` 后(该字符串唯一)。
   - 第二个 `<BaseModal v-if="deleting" @close="deleting=null">`,闭于 `</div></BaseModal></template>` 前。
5. `AdvisorAccountPage.vue`:`<BaseModal v-if="action" @close="close">`,闭于 `</form></BaseModal>` 后(该字符串唯一,其后紧跟 `</template>`)。
6. `AdminWorkbenchView.vue`:
   - `<BaseModal v-if="modal" @close="modal=false">`,闭于 `</form></BaseModal>` 后。
   - `<BaseModal v-if="generatedInitialPasswords.length" :closeable="false">`,闭于 `我已记录</button></div></section></BaseModal>` 后。
   - `<BaseModal v-if="deleteTarget" @close="deleteTarget=null">`,闭于 `确定删除</button></div></section></BaseModal>` 后。

> 注意:`<Transition>` 要求单根子元素,每个调用点恰好一个弹窗组件,满足。`mode="out-in"` 不需要——弹窗进出本就互斥。

- [ ] **Step 4: 追加弹窗/下拉/Toast 过渡规则到 motion.css 末尾**

Append:
```css
/* ---- 弹窗:遮罩淡入淡出 + 卡片 0.97→1 缩放 ---- */
.modal-enter-active, .modal-leave-active { transition: opacity var(--dur-base) var(--ease-out); }
.modal-enter-active .modal-card, .modal-leave-active .modal-card { transition: transform var(--dur-base) var(--ease-out); }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal-card, .modal-leave-to .modal-card { transform: scale(.97); }

/* ---- 下拉(BaseSelect 菜单)---- */
.dropdown-enter-active, .dropdown-leave-active { transition: opacity var(--dur-micro) var(--ease-out), transform var(--dur-micro) var(--ease-out); transform-origin: top; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-2px) scaleY(.98); }

/* ---- 下拉(customSelect 原生增强:类切换过渡)---- */
.native-select-menu { display: block; opacity: 0; transform: translateY(-2px) scaleY(.98); transform-origin: top; visibility: hidden; pointer-events: none; transition: opacity var(--dur-micro) var(--ease-out), transform var(--dur-micro) var(--ease-out), visibility var(--dur-micro); }
.native-select-renderer.open .native-select-menu { opacity: 1; transform: none; visibility: visible; pointer-events: auto; }

/* ---- Toast:右侧滑入 + 淡出 ---- */
.toast-enter-active, .toast-leave-active { transition: opacity .22s var(--ease-out), transform .22s var(--ease-out); }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(24px); }
```

> customSelect 的下拉由 `.open` 类切换 `display`。原规则 `.native-select-menu{display:none}` 与 `.open .native-select-menu{display:block}` 会被上面 `display:block` 覆盖;开合用 `visibility` 与 `opacity` 控制,无需改 JS。

- [ ] **Step 5: 构建验证**

Run: `cd /Users/tano/Documents/GitHub/career-planner/fronted && npm run build`
Expected: 通过。手动检查:任一处弹窗开合、辅导员详情弹窗、两个 BaseSelect 下拉、toast 弹出均有过渡;强制改密弹窗(`:closeable="false"`)不受影响。

- [ ] **Step 6: 提交**

```bash
cd /Users/tano/Documents/GitHub/career-planner && git add fronted/src/components/BaseSelect.vue fronted/src/App.vue fronted/src/views/student/StudentProfilePage.vue fronted/src/views/student/StudentPrivacyPage.vue fronted/src/views/student/StudentExperiencesPage.vue fronted/src/views/advisor/AdvisorAccountPage.vue fronted/src/views/admin/AdminWorkbenchView.vue fronted/src/assets/styles/motion.css && git commit -m "feat: 弹窗、下拉与 Toast 过渡"
```

---

### Task 4: 按钮 / 导航 / 卡片微交互与焦点环

**Files:**
- Modify: `fronted/src/assets/styles/motion.css`(追加微交互规则)

**Interfaces:**
- Consumes: Task 1 的 `--ease-out`、`--dur-micro`、`--dur-base`;`--cqu-blue`(来自 `cqu-theme.css`)。

- [ ] **Step 1: 追加按钮/导航/卡片/焦点环规则到 motion.css 末尾**

Append:
```css
/* ---- 按钮微交互 ---- */
.outline-btn, .primary-btn, .outline-light, .white-btn, .compact-btn, .topbar .logout-btn, .tab-bar button {
  transition: transform var(--dur-micro) var(--ease-out), background-color var(--dur-micro) var(--ease-out), border-color var(--dur-micro) var(--ease-out), color var(--dur-micro) var(--ease-out), box-shadow var(--dur-micro) var(--ease-out);
}
.outline-btn:hover, .outline-light:hover, .white-btn:hover, .compact-btn:hover, .topbar .logout-btn:hover, .tab-bar button:hover:not(:disabled) { transform: translateY(-1px); }
.primary-btn:hover { transform: translateY(-1px); }
.outline-btn:active, .primary-btn:active, .outline-light:active, .white-btn:active, .compact-btn:active, .topbar .logout-btn:active, .tab-bar button:active:not(:disabled) { transform: translateY(0) scale(.98); }

/* ---- 导航项:悬停右移 + 左侧指示条生长(伪元素,避免布局偏移)---- */
.side-nav nav button { position: relative; border-left-width: 0; transition: background-color var(--dur-base) var(--ease-out), color var(--dur-base) var(--ease-out), transform var(--dur-base) var(--ease-out); }
.side-nav nav button::before { content: ""; position: absolute; left: 0; top: 0; bottom: 0; width: 4px; background: #fff; transform: scaleY(0); transition: transform var(--dur-base) var(--ease-out); }
.side-nav nav button:hover { transform: translateX(2px); }
.side-nav nav button.active::before { transform: scaleY(1); }

/* ---- 卡片 / 列表行微抬升 ---- */
.weight-card { transition: transform var(--dur-base) var(--ease-out), box-shadow var(--dur-base) var(--ease-out); }
.weight-card:hover { transform: translateY(-2px); box-shadow: 0 12px 28px rgba(0, 75, 157, .12); }
.enhanced-list article { transition: background-color var(--dur-micro) var(--ease-out), transform var(--dur-micro) var(--ease-out); }
.enhanced-list article:hover { transform: translateY(-1px); }

/* ---- 键盘焦点环 ---- */
button:focus-visible, a:focus-visible, [role="button"]:focus-visible { outline: 2px solid var(--cqu-blue); outline-offset: 2px; }
```

> 说明:原 `.side-nav nav button{...border-left:2px solid transparent...}` 的指示线改为伪元素 `::before` 的 4px 白色竖条,`scaleY` 从 0→1 生长,不改变布局。按钮的 hover 底色变化保留(来自 `styles.css`/`cqu-theme.css`),此处只叠加位移与按压反馈。

- [ ] **Step 2: 构建验证**

Run: `cd /Users/tano/Documents/GitHub/career-planner/fronted && npm run build`
Expected: 通过。手动检查:按钮 hover 上移 1px、按压回缩;导航 hover 右移、active 左侧白条生长;Tab 键聚焦出现蓝描边。

- [ ] **Step 3: 提交**

```bash
cd /Users/tano/Documents/GitHub/career-planner && git add fronted/src/assets/styles/motion.css && git commit -m "feat: 按钮、导航与卡片微交互及焦点环"
```

---

### Task 5: 进度条生长 + 列表逐行错峰入场

**Files:**
- Modify: `fronted/src/assets/styles/motion.css`(追加 `grow`、`item-in` 关键帧与延迟规则)
- Modify(为 v-for 行加 `--i` 延迟索引,共 6 文件):
  - `fronted/src/views/advisor/AdvisorStudentsPage.vue:10`
  - `fronted/src/views/advisor/AdvisorAttentionPage.vue:9`
  - `fronted/src/views/advisor/AdvisorGuidancePage.vue:8`
  - `fronted/src/views/admin/AdminWorkbenchView.vue:243`
  - `fronted/src/views/student/StudentExperiencesPage.vue:18`
  - `fronted/src/views/student/StudentProfilePage.vue:15`

**Interfaces:**
- Consumes: Task 1 的 `--ease-out`、`--dur-grow`。列表入场规则作用于既有类名 `.enhanced-list/.record-list/.advisor-student-table/.attention-list` 下的 `article`。

- [ ] **Step 1: 追加 grow / item-in 关键帧到 motion.css 末尾**

Append:
```css
/* ---- 进度条生长:挂载时从 0 生长到实际宽度 ---- */
.progress-line span, .weight-bars i b, .analysis-bars em, .path-bars em, .dimension-list em {
  animation: grow var(--dur-grow) var(--ease-out) both;
}
@keyframes grow { from { width: 0; } }

/* ---- 列表逐行错峰入场:第 n 行延迟 n*30ms ---- */
.enhanced-list article, .record-list article, .advisor-student-table article, .attention-list article {
  animation: item-in .3s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 30ms);
}
@keyframes item-in { from { opacity: 0; transform: translateY(6px); } }
```

- [ ] **Step 2: 六个 v-for 行加 `index` 与 `:style="{ '--i': index }"`**

对下列每处,把 `v-for="X in Y"` 改为 `v-for="(X,index) in Y"`,并在同一 `<article ...>` 标签里加 `:style="{ '--i': index }"`:

1. `AdvisorStudentsPage.vue`:`<article v-for="student in students" :key="student.id">`
   → `<article v-for="(student,index) in students" :key="student.id" :style="{ '--i': index }">`
2. `AdvisorAttentionPage.vue`:`<article v-for="item in items" :key="item.student.id">`
   → `<article v-for="(item,index) in items" :key="item.student.id" :style="{ '--i': index }">`
3. `AdvisorGuidancePage.vue`:`<article v-for="student in students" :key="student.id">`
   → `<article v-for="(student,index) in students" :key="student.id" :style="{ '--i': index }">`
4. `AdminWorkbenchView.vue`:`<article v-for="row in rows" :key="rowId(row)">`
   → `<article v-for="(row,index) in rows" :key="rowId(row)" :style="{ '--i': index }">`
5. `StudentExperiencesPage.vue`:`<article v-for="item in experiences" :key="item.id">`
   → `<article v-for="(item,index) in experiences" :key="item.id" :style="{ '--i': index }">`
6. `StudentProfilePage.vue`:`<article v-for="item in [{t:'姓名',...}]" :key="item.t">`
   → `<article v-for="(item,index) in [{t:'姓名',...}]" :key="item.t" :style="{ '--i': index }">`(`...` 为原有的对象数组原文,保持不变)

- [ ] **Step 3: 构建验证**

Run: `cd /Users/tano/Documents/GitHub/career-planner/fronted && npm run build`
Expected: 通过。手动检查:各列表首次渲染时逐行淡入上移;进度条/权重条/统计条挂载时从 0 生长到目标宽度。

- [ ] **Step 4: 提交**

```bash
cd /Users/tano/Documents/GitHub/career-planner && git add fronted/src/assets/styles/motion.css fronted/src/views/advisor/AdvisorStudentsPage.vue fronted/src/views/advisor/AdvisorAttentionPage.vue fronted/src/views/advisor/AdvisorGuidancePage.vue fronted/src/views/admin/AdminWorkbenchView.vue fronted/src/views/student/StudentExperiencesPage.vue fronted/src/views/student/StudentProfilePage.vue && git commit -m "feat: 进度条生长与列表逐行入场"
```

---

### Task 6: 数字滚动 CountUp 组件并接线

**Files:**
- Create: `fronted/src/components/CountUp.vue`
- Modify(接线数字):
  - `fronted/src/views/student/StudentOverviewPage.vue:10`(档案完整度)
  - `fronted/src/views/advisor/AdvisorDashboardPage.vue:10`(四项指标 + 任务完成率)
  - `fronted/src/views/advisor/AdvisorStatisticsPage.vue:45-48`(四项指标)

**Interfaces:**
- Produces: `<CountUp :to="number" :duration="0.8" />` —— props:`to:number`(必填)、`duration?:number`(默认 0.8)。渲染为 `<span>{{ 取整后的数字 }}</span>`。
- Consumes: `motion-v` 的 `animate()` 函数。

- [ ] **Step 1: 创建 CountUp.vue**

Create `fronted/src/components/CountUp.vue`:

```vue
<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { animate } from 'motion-v'

const props = withDefaults(defineProps<{ to: number; duration?: number }>(), { duration: 0.8 })
const EASE: [number, number, number, number] = [0.16, 1, 0.3, 1]
const display = ref(0)
let controls: { stop: () => void } | undefined

function run() {
  controls?.stop()
  const target = Number(props.to)
  if (!Number.isFinite(target)) { display.value = 0; return }
  controls = animate(display.value, target, {
    duration: props.duration,
    ease: EASE,
    onUpdate: (value: number) => { display.value = Math.round(value) },
  })
}
watch(() => props.to, run, { immediate: true })
onBeforeUnmount(() => controls?.stop())
</script>

<template>
  <span>{{ display }}</span>
</template>
```

- [ ] **Step 2: 接线 StudentOverviewPage 完整度**

`fronted/src/views/student/StudentOverviewPage.vue`:
1. `<script setup>` 加:`import CountUp from '../../components/CountUp.vue'`
2. 模板:
```
<b>{{ completeness?.score ?? profile?.completeness ?? '—' }}<small>%</small></b>
```
改为:
```
<b v-if="(completeness?.score ?? profile?.completeness) != null"><CountUp :to="Number(completeness?.score ?? profile?.completeness ?? 0)"/><small>%</small></b><b v-else>—</b>
```

- [ ] **Step 3: 接线 AdvisorDashboardPage 指标**

`fronted/src/views/advisor/AdvisorDashboardPage.vue`:
1. `<script setup>` 加:`import CountUp from '../../components/CountUp.vue'`
2. 模板四项指标(第 10 行 `metric-grid advisor-metrics` 内):
```
<b>{{statistics?.totalStudents??0}}</b>
```
改为:
```
<b><CountUp :to="statistics?.totalStudents ?? 0"/></b>
```
同法处理 `assessedCount`、`planMadeCount`、`reviewedCount`(同样包进 `<CountUp :to="... ?? 0"/>`)。任务完成率:
```
<b class="advisor-rate">{{statistics?.taskCompletionRate??'—'}}<small v-if="statistics?.taskCompletionRate!==undefined">%</small></b>
```
改为:
```
<b class="advisor-rate"><CountUp v-if="statistics?.taskCompletionRate!==undefined" :to="statistics?.taskCompletionRate ?? 0"/><span v-else>—</span><small v-if="statistics?.taskCompletionRate!==undefined">%</small></b>
```

- [ ] **Step 4: 接线 AdvisorStatisticsPage 指标**

`fronted/src/views/advisor/AdvisorStatisticsPage.vue`:
1. `<script setup>` 加:`import CountUp from '../../components/CountUp.vue'`
2. 模板(第 45–48 行四项):
```
<b>{{students.length}}</b>            → <b><CountUp :to="students.length"/></b>
<b>{{averageCompleteness}}<small>%</small></b> → <b><CountUp :to="averageCompleteness"/><small>%</small></b>
<b>{{averagePlanRate}}<small>%</small></b>     → <b><CountUp :to="averagePlanRate"/><small>%</small></b>
<b>{{priorityCount}}</b>             → <b><CountUp :to="priorityCount"/></b>
```

- [ ] **Step 5: 构建验证**

Run: `cd /Users/tano/Documents/GitHub/career-planner/fronted && npm run build`
Expected: 通过。若 `animate` 的返回类型不满足 `{ stop: () => void }`,把 Step 1 的 `controls` 类型改为 `ReturnType<typeof animate> | undefined`。手动检查:上述数字挂载时在 0.8s 内滚动到目标值。

- [ ] **Step 6: 提交**

```bash
cd /Users/tano/Documents/GitHub/career-planner && git add fronted/src/components/CountUp.vue fronted/src/views/student/StudentOverviewPage.vue fronted/src/views/advisor/AdvisorDashboardPage.vue fronted/src/views/advisor/AdvisorStatisticsPage.vue && git commit -m "feat: 数字滚动 CountUp 组件并接入指标展示"
```

---

### Task 7: 骨架屏加载态

**Files:**
- Modify(替换「正在读取…」文案为骨架块,3 文件):
  - `fronted/src/views/advisor/AdvisorDashboardPage.vue:10`
  - `fronted/src/views/advisor/AdvisorStudentsPage.vue:10`
  - `fronted/src/views/admin/AdminWorkbenchView.vue:242`
- Modify: `fronted/src/assets/styles/motion.css`(追加 `.skeleton` 与 shimmer 关键帧)

**Interfaces:**
- Produces: `.skeleton` 基类(灰底 + 扫光 shimmer)。三个页面各自的加载分支改为该标记。

- [ ] **Step 1: 追加 skeleton 样式到 motion.css 末尾**

Append:
```css
/* ---- 骨架屏 ---- */
.skeleton { position: relative; overflow: hidden; background: var(--cqu-blue-20); border-radius: 2px; }
.skeleton::after { content: ""; position: absolute; inset: 0; background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .55), transparent); animation: shimmer 1.1s var(--ease-out) infinite; }
@keyframes shimmer { from { transform: translateX(-100%); } to { transform: translateX(100%); } }
```

- [ ] **Step 2: AdvisorDashboardPage 加载分支换骨架**

`fronted/src/views/advisor/AdvisorDashboardPage.vue` 模板中:
```
<p v-if="loading" class="empty">正在读取统计数据…</p>
```
改为:
```
<div v-if="loading" class="skeleton-group"><div class="metric-grid advisor-metrics"><div v-for="i in 4" :key="i" class="skeleton" style="height:96px;border-right:1px solid var(--line)"></div></div><div class="skeleton" style="height:180px;margin-top:16px"></div></div>
```
> 说明:第 4 个格子无右边框,`border-right` 会被最后一项默认覆盖;如视觉多一条线可接受,保持简单。

- [ ] **Step 3: AdvisorStudentsPage 加载分支换骨架**

`fronted/src/views/advisor/AdvisorStudentsPage.vue` 模板中:
```
<p v-if="loading" class="empty">正在读取学生数据…</p>
```
改为:
```
<div v-if="loading" class="skeleton-group"><div class="skeleton" style="height:46px;margin-bottom:14px"></div><div v-for="i in 6" :key="i" class="skeleton" style="height:64px;margin-bottom:10px"></div></div>
```

- [ ] **Step 4: AdminWorkbenchView 加载分支换骨架**

`fronted/src/views/admin/AdminWorkbenchView.vue` 模板中:
```
<p v-if="loading" class="empty">正在读取数据…</p>
```
改为:
```
<div v-if="loading" class="skeleton-group"><div class="skeleton" style="height:46px;margin-bottom:14px"></div><div v-for="i in 6" :key="i" class="skeleton" style="height:56px;margin-bottom:10px"></div></div>
```

- [ ] **Step 5: 构建验证**

Run: `cd /Users/tano/Documents/GitHub/career-planner/fronted && npm run build`
Expected: 通过。手动检查:辅导员总览/学生列表/管理端各模块加载时出现灰底扫光骨架,数据就绪后切换为内容。

- [ ] **Step 6: 提交**

```bash
cd /Users/tano/Documents/GitHub/career-planner && git add fronted/src/assets/styles/motion.css fronted/src/views/advisor/AdvisorDashboardPage.vue fronted/src/views/advisor/AdvisorStudentsPage.vue fronted/src/views/admin/AdminWorkbenchView.vue && git commit -m "feat: 加载骨架屏"
```

---

### Task 8: 全量构建验证与目测清单

**Files:**
- 无代码改动。

- [ ] **Step 1: 全量构建**

Run: `cd /Users/tano/Documents/GitHub/career-planner/fronted && npm run build`
Expected: `vue-tsc -b && vite build` 通过。

- [ ] **Step 2: 目测清单(逐项人工确认)**

- 登录页 ↔ 工作台、工作台内页跳转:240ms 淡入上移(降级下纯淡入)。
- 弹窗(资料编辑、删除确认、改密、辅导员详情、管理端编辑器/初始密码/删除确认):遮罩淡入 + 卡片 0.97→1;关闭时反向。
- 两个下拉(BaseSelect 与 customSelect 原生增强):开合有 120ms 过渡。
- Toast:右侧滑入、淡出。
- 按钮 hover 上移/按压回缩;导航 hover 右移、active 左侧白条;Tab 聚焦蓝描边。
- 完整度/辅导员指标/群体统计数字:0.8s 滚动;进度条/权重条从 0 生长。
- 各列表首次渲染逐行错峰。
- 辅导员总览/学生列表/管理端:加载时骨架屏。
- 系统偏好「减弱动态效果」开启后:位移/滚动/骨架扫光消失,仅保留极短透明度过渡。

- [ ] **Step 3: 提交(如 Step 2 有意外修正,单独提交;否则跳过)**

```bash
cd /Users/tano/Documents/GitHub/career-planner && git status
```
若干净则无需提交;若有修正文件,按前序规范单独 `git add` + 中文提交信息(无 Claude 署名、不 push)。

---

## Self-Review 记录

- **Spec 覆盖**:设计文档 §1→Task 1;§2→Task 2;§3→Task 3;§4→Task 4;§5(数字/进度/列表/骨架)→Task 5/6/7;§6→Task 1(reduced-motion)+ Task 4(焦点环);§7→Task 1–8 顺序一致。无缺口。
- **占位符**:全部步骤含完整代码或精确锚点字符串;`StudentProfilePage` 的 `{...}` 保留为「对象数组原文」并非占位,属实现时原样保留的既有代码。
- **类型一致性**:`CountUp` props `to/duration` 在 Task 6 定义,接线处使用一致;CSS 类名(`page/modal/dropdown/toast/item-in/grow/skeleton`)在各自任务定义且被引用处一致。
