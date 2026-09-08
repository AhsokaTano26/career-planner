/**
 * Adapter for the Spring Boot branch (`career-core(back-end)`).
 * AI is intentionally not called from the browser: the AI service accepts an
 * internal token and must be reached through a server-side gateway.
 */
export type ApiEnvelope<T> = { code: string; message: string; data: T; traceId?: string; timestamp?: string }
export type User = { id:string; username:string; name:string; role:'STUDENT'|'ADVISOR'|'ADMIN'; studentNo?:string; grade?:string; majorCategory?:string; className?:string; consentAgreed?:boolean; passwordChangeRequired?:boolean }
export type Token = { accessToken:string; refreshToken:string; expiresIn:number; tokenType:string; firstLogin:boolean; user:User }
export type ConsentStatus = { agreed:boolean; version?:string; agreedAt?:string; currentVersion:string; currentVersionPublishedAt?:string; content?:string }
export type Experience = { id:string; type:string; title:string; startDate:string; endDate?:string; description?:string; attachmentUrl?:string }
export type Profile = { userId:string; name:string; className?:string; grade?:string; majorCategory?:string; basic?:{gender?:string;hometown?:string;birthday?:string;phone?:string}; academic?:{math?:number;english?:number;programming?:number;note?:string}; abilitySelf?:{programming?:number;math?:number;english?:number;communication?:number;organization?:number}; interestPrefs?:string[]; values?:string[]; developmentIntention?:string; constraints?:string[]; completeness?:number; experiences?:Experience[] }
export type Completeness = { score:number; total:number; filled:number; missing:{key:string;name:string}[]; dimensions:{key:string;name:string;filled:boolean;required:boolean}[] }

const base = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')
let accessToken = sessionStorage.getItem('career.access-token') ?? ''
let refreshToken = sessionStorage.getItem('career.refresh-token') ?? ''
export const setAccessToken = (token:string) => {
  accessToken = token
  token ? sessionStorage.setItem('career.access-token', token) : sessionStorage.removeItem('career.access-token')
}
export const hasAccessToken = () => Boolean(accessToken)
export const hasRefreshToken = () => Boolean(refreshToken)
export const setAuthTokens = (token:Pick<Token, 'accessToken'|'refreshToken'>) => {
  setAccessToken(token.accessToken)
  refreshToken = token.refreshToken
  refreshToken ? sessionStorage.setItem('career.refresh-token', refreshToken) : sessionStorage.removeItem('career.refresh-token')
}
export const clearAuthSession = () => {
  setAccessToken('')
  refreshToken = ''
  sessionStorage.removeItem('career.refresh-token')
  // 稳定性：token 失效即通知各单例清数据（此前只清 storage，旧号数据残留到下次守卫）
  authInvalidatedHandlers.forEach(handler => {
    try { handler() } catch { /* 单例清理不得影响请求错误路径 */ }
  })
}
// 会话失效通知（useAuth 注册 resetSession；request.ts 不反向依赖 composable，避免循环）
type AuthInvalidatedHandler = () => void
const authInvalidatedHandlers = new Set<AuthInvalidatedHandler>()
export function onAuthInvalidated(handler: AuthInvalidatedHandler) {
  authInvalidatedHandlers.add(handler)
}
export const getErrorMessage = (error:unknown) => error instanceof Error ? error.message : '请求失败，请稍后重试'

let refreshInFlight: Promise<boolean> | null = null
async function renewAccessToken():Promise<boolean> {
  if (!refreshToken) return false
  if (refreshInFlight) return refreshInFlight
  refreshInFlight = (async () => {
    try {
      const response = await fetch(`${base}/auth/refresh`, {
        method:'POST', credentials:'include', headers:{'Content-Type':'application/json', 'X-Request-Id':crypto.randomUUID()},
        body:JSON.stringify({ refreshToken }),
      })
      const body = await response.json().catch(() => null) as ApiEnvelope<Token> | null
      if (!response.ok || !body || body.code !== 'OK') { clearAuthSession(); return false }
      setAuthTokens(body.data)
      return true
    } catch {
      // 稳定性：网络异常同样清理过期 token（此前只清 HTTP 失败分支，过期 token 常驻导致每次 401 都重试 refresh）
      clearAuthSession()
      return false
    } finally { refreshInFlight = null }
  })()
  return refreshInFlight
}

export async function request<T>(path:string, init:RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  headers.set('X-Request-Id', crypto.randomUUID())
  const method = (init.method || 'GET').toUpperCase()
  // Every write is replay-safe on the backend. Preserve this key if a 401 refresh
  // causes the request to be retried, so one user action remains one operation.
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) && !headers.has('Idempotency-Key')) {
    headers.set('Idempotency-Key', crypto.randomUUID())
  }
  // 稳定性：默认 60s 超时（AI 类长调用由调用方经 signal 覆盖）；超时错误可被 catch 区分展示
  const externalSignal = init.signal ?? null
  const controller = new AbortController()
  const onExternalAbort = () => controller.abort(externalSignal?.reason)
  if (externalSignal) {
    if (externalSignal.aborted) controller.abort(externalSignal.reason)
    else externalSignal.addEventListener('abort', onExternalAbort, { once: true })
  }
  const timer = setTimeout(() => controller.abort(new Error('请求超时，请稍后重试')), 60000)
  const requestInit:RequestInit = { ...init, headers, credentials:'include', signal: controller.signal }
  let response: Response
  try {
    response = await fetch(`${base}${path}`, requestInit)
  } catch (e) {
    if (externalSignal) externalSignal.removeEventListener('abort', onExternalAbort)
    clearTimeout(timer)
    throw e instanceof Error ? e : new Error('网络请求失败，请检查网络后重试')
  }
  clearTimeout(timer)
  if (externalSignal) externalSignal.removeEventListener('abort', onExternalAbort)
  const body = await response.json().catch(() => null) as ApiEnvelope<T> | null
  // 复审 Batch4：重试传原始 init（此前传已消耗的 requestInit，其 signal 已绑定旧 controller，
  // 刷新瞬间点的取消/超时会丢失；init 里的 headers 若已有 Idempotency-Key 则保持同一键）。
  if (response.status === 401 && path !== '/auth/refresh' && await renewAccessToken()) return request<T>(path, init)
  if (!response.ok || !body || body.code !== 'OK') {
    if (response.status === 401) clearAuthSession()
    throw new Error(body?.message || `请求失败（HTTP ${response.status}）`)
  }
  return body.data
}

/** 带超时 + 外部取消的 fetch（复审 Batch4：downloadFile/postRaw 此前零超时可永久 hanging）。 */
async function fetchWithTimeout(url: string, init: RequestInit, timeoutMs: number): Promise<Response> {
  const externalSignal = init.signal ?? null
  const controller = new AbortController()
  const onExternalAbort = () => controller.abort(externalSignal?.reason)
  if (externalSignal) {
    if (externalSignal.aborted) controller.abort(externalSignal.reason)
    else externalSignal.addEventListener('abort', onExternalAbort, { once: true })
  }
  const timer = setTimeout(() => controller.abort(new Error('请求超时，请稍后重试')), timeoutMs)
  try {
    return await fetch(url, { ...init, signal: controller.signal })
  } finally {
    clearTimeout(timer)
    if (externalSignal) externalSignal.removeEventListener('abort', onExternalAbort)
  }
}

export async function downloadFile(path:string, timeoutMs = 120000):Promise<{blob:Blob; filename:string}> {
  const headers = new Headers()
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  headers.set('X-Request-Id', crypto.randomUUID())
  let response = await fetchWithTimeout(`${base}${path}`, { headers, credentials:'include' }, timeoutMs)
  if (response.status === 401 && await renewAccessToken()) {
    headers.set('Authorization', `Bearer ${accessToken}`)
    response = await fetchWithTimeout(`${base}${path}`, { headers, credentials:'include' }, timeoutMs)
  }
  if (!response.ok) {
    if (response.status === 401) clearAuthSession()
    throw new Error(`下载失败（HTTP ${response.status}）`)
  }
  const disposition = response.headers.get('Content-Disposition') || ''
  const filename = /filename="?([^";]+)"?/i.exec(disposition)?.[1] || 'export.csv'
  return { blob: await response.blob(), filename: decodeURIComponent(filename) }
}
const get = <T>(path:string) => request<T>(path)
const post = <T>(path:string, data?:unknown) => request<T>(path, { method:'POST', body:data === undefined ? undefined : JSON.stringify(data) })
const postRaw = async <T>(path:string, data:unknown, timeoutMs = 120000):Promise<T> => {
  const headers = new Headers({'Content-Type':'application/json', 'X-Request-Id':crypto.randomUUID()})
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  let response = await fetchWithTimeout(`${base}${path}`, { method:'POST', headers, credentials:'include', body:JSON.stringify(data) }, timeoutMs)
  // 稳定性：过期 access 自愈一次（此前直接抛 401；响应体为网关原生 JSON，不做信封校验）
  if (response.status === 401 && await renewAccessToken()) {
    headers.set('Authorization', `Bearer ${accessToken}`)
    response = await fetchWithTimeout(`${base}${path}`, { method:'POST', headers, credentials:'include', body:JSON.stringify(data) }, timeoutMs)
  }
  if (!response.ok) {
    if (response.status === 401) clearAuthSession()
    throw new Error(`请求失败（HTTP ${response.status}）`)
  }
  return response.json() as Promise<T>
}
const patch = <T>(path:string, data:unknown) => request<T>(path, { method:'PATCH', body:JSON.stringify(data) })
const del = <T>(path:string) => request<T>(path, { method:'DELETE' })
const formPost = <T>(path:string, data:FormData) => request<T>(path, { method:'POST', body:data })

export function toQuery(params: Record<string, unknown>): string {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
  })
  return query.toString()
}

export const api = {
  auth: {
    login: (data:{account:string;password:string;role?:string}) => post<Token>('/auth/login', data),
    register: (data:{studentNo:string;name:string;className?:string;initialPassword:string}) => post<Token>('/auth/register', data),
    me: () => get<User>('/auth/me'), logout: () => post<void>('/auth/logout'),
    updateMe: (data:{name:string}) => patch<User>('/auth/me', data),
    resetPassword:(data:unknown)=>post<void>('/auth/password/reset',data),
    changePassword: (data:{oldPassword:string;newPassword:string}) => patch<void>('/auth/me/password', data),
    consentStatus: () => get<ConsentStatus>('/auth/privacy-consent/status'),
    consent: (data:{version:string}) => post<ConsentStatus>('/auth/privacy-consent', data),
  },
  student: {
    me: () => get<Profile>('/students/me'), update: (data:Partial<Profile>) => patch<Profile>('/students/me', data),
    completeness: () => get<Completeness>('/students/me/completeness'), experiences: () => get<Experience[]>('/students/me/experiences?size=100'),
    addExperience: (data:Omit<Experience,'id'|'attachmentUrl'> & {attachment?:string}) => post<Experience>('/students/me/experiences', data),
    updateExperience: (id:string, data:Omit<Experience,'id'|'attachmentUrl'> & {attachment?:string}) => patch<Experience>(`/students/me/experiences/${id}`, data),
    deleteExperience: (id:string) => del<void>(`/students/me/experiences/${id}`),
    requestDeletion: (reason:string) => post<void>('/students/me/deletion-request', { reason }),
    questionnaires:()=>get<unknown[]>('/questionnaires'), questionnaire:(id:string)=>get<unknown>(`/questionnaires/${id}`), questionnaireVersions:(id:string)=>get<unknown[]>(`/questionnaires/${id}/versions`),
    assessmentSessions:()=>get<unknown[]>('/assessment-sessions'), assessmentSession:(id:string)=>get<unknown>(`/assessment-sessions/${id}`), createAssessment:(data:{questionnaireId:string;resumeSessionId?:string})=>post<unknown>('/assessment-sessions',data), saveAssessment:(id:string,data:{requestId?:string;answers:unknown[];finished?:boolean})=>request<void>(`/assessment-sessions/${id}/answers`,{method:'PUT',body:JSON.stringify(data)}), submitAssessment:(id:string)=>post<unknown>(`/assessment-sessions/${id}/submit`), assessmentScores:(id:string)=>get<unknown>(`/assessment-sessions/${id}/scores`),
    latestPortrait:()=>get<unknown>('/students/me/profile/latest'), refreshPortrait:()=>post<unknown>('/students/me/profile/refresh'), portraitVersions:()=>get<unknown[]>('/students/me/profile/versions'), portraitSnapshot:(id:string)=>get<unknown>(`/profile-snapshots/${id}`), portraitFeedback:(id:string,data:{feedbackType:string;comment?:string})=>post<unknown>(`/profile-snapshots/${id}/feedback`,data),
    recommendationLatest:()=>get<unknown>('/students/me/recommendations/latest'), recommendationRuns:(page=1,size=20)=>get<unknown>(`/students/me/recommendations?page=${page}&size=${size}`), createRecommendation:(data:{pathFilter?:string;requestId?:string})=>post<unknown>('/students/me/recommendations/runs',data), recommendationDetail:(id:string)=>get<unknown>(`/recommendation-runs/${id}`), recommendationFeedback:(id:string,data:{feedbackType:string;comment?:string})=>post<unknown>(`/recommendation-results/${id}/feedback`,data),
    directions:(path?:string)=>get<unknown[]>(`/students/me/directions${path?`?path=${encodeURIComponent(path)}`:''}`), direction:(id:string)=>get<unknown>(`/students/me/directions/${id}`), favorites:()=>get<unknown[]>('/students/me/favorites'), addFavorite:(id:string)=>post<unknown>(`/students/me/favorites/${id}`), removeFavorite:(id:string)=>del<unknown>(`/students/me/favorites/${id}`),
    goals:()=>get<unknown>('/students/me/goals'), saveGoals:(data:{primaryDirectionId?:string;backupDirectionId?:string;changeReason?:string})=>post<unknown>('/students/me/goals',data), updateGoals:(data:{primaryDirectionId?:string;backupDirectionId?:string;changeReason?:string})=>request<unknown>('/students/me/goals',{method:'PUT',body:JSON.stringify(data)}), goalVersions:()=>get<unknown[]>('/students/me/goals/versions'),
    latestPlan:()=>get<unknown>('/students/me/plans/latest'), plans:()=>get<unknown[]>('/students/me/plans'), plan:(id:string)=>get<unknown>(`/students/me/plans/${id}`), draftPlan:(data:{directionId?:string;useAi?:boolean;requestId?:string})=>post<unknown>('/students/me/plans/draft',data), confirmPlan:(data:{confirm:boolean})=>post<unknown>('/students/me/plans/confirm',data), updatePlan:(data:unknown)=>request<unknown>('/students/me/plans',{method:'PUT',body:JSON.stringify(data)}),
    tasks:(params:{month?:string;status?:string;page?:number;size?:number}={})=>get<unknown>(`/students/me/tasks?${toQuery({page:1,size:20,...params})}`), task:(id:string)=>get<unknown>(`/students/me/tasks/${id}`), createTask:(data:unknown)=>post<unknown>('/students/me/tasks',data), updateTask:(id:string,data:unknown)=>request<unknown>(`/students/me/tasks/${id}`,{method:'PUT',body:JSON.stringify(data)}), checkinTask:(id:string,data:unknown)=>post<unknown>(`/students/me/tasks/${id}/checkin`,data),
    reviews:()=>get<unknown[]>('/reviews'), review:(id:string)=>get<unknown>(`/reviews/${id}`), createReview:(data:unknown)=>post<unknown>('/reviews/drafts',data), updateReview:(id:string,data:unknown)=>request<unknown>(`/reviews/${id}/draft`,{method:'PUT',body:JSON.stringify(data)}), submitReview:(id:string)=>post<unknown>(`/reviews/${id}/submit`), summarizeReview:(id:string)=>post<unknown>(`/reviews/${id}/ai-summary`), adoptAdvice:(id:string,data:{adopt:boolean})=>post<unknown>(`/reviews/${id}/adopt-advice`,data), requestGuidance:(id:string,data:{message?:string})=>post<unknown>(`/reviews/${id}/guidance-request`,data),
    reminders:(params:{unreadOnly?:boolean;page?:number;size?:number}={})=>get<unknown>(`/students/me/reminders?${toQuery({unreadOnly:false,page:1,size:20,...params})}`), unreadReminderCount:()=>get<{count:number}>('/students/me/reminders/unread-count'), markReminderRead:(id:string)=>post<unknown>(`/students/me/reminders/${id}/read`), generateReminders:()=>post<unknown[]>('/students/me/reminders/generate'),
  },
  advisor: {
    statistics: () => get<unknown>('/advisor/statistics'), attention: () => get<unknown[]>('/advisor/attention'),
    students: (query = '') => get<unknown>(`/advisor/students${query ? `?${query}` : ''}`), detail:(id:string) => get<unknown>(`/advisor/students/${id}`),
    guidance:(id:string) => get<unknown[]>(`/advisor/students/${id}/guidance`),
    writeGuidance:(id:string,data:unknown) => post<unknown>(`/advisor/students/${id}/guidance`,data),
    writeAdvice:(id:string,data:unknown) => post<unknown>(`/advisor/students/${id}/advice`,data),
  },
  admin: {
    users:(params:Record<string,unknown>={})=>get<unknown>(`/admin/users?${toQuery({page:1,size:20,...params})}`),
    whitelist:(params:Record<string,unknown>={})=>get<unknown>(`/admin/whitelist?${toQuery({page:1,size:20,...params})}`),
    relations:(params:Record<string,unknown>={})=>get<unknown>(`/admin/relations?${toQuery({page:1,size:20,...params})}`),
    directions:(params:Record<string,unknown>={})=>get<unknown>(`/admin/directions?${toQuery({page:1,size:20,...params})}`),
    abilities:(params:Record<string,unknown>={})=>get<unknown>(`/admin/abilities?${toQuery({page:1,size:20,...params})}`),
    templates:(params:Record<string,unknown>={})=>get<unknown>(`/admin/templates?${toQuery({page:1,size:20,...params})}`),
    exports:(params:Record<string,unknown>={})=>get<unknown>(`/admin/exports?${toQuery({page:1,size:20,...params})}`),
    aiLogs:(params:Record<string,unknown>={})=>get<unknown>(`/admin/logs/ai?${toQuery({page:1,size:20,...params})}`),
    operationLogs:(params:Record<string,unknown>={})=>get<unknown>(`/admin/logs/operations?${toQuery({page:1,size:20,...params})}`),
    curriculumJobs:(params:Record<string,unknown>={})=>get<unknown>(`/admin/curricula/jobs?${toQuery({page:1,size:20,...params})}`),
    curriculumItems:(params:Record<string,unknown>)=>get<unknown>(`/admin/curricula/items?${toQuery({page:1,size:20,...params})}`),
    curriculumVersions:(params:Record<string,unknown>={})=>get<unknown>(`/admin/curricula/versions?${toQuery({page:1,size:20,...params})}`),
    weights: () => get<unknown>('/admin/weights'),
    createWhitelist: (data:unknown) => post<unknown>('/admin/whitelist', data), createRelations: (data:unknown) => post<unknown>('/admin/relations', data),
    createDirection: (data:unknown) => post<unknown>('/admin/directions', data), createAbility: (data:unknown) => post<unknown>('/admin/abilities', data),
    createTemplate: (data:unknown) => post<unknown>('/admin/templates', data), createWeights: (data:unknown) => post<unknown>('/admin/weights', data), createExport: (data:unknown) => post<unknown>('/admin/exports', data),
    updateUser:(id:string,data:unknown)=>patch<void>(`/admin/users/${id}`,data), deleteWhitelist:(id:string)=>del<void>(`/admin/whitelist/${id}`), deleteRelation:(id:string)=>del<void>(`/admin/relations/${id}`),
    updateDirection:(id:string,data:unknown)=>patch<unknown>(`/admin/directions/${id}`,data), updateAbility:(id:string,data:unknown)=>patch<unknown>(`/admin/abilities/${id}`,data), updateTemplate:(id:string,data:unknown)=>patch<unknown>(`/admin/templates/${id}`,data),
    setDirectionStatus:(id:string,data:unknown)=>patch<unknown>(`/admin/directions/${id}/status`,data),
    curriculumJob:(id:string)=>get<unknown>(`/admin/curricula/jobs/${id}`),
    importWhitelist:(file:File)=>{const data=new FormData();data.append('file',file);return formPost<unknown>('/admin/whitelist/import',data)}, importCurriculum:(file:File)=>{const data=new FormData();data.append('file',file);return formPost<unknown>('/admin/curricula/import',data)},
    reviewCurriculumItem:(id:string,data:unknown)=>patch<unknown>(`/admin/curricula/items/${id}`,data), batchReviewCurriculum:(data:unknown)=>post<unknown>('/admin/curricula/items/batch',data), publishCurriculum:(data:unknown)=>post<unknown>('/admin/curricula/publish',data),
    modelConfigs:()=>get<unknown[]>('/admin/model-configs'), updateModelConfig:(key:string,data:{configValue:string})=>request<unknown>(`/admin/model-configs/${key}`,{method:'PUT',body:JSON.stringify(data)}),
    promptScenes:()=>get<unknown[]>('/admin/prompts/scenes'), prompts:(scene?:string)=>get<unknown[]>(`/admin/prompts${scene?`?scene=${encodeURIComponent(scene)}`:''}`), createPrompt:(data:{scene:string;version:string;content:string})=>post<unknown>('/admin/prompts',data), publishPrompt:(id:string)=>post<unknown>(`/admin/prompts/${id}/publish`),
    questionnaires:(params:{keyword?:string;page?:number;size?:number}={})=>get<unknown>(`/admin/questionnaires?${toQuery({page:1,size:20,...params})}`), createQuestionnaire:(data:unknown)=>post<unknown>('/admin/questionnaires',data), updateQuestionnaire:(id:string,data:unknown)=>patch<unknown>(`/admin/questionnaires/${id}`,data), updateQuestionnaireStatus:(id:string,data:{status:string})=>patch<unknown>(`/admin/questionnaires/${id}/status`,data), questionnaireVersions:(id:string)=>get<unknown[]>(`/admin/questionnaires/${id}/versions`), questionnaireVersion:(qid:string,vid:string)=>get<unknown>(`/admin/questionnaires/${qid}/versions/${vid}`), createQuestionnaireVersion:(id:string,data:unknown)=>post<unknown>(`/admin/questionnaires/${id}/versions`,data), publishQuestionnaireVersion:(qid:string,vid:string)=>post<unknown>(`/admin/questionnaires/${qid}/versions/${vid}/publish`),
  },
  gateway: {
    generate: (data:unknown) => post<unknown>('/gateway/generate', data),
    chatCompletions: (data:unknown) => postRaw<unknown>('/gateway/chat/completions', data),
  },
  ai: {
    chat: (data:unknown) => post<unknown>('/ai/chat', data),
    chatHistory: (params:{page?:number;size?:number}={}) => get<unknown>(`/ai/chat/history?page=${params.page ?? 1}&size=${params.size ?? 20}`),
    chatFeedback: (id:string,data:unknown) => post<unknown>(`/ai/chat/${id}/feedback`,data),
    chatFeedbackFallback: (data:unknown) => post<unknown>('/ai/chat/feedback',data),
    reviewSummarize:(data:unknown)=>post<unknown>('/ai/review/summarize',data),
    recommendationExplain:(data:unknown)=>post<unknown>('/ai/recommendation/explain',data),
    planGenerate:(data:unknown)=>post<unknown>('/ai/plan/generate',data),
    pdfParse:(data:unknown)=>post<unknown>('/ai/pdf/parse',data),
  },
}
