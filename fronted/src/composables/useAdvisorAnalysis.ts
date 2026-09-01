import { onMounted, ref, watch } from 'vue'
import { api, getErrorMessage } from '../api/request'
import type { AdvisorStudent } from '../types/domain'
import { onSessionReset, useAuth } from './useAuth'

const students = ref<AdvisorStudent[]>([])
const loading = ref(false)
const error = ref('')

onSessionReset(() => {
  students.value = []
  loading.value = false
  error.value = ''
})

export function useAdvisorAnalysis() {
  const auth = useAuth()

  /** Fetch every page of students (page size 100) for the aggregate statistics page. */
  async function load() {
    if (auth.forcePasswordChange.value) return
    loading.value = true
    error.value = ''
    try {
      const all: AdvisorStudent[] = []
      let current = 1
      let totalPages = 1
      do {
        const data = await api.advisor.students(`page=${current}&size=100&sort=name`) as { list?: AdvisorStudent[]; totalPages?: number }
        all.push(...(data.list || []))
        totalPages = Math.max(data.totalPages || 1, 1)
        current++
      } while (current <= totalPages)
      students.value = all
    } catch (e) {
      error.value = getErrorMessage(e)
    } finally {
      loading.value = false
    }
  }

  onMounted(() => { if (auth.role.value === 'ADVISOR') load() })
  watch(() => auth.forcePasswordChange.value, (forced) => {
    if (!forced && auth.loggedIn.value) load()
  })

  return { students, loading, error, load }
}
