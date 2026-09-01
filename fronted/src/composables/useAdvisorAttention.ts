import { onMounted, ref, watch } from 'vue'
import { api, getErrorMessage } from '../api/request'
import type { AdvisorAttention } from '../types/domain'
import { useAuth } from './useAuth'

const items = ref<AdvisorAttention[]>([])
const loading = ref(false)
const error = ref('')

export function useAdvisorAttention() {
  const auth = useAuth()

  async function load() {
    if (auth.forcePasswordChange.value) return
    loading.value = true
    error.value = ''
    try {
      items.value = await api.advisor.attention() as AdvisorAttention[]
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

  return { items, loading, error, load }
}
