# 前端 UX 与动画全面优化 — 设计文档

日期:2026-09-02
范围:`fronted/`(Vue 3 + TypeScript,纯手写 CSS,零组件库)

## 背景与目标

当前前端仅有少量动效(页面 `.fade-in` 200ms、列表 hover 底色、导航按钮 `.15s`、下拉箭头旋转)。路由切换、弹窗开关、Toast、下拉菜单、数字/进度/列表入场均无过渡,也无 `prefers-reduced-motion` 支持。目标是做一次「克制精致」的全方位 UX 打磨:短促、顺滑、与现有 CQU 学院派风格一致,不引入组件库、不重构业务逻辑。

**基调**:克制精致(150–250ms 短促缓动,克制的位移与淡入)。已获用户确认。

## 技术路线(已确认)

- 新增依赖 **`motion-v`**(Motion for Vue 3 官方实现,稳定 v1.x,TS 支持)。
- 职责分工:
  - `motion-v`:数字滚动(count-up)、hover/press 微交互反馈、`MotionConfig` 降级设置。
  - Vue 内置 `<Transition>` + 纯 CSS:路由切换、弹窗/下拉/Toast 的进出场(退出动画 CSS 更稳、可访问性最稳)。
  - 纯 CSS:进度条生长、列表逐行错峰入场(GPU 友好、零 JS)。

## 1. 依赖与动效节奏令牌

- `npm install motion-v`。
- 新增 `fronted/src/assets/styles/motion.css`,集中定义:
  - `--ease-out: cubic-bezier(.16,1,.3,1)`(克制版 easeOutCubic)
  - `--ease-in-out`(默认)
  - 时长档位:micro 120ms / base 200ms / route 240ms / grow 700ms
- `App.vue` 根级包 `<MotionConfig :reducedMotion="'user'">`。
- 全局 `@media (prefers-reduced-motion: reduce)`:
  - 关闭所有 transform/width/opacity 过渡动画,保留 ≤80ms 的透明度过渡,保证信息仍可感知。

## 2. 路由 / 页面切换

- `App.vue` 根 `<RouterView>`、`layouts/DefaultLayout.vue`、`layouts/AdminLayout.vue` 内层 `<RouterView>` 均包 `<Transition name="page" mode="out-in">`。
- 效果:进入 = 淡入 + translateY(8px) → 归位;离开 = 淡出 + translateY(-4px)。240ms,`--ease-out`。
- 覆盖登录↔工作台(根 RouterView)与工作台内页跳转(布局内 RouterView)。
- `mode="out-in"` 避免进出重叠。

## 3. 弹窗 / 下拉 / Toast 过渡

- **弹窗**:所有 `BaseModal`/对话框(`PasswordChangeDialog`、`AdvisorStudentDialog`)调用点包 `<Transition name="modal">`。遮罩淡入淡出 + 卡片 scale .97→1。160ms。
  - 调用点(已核实,共 9 个 `<Transition>` 包装点):`App.vue` 的 `<PasswordChangeDialog>` 与 `<AdvisorStudentDialog>` 各 1;直接使用 `BaseModal` 的页面:`StudentExperiencesPage`(1)、`StudentProfilePage`(1)、`StudentPrivacyPage`(1)、`AdvisorAccountPage`(1)、`AdminWorkbenchView`(3 处,第 245–247 行)。注:`AdvisorStudentsPage`/`AdvisorAttentionPage` 无自带 BaseModal,其详情弹窗由 `AdvisorStudentDialog` 承载。
- **BaseSelect 下拉**:组件内部菜单 `v-if="open"` 处包 `<Transition name="dropdown">`,淡入 + scaleY .98→1。
- **customSelect 插件**:原生下拉由 DOM 类切换(`.base-select-renderer.open`),改用 CSS transition(透明度 + translateY)开合对称。
- **Toast**:`App.vue` 的 `<div class="toast">` 包 `<Transition name="toast">`,右侧滑入(translateX(24px)→0)+ 淡出,220ms,与现有 2200ms 自动消失衔接。

## 4. 按钮 / 导航 / 卡片微交互

- **按钮**(`.outline-btn/.primary-btn/.compact-btn/.tab-bar button` 等):hover translateY(-1px) + 底色/边框加深;`:active` scale(.98);统一 `transition: transform .12s var(--ease-out), background .12s`。
- **导航项**(`nav button`):hover 右移 2px;`active` 左侧指示条宽度 0→4px 过渡 + 底色平滑(时长令牌化)。
- **卡片 / 列表行**:可交互卡片 hover translateY(-2px) + 轻阴影;列表行保持现有底色反馈不抬升(避免过度)。
- **焦点态**:所有可交互元素补 `:focus-visible` 2px 外圈描边(使用 CQU 蓝),键盘可访问性。

## 5. 数字 / 进度 / 列表入场 / 骨架屏

- **数字滚动**:新增 `fronted/src/components/CountUp.vue`,`props: { to: number; duration?: number }`,内部用 motion-v `animate()` 写本地 ref,0.8s 缓动,`{{ Math.round(value) }}` 渲染。
  - 应用点:学生档案完整度、辅导员四项指标、任务完成率、管理端概览大数字。
- **进度条生长**:`.progress-line span`、`.weight-bars i b`、`.analysis-bars em`、`.path-bars em`、`.dimension-list em` 加 `@keyframes grow`(`from { width: 0 }` → 元素实际宽度),`animation: grow .7s var(--ease-out) both`,挂载即播。
- **列表逐行错峰**:`.enhanced-list article`、`.record-list article`、`.advisor-student-table article`、`.attention-list article` 加 `style="--i: index"`,`animation: item-in .3s both` + `animation-delay: calc(var(--i) * 30ms)`(纯 CSS)。
- **骨架屏**:`.skeleton` 基类 + shimmer 渐变关键帧。替换以下「正在读取…」文案为对应形状骨架块:辅导员总览(`AdvisorDashboardPage`)、学生列表(`AdvisorStudentsPage`)、管理端工作台(`AdminWorkbenchView`)、学生档案概览(`StudentOverviewPage`)。

## 6. 可访问性

- 全局 `prefers-reduced-motion` 兜底 + `MotionConfig reducedMotion="user"`。
- 动画仅 opacity/transform/width,不阻塞交互、不改变布局流、不导致内容消失(降级下保留透明度过渡)。

## 7. 改动范围与实现顺序

- 新增:`fronted/src/assets/styles/motion.css`、`fronted/src/components/CountUp.vue`。
- 修改:`App.vue`、`DefaultLayout.vue`、`AdminLayout.vue`、`BaseModal.vue`(如需)、`BaseSelect.vue`、`plugins/customSelect.ts`、`cqu-theme.css`、`styles.css`(或 `lists.css`/`functional.css` 内相关选择器)、约 10 个页面/对话框组件。
- 顺序:
  1. 安装 `motion-v`,落地 `motion.css` + `MotionConfig` + reduced-motion。
  2. 路由过渡(App.vue + 两布局)。
  3. 弹窗/下拉/Toast 过渡。
  4. 按钮/导航/卡片微交互 + 焦点态。
  5. `CountUp.vue` + 数字应用点。
  6. 进度条生长 + 列表错峰 + 骨架屏。
  7. `npm run build` 验证,逐一目测关键页面。

## 约束

- 不新增组件库、不重构既有布局与业务逻辑。
- 全部动效克制:时长 ≤250ms(除进度条/数字 700–800ms 生长),位移 ≤8px。
- 保持现有 CQU 学院派视觉语言。
