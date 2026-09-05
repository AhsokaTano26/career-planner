# 全量后端接口前端接入 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 让 career-core 当前公开的全部业务接口在受角色保护的 Vue 页面中可发现、可提交并可查看结果。

**Architecture:** request.ts 是唯一 HTTP 边界，补齐强类型端点与查询编码；领域页面通过 composable 管理加载、提交和错误。覆盖注册表连接后端路径、API 方法和 UI 入口并由测试约束。

**Tech Stack:** Vue 3、TypeScript、Vue Router、Vite、Vitest、Vue Test Utils。

**Spec:** docs/superpowers/specs/2026-09-05-frontend-full-api-integration-design.md

## Global Constraints

- 不修改后端 API、数据库或权限契约。
- HTTP 调用只能经 request.ts；保持 JWT 刷新、请求 ID 和幂等键。
- 每个写操作必须有 loading、成功提示和可读失败提示。
- AI 仅调用后端网关，不能在前端存放供应商密钥。
- 每项行为先写失败测试，确认 RED 后才写实现。

---

### Task 1: API 契约与控制器覆盖注册表

**Files:**
- Create: fronted/src/api/coverage.ts
- Create: fronted/src/api/request.test.ts
- Modify: fronted/src/api/request.ts

**Interfaces:**
- Produces: toQuery(params: Record<string, unknown>): string。
- Produces: API_COVERAGE，每项包含 method、path、apiMethod、ui。
- Produces: 后台列表、日志、培养方案及学生详情的具名 api 方法。

- [ ] **Step 1: Write the failing test**

~~~ts
it('encodes only defined query parameters', () => {
  expect(toQuery({ page: 1, keyword: '张 三', blank: '', none: undefined }))
    .toBe('page=1&keyword=%E5%BC%A0+%E4%B8%89')
})
it('maps every controller endpoint to a method and UI', () => {
  expect(API_COVERAGE).toHaveLength(134)
  expect(API_COVERAGE.every(item => item.apiMethod && item.ui)).toBe(true)
})
~~~

- [ ] **Step 2: Run test to verify it fails**

Run: cd fronted && npm test -- src/api/request.test.ts

Expected: FAIL because toQuery and coverage do not exist.

- [ ] **Step 3: Write minimal implementation**

~~~ts
export function toQuery(params: Record<string, unknown>): string {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
  }
  return query.toString()
}
~~~

Add every controller mapping individually to API_COVERAGE; replace raw admin request calls with named api.admin methods.

- [ ] **Step 4: Run test to verify it passes**

Run: cd fronted && npm test -- src/api/request.test.ts

Expected: PASS.

- [ ] **Step 5: Commit**

~~~bash
git add fronted/src/api/request.ts fronted/src/api/request.test.ts fronted/src/api/coverage.ts
git commit -m "feat: complete frontend API contract"
~~~

### Task 2: Complete student workflows

**Files:**
- Create: fronted/src/composables/useStudentDevelopment.test.ts
- Modify: fronted/src/composables/useStudent.ts
- Modify: fronted/src/views/student/StudentDevelopmentPage.vue
- Modify: fronted/src/views/student/StudentPrivacyPage.vue

**Interfaces:**
- Consumes: student task, review, direction, portraitSnapshot, recommendationDetail, plan, updateReview, adoptAdvice and updatePlan API calls.
- Produces: 对问卷、画像、方向、推荐、目标、计划、任务、复盘和提醒的详情、历史、反馈和写入 UI。

- [ ] **Step 1: Write the failing test**

~~~ts
it('opens a historical plan by its id', async () => {
  await wrapper.get('[data-test="plan-history-item-plan-7"]').trigger('click')
  expect(api.student.plan).toHaveBeenCalledWith('plan-7')
})
it('adopts advisor advice for a review', async () => {
  await wrapper.get('[data-test="adopt-advice-review-2"]').trigger('click')
  expect(api.student.adoptAdvice).toHaveBeenCalledWith('review-2', { adopt: true })
})
~~~

- [ ] **Step 2: Run test to verify it fails**

Run: cd fronted && npm test -- src/composables/useStudentDevelopment.test.ts

Expected: FAIL because the detail and adoption entries are absent.

- [ ] **Step 3: Write minimal implementation**

Add drawers and history buttons for questionnaire versions/session details, profile snapshots, direction details, recommendation run details, plan edit/history, task details, review edit/detail/advice adoption and reminder filters. Use shared request state:

~~~ts
async function openPlan(id: string) {
  busy.value = 'plan-' + id
  try { selectedPlan.value = await api.student.plan(id) }
  catch (error) { show(getErrorMessage(error)) }
  finally { busy.value = '' }
}
~~~

- [ ] **Step 4: Run test to verify it passes**

Run: cd fronted && npm test -- src/composables/useStudentDevelopment.test.ts

Expected: PASS.

- [ ] **Step 5: Commit**

~~~bash
git add fronted/src/composables/useStudent.ts fronted/src/composables/useStudentDevelopment.test.ts fronted/src/views/student
git commit -m "feat: expose complete student workflows"
~~~

### Task 3: Complete advisor detail and guidance actions

**Files:**
- Create: fronted/src/components/AdvisorStudentDialog.test.ts
- Modify: fronted/src/components/AdvisorStudentDialog.vue
- Modify: fronted/src/composables/useAdvisorDetail.ts
- Modify: fronted/src/views/advisor/AdvisorDashboardPage.vue

**Interfaces:**
- Consumes: advisor detail, guidance, writeGuidance and writeAdvice API calls.
- Produces: 详情、历史指导、指导意见、建议任务与重测建议的完整对话框。

- [ ] **Step 1: Write the failing test**

~~~ts
it('submits a reassessment advice for the selected student', async () => {
  const wrapper = mount(AdvisorStudentDialog, { props: { studentId: 'stu-1', open: true } })
  await wrapper.get('[data-test="advice-type"]').setValue('REASSESSMENT')
  await wrapper.get('form').trigger('submit')
  expect(api.advisor.writeAdvice).toHaveBeenCalledWith('stu-1', expect.objectContaining({ type: 'REASSESSMENT' }))
})
~~~

- [ ] **Step 2: Run test to verify it fails**

Run: cd fronted && npm test -- src/components/AdvisorStudentDialog.test.ts

Expected: FAIL because the advice control does not exist.

- [ ] **Step 3: Write minimal implementation**

Open the dialog by loading detail and guidance in parallel. Provide forms for guidance, recommended task and reassessment, then refresh both sources after the write succeeds.

~~~ts
const results = await Promise.all([
  api.advisor.detail(studentId),
  api.advisor.guidance(studentId),
])
~~~

- [ ] **Step 4: Run test to verify it passes**

Run: cd fronted && npm test -- src/components/AdvisorStudentDialog.test.ts

Expected: PASS.

- [ ] **Step 5: Commit**

~~~bash
git add fronted/src/components/AdvisorStudentDialog.vue fronted/src/components/AdvisorStudentDialog.test.ts fronted/src/composables/useAdvisorDetail.ts fronted/src/views/advisor/AdvisorDashboardPage.vue
git commit -m "feat: complete advisor guidance actions"
~~~

### Task 4: Complete admin domain pages and navigation

**Files:**
- Create: fronted/src/views/admin/AdminWorkbenchView.test.ts
- Modify: fronted/src/views/admin/AdminWorkbenchView.vue
- Modify: fronted/src/router/index.ts
- Modify: fronted/src/layouts/AdminLayout.vue

**Interfaces:**
- Consumes: every api.admin method and auth password reset method.
- Produces: questionnaire and prompt routes plus role-protected CRUD, imports, polling, publishing, logs and exports.

- [ ] **Step 1: Write the failing test**

~~~ts
it.each([['/admin/questionnaires', '问卷管理'], ['/admin/prompts', '提示词管理']])
('routes path through its navigation entry', (path, label) => {
  expect(router.resolve(path).matched.length).toBeGreaterThan(0)
  expect(adminMenuText()).toContain(label)
})
it('uses a named method for curriculum items', async () => {
  await mountWorkbench('curricula').get('[data-test="curriculum-items"]').trigger('click')
  expect(api.admin.curriculumItems).toHaveBeenCalled()
})
~~~

- [ ] **Step 2: Run test to verify it fails**

Run: cd fronted && npm test -- src/views/admin/AdminWorkbenchView.test.ts

Expected: FAIL because routes and named curriculum method are absent.

- [ ] **Step 3: Write minimal implementation**

Replace every raw request in AdminWorkbenchView with api.admin calls. Add questionnaire list/create/edit/status/version/detail/publish, prompt scenes/list/create/publish, curriculum job/item/version/publish, logs, model config and export download. Add routes with module props questionnaire and prompts.

- [ ] **Step 4: Run test to verify it passes**

Run: cd fronted && npm test -- src/views/admin/AdminWorkbenchView.test.ts

Expected: PASS.

- [ ] **Step 5: Commit**

~~~bash
git add fronted/src/views/admin fronted/src/router/index.ts fronted/src/layouts/AdminLayout.vue
git commit -m "feat: expose complete admin API workflows"
~~~

### Task 5: Complete AI actions and enforce coverage

**Files:**
- Create: fronted/src/views/admin/AiPlaygroundPage.test.ts
- Create: fronted/src/api/coverage.test.ts
- Modify: fronted/src/views/admin/AiPlaygroundPage.vue
- Modify: fronted/src/views/student/StudentDevelopmentPage.vue
- Modify: fronted/API_COVERAGE.md

**Interfaces:**
- Consumes: every api.ai and api.gateway action and API_COVERAGE.
- Produces: AI form/response views and a no-missing-controller-endpoint regression test.

- [ ] **Step 1: Write the failing test**

~~~ts
it('offers all AI and gateway actions', () => {
  expect(AI_ENDPOINTS.map(item => item.id)).toContain('ai-chat-history')
  expect(AI_ENDPOINTS.map(item => item.id)).toContain('ai-pdf-parse')
})
it('does not leave an endpoint without a UI entry', () => {
  expect(API_COVERAGE.every(item => item.ui.startsWith('/'))).toBe(true)
})
~~~

- [ ] **Step 2: Run test to verify it fails**

Run: cd fronted && npm test -- src/views/admin/AiPlaygroundPage.test.ts src/api/coverage.test.ts

Expected: FAIL because historical chat and coverage items are incomplete.

- [ ] **Step 3: Write minimal implementation**

Expose gateway generate/chat-completions and every AI action in the typed playground. Add student chat, history and feedback entry in the development UI. Display formatted server successes and request errors; rewrite API_COVERAGE.md as the verified controller-to-UI matrix.

- [ ] **Step 4: Run full verification**

Run: cd fronted && npm test && npm run build

Expected: all Vitest tests and the TypeScript/Vite build PASS.

- [ ] **Step 5: Commit**

~~~bash
git add fronted/API_COVERAGE.md fronted/src/api/coverage.ts fronted/src/api/coverage.test.ts fronted/src/views/admin/AiPlaygroundPage.vue fronted/src/views/admin/AiPlaygroundPage.test.ts fronted/src/views/student/StudentDevelopmentPage.vue
git commit -m "test: verify full backend API coverage"
~~~

## Plan Self-Review

- The approved design's student, advisor, admin, AI, error and coverage requirements map to Tasks 1–5.
- Every task has a red-green test cycle, exact files, interface boundary, and verification command.
- The coverage registry requires each controller mapping to be listed individually; no wildcard can conceal a missing UI.
