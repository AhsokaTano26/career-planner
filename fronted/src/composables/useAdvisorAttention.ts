import { onMounted, ref } from 'vue'
import { api, getErrorMessage } from '../api/request'
import type { AdvisorAttention } from '../types/domain'
import { onSessionReset, useAuth } from './useAuth'

const items = ref<AdvisorAttention[]>([])
const loading = ref(false)
const error = ref('')
let loadSeq = 0

onSessionReset(() => {
  items.value = []
  loading.value = false
  error.value = ''
})

export function useAdvisorAttention() {
  const auth = useAuth()

  async function load() {
    // 稳定性：序号防卫（与 useAdvisorStudents 同模式）
    const seq = ++loadSeq
    loading.value = true
    error.value = ''
    try {
      const data = await api.advisor.attention() as AdvisorAttention[]
      if (seq !== loadSeq) return
      items.value = data
    } catch (e) {
      if (seq !== loadSeq) return
      error.value = getErrorMessage(e)
    } finally {
      if (seq === loadSeq) loading.value = false
    }
  }

  onMounted(() => { if (auth.role.value === 'ADVISOR') load() })

  return { items, loading, error, load }
}
