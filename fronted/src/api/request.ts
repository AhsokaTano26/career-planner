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
    } catch { return false } finally { refreshInFlight = null }
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
  const requestInit:RequestInit = { ...init, headers, credentials:'include' }
  const response = await fetch(`${base}${path}`, requestInit)
  const body = await response.json().catch(() => null) as ApiEnvelope<T> | null
  if (response.status === 401 && path !== '/auth/refresh' && await renewAccessToken()) return request<T>(path, requestInit)
  if (!response.ok || !body || body.code !== 'OK') {
    if (response.status === 401) clearAuthSession()
    throw new Error(body?.message || `请求失败（HTTP ${response.status}）`)
  }
  return body.data
}

export async function downloadFile(path:string):Promise<{blob:Blob; filename:string}> {
  const headers = new Headers()
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  headers.set('X-Request-Id', crypto.randomUUID())
  let response = await fetch(`${base}${path}`, { headers, credentials:'include' })
  if (response.status === 401 && await renewAccessToken()) {
    headers.set('Authorization', `Bearer ${accessToken}`)
    response = await fetch(`${base}${path}`, { headers, credentials:'include' })
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
const patch = <T>(path:string, data:unknown) => request<T>(path, { method:'PATCH', body:JSON.stringify(data) })
const del = <T>(path:string) => request<T>(path, { method:'DELETE' })
const formPost = <T>(path:string, data:FormData) => request<T>(path, { method:'POST', body:data })

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
  },
  advisor: {
    statistics: () => get<unknown>('/advisor/statistics'), attention: () => get<unknown[]>('/advisor/attention'),
    students: (query = '') => get<unknown>(`/advisor/students${query ? `?${query}` : ''}`), detail:(id:string) => get<unknown>(`/advisor/students/${id}`),
    guidance:(id:string) => get<unknown[]>(`/advisor/students/${id}/guidance`),
    writeGuidance:(id:string,data:unknown) => post<unknown>(`/advisor/students/${id}/guidance`,data),
    writeAdvice:(id:string,data:unknown) => post<unknown>(`/advisor/students/${id}/advice`,data),
  },
  admin: {
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
  },
}
