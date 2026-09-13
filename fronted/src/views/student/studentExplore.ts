/**
 * 生涯探索前端状态机（纯函数，便于单测）。
 *
 * 配额口径（与需求 FR、后端 ExploreService 一致）：
 *  - 路径卡 1 次 + 兴趣标签 ≤3 次 + AI 深挖 ≤6 条 = 学生主动输入合计 ≤10；
 *  - 每个标签作答 = 该标签题选 optionIndex 0；probe 答案为选项下标。
 */

export const TOTAL_INPUT_LIMIT = 10
export const TAG_LIMIT = 3
export const PROBE_LIMIT = 6

export const PATH_OPTIONS = [
  { value: 'graduate', label: '国内升学' },
  { value: 'employment', label: '就业发展' },
  { value: 'overseas', label: '出国留学' },
  { value: 'undecided', label: '还没想好' },
] as const

export type PathValue = (typeof PATH_OPTIONS)[number]['value']
export type ExploreAnswer = { questionId: string; optionIndex: number }

export type ExploreState = {
  path: PathValue | ''
  tagIds: readonly string[]
  probeAnswers: readonly ExploreAnswer[]
}

const ALLOWED_PATHS: readonly string[] = PATH_OPTIONS.map((o) => o.value)

export function emptyState(): ExploreState {
  return { path: '', tagIds: [], probeAnswers: [] }
}

/** 已消耗的主动输入次数（选了路径即 1 次）。 */
export function usedInputs(state: ExploreState): number {
  return (state.path ? 1 : 0) + state.tagIds.length + state.probeAnswers.length
}

/** 剩余可主动输入次数（≥0）。 */
export function remainingInputs(state: ExploreState): number {
  return Math.max(0, TOTAL_INPUT_LIMIT - usedInputs(state))
}

export function tagsRemaining(state: ExploreState): number {
  return Math.max(0, TAG_LIMIT - state.tagIds.length)
}

export function probesRemaining(state: ExploreState): number {
  return Math.max(0, PROBE_LIMIT - state.probeAnswers.length)
}

export function selectPath(state: ExploreState, path: PathValue | ''): ExploreState {
  if (path !== '' && !ALLOWED_PATHS.includes(path)) {
    return state
  }
  return { ...state, path }
}

/** 点选/取消兴趣标签：重复忽略，超过 3 个忽略。 */
export function toggleTag(state: ExploreState, tagId: string): ExploreState {
  if (state.tagIds.includes(tagId)) {
    return { ...state, tagIds: state.tagIds.filter((id) => id !== tagId) }
  }
  if (state.tagIds.length >= TAG_LIMIT) {
    return state
  }
  return { ...state, tagIds: [...state.tagIds, tagId] }
}

/** 追加 AI 深挖作答：同题替换旧值；超过 6 条忽略（配额由后端兜底）。 */
export function addProbeAnswer(state: ExploreState, questionId: string, optionIndex: number): ExploreState {
  const existing = state.probeAnswers.findIndex((a) => a.questionId === questionId)
  const next = { questionId, optionIndex }
  if (existing >= 0) {
    const probeAnswers = state.probeAnswers.map((a, i) => (i === existing ? next : a))
    return { ...state, probeAnswers }
  }
  if (state.probeAnswers.length >= PROBE_LIMIT) {
    return state
  }
  return { ...state, probeAnswers: [...state.probeAnswers, next] }
}

/** 汇总结交答案：已点标签（每题 optionIndex=0）在前，probe 答案在后。 */
export function buildAnswers(state: ExploreState): ExploreAnswer[] {
  const tagAnswers = state.tagIds.map((questionId) => ({ questionId, optionIndex: 0 }))
  return [...tagAnswers, ...state.probeAnswers]
}

/** 组装聚合端点请求体；未选路径时不携带 path。 */
export function buildExplorationRequest(state: ExploreState): { path?: string; answers: ExploreAnswer[] } {
  const req: { path?: string; answers: ExploreAnswer[] } = { answers: buildAnswers(state) }
  if (state.path) {
    req.path = state.path
  }
  return req
}
