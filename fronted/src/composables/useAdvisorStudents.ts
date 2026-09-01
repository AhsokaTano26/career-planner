import { onMounted, ref, watch } from 'vue'
import { api, getErrorMessage } from '../api/request'
import type { AdvisorFilters, AdvisorStudent } from '../types/domain'
import { useAuth } from './useAuth'

// Module-level singleton state shared by the student list and guidance pages.
const students = ref<AdvisorStudent[]>([])
const filters = ref<AdvisorFilters>({ keyword: '', path: '', goalStatus: '', reviewStatus: '', guidanceRequested: '', sort: '-createdAt' })
const total = ref(0)
const page = ref(1)
const totalPages = ref(1)
const loading = ref(false)
const error = ref('')

const EMPTY_FILTERS: AdvisorFilters = { keyword: '', path: '', goalStatus: '', reviewStatus: '', guidanceRequested: '', sort: '-createdAt' }

export function useAdvisorStudents() {
  const auth = useAuth()

  function query(current = page.value) {
    const params = new URLSearchParams({ page: String(current), size: '20', sort: filters.value.sort || '-createdAt' })
    Object.entries(filters.value).forEach(([key, value]) => {
      if (key !== 'sort' && value) params.set(key, value)
    })
    return params.toString()
  }

  async function load(current?: number) {
    if (auth.forcePasswordChange.value) return
    const target = current ?? page.value
    loading.value = true
    error.value = ''
    try {
      const data = await api.advisor.students(query(target)) as { list?: AdvisorStudent[]; total?: number; page?: number; totalPages?: number }
      students.value = data.list || []
      total.value = data.total || 0
      page.value = data.page ?? target
      totalPages.value = Math.max(data.totalPages || 1, 1)
    } catch (e) {
      error.value = getErrorMessage(e)
    } finally {
      loading.value = false
    }
  }

  function updateFilters(next: AdvisorFilters) { filters.value = next }
  function apply() { load(1) }
  function reset() { filters.value = { ...EMPTY_FILTERS }; load(1) }

  onMounted(() => { if (auth.role.value === 'ADVISOR') load() })
  watch(() => auth.forcePasswordChange.value, (forced) => {
    if (!forced && auth.loggedIn.value) load()
  })

  return { students, filters, total, page, totalPages, loading, error, load, updateFilters, apply, reset }
}
