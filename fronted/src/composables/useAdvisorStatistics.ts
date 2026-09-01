import { onMounted, ref, watch } from 'vue'
import { api, getErrorMessage } from '../api/request'
import type { AdvisorStatistics } from '../types/domain'
import { onSessionReset, useAuth } from './useAuth'

const statistics = ref<AdvisorStatistics | null>(null)
const loading = ref(false)
const error = ref('')

onSessionReset(() => {
  statistics.value = null
  loading.value = false
  error.value = ''
})

export function useAdvisorStatistics() {
  const auth = useAuth()

  async function load() {
    if (auth.forcePasswordChange.value) return
    loading.value = true
    error.value = ''
    try {
      statistics.value = await api.advisor.statistics() as AdvisorStatistics
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

  return { statistics, loading, error, load }
}
