import { onMounted, ref, watch } from 'vue'
import { api, getErrorMessage } from '../api/request'
import type { AdvisorFilters, AdvisorStudent } from '../types/domain'
import { resolvePage } from '../utils/pagination'
import { onSessionReset, useAuth } from './useAuth'

export type DirectionOption = { id: string; name: string }

// Module-level singleton state shared by the student list and guidance pages.
const students = ref<AdvisorStudent[]>([])
const filters = ref<AdvisorFilters>({ keyword: '', path: '', directionId: '', goalStatus: '', reviewStatus: '', guidanceRequested: undefined, sort: '-createdAt' })
const total = ref(0)
const page = ref(1)
const totalPages = ref(1)
const loading = ref(false)
const error = ref('')
// G2：已发布方向下拉选项（两页复用；顾问可调学生端方向列表）。
const directions = ref<DirectionOption[]>([])
const directionsError = ref('')
let loadSeq = 0

const EMPTY_FILTERS: AdvisorFilters = { keyword: '', path: '', directionId: '', goalStatus: '', reviewStatus: '', guidanceRequested: undefined, sort: '-createdAt' }

onSessionReset(() => {
  students.value = []
  filters.value = { ...EMPTY_FILTERS }
  total.value = 0
  page.value = 1
  totalPages.value = 1
  loading.value = false
  error.value = ''
  directions.value = []
  directionsError.value = ''
})

export function useAdvisorStudents() {
  const auth = useAuth()

  function query(current = page.value) {
    const params = new URLSearchParams({ page: String(current), size: '20', sort: filters.value.sort || '-createdAt' })
    Object.entries(filters.value).forEach(([key, value]) => {
      // G3：boolean false 必须透传（仅跳过空串/undefined），否则“未申请指导”筛选项失效
      if (key !== 'sort' && value !== '' && value !== undefined) params.set(key, String(value))
    })
    return params.toString()
  }

  async function load(current?: number) {
    if (auth.forcePasswordChange.value) return
    const target = current ?? page.value
    // 稳定性：序号防卫，慢响应不得覆盖新筛选/翻页的结果
    const seq = ++loadSeq
    loading.value = true
    error.value = ''
    try {
      const data = await api.advisor.students(query(target)) as { list?: AdvisorStudent[]; total?: number; page?: number; totalPages?: number }
      if (seq !== loadSeq) return
      students.value = data.list || []
      total.value = data.total || 0
      // 复审 Batch4：分页钳制——筛选收敛后服务端总页收缩，旧大页码回落到末页重拉一次，
      // 避免“有总数无数据”误导；末页重拉仍空则接受空列表。
      const resolved = resolvePage(target, data.page ?? target, data.totalPages || 1)
      totalPages.value = Math.max(data.totalPages || 1, 1)
      if (resolved.refetch) {
        page.value = resolved.page
        load(resolved.page)
        return
      }
      page.value = resolved.page
    } catch (e) {
      if (seq !== loadSeq) return
      error.value = getErrorMessage(e)
    } finally {
      if (seq === loadSeq) loading.value = false
    }
  }

  function updateFilters(next: AdvisorFilters) { filters.value = next }
  function apply() { load(1) }
  function reset() { filters.value = { ...EMPTY_FILTERS }; load(1) }

  async function loadDirections() {
    if (directions.value.length || directionsError.value) return
    try {
      directions.value = await api.student.directions() as DirectionOption[]
    } catch (e) {
      directionsError.value = getErrorMessage(e)
    }
  }

  // G3：指导申请三态映射到 boolean/undefined（契约 type:boolean；undefined=不传参）
  function setGuidanceRequested(value: string) {
    filters.value = { ...filters.value, guidanceRequested: value === '' ? undefined : value === 'true' }
  }

  onMounted(() => { if (auth.role.value === 'ADVISOR') { load(); loadDirections() } })
  watch(() => auth.forcePasswordChange.value, (forced) => {
    if (!forced && auth.loggedIn.value) { load(); loadDirections() }
  })

  return { students, filters, total, page, totalPages, loading, error, load, updateFilters, apply, reset, directions, directionsError, loadDirections, setGuidanceRequested }
}
