import { ref } from 'vue'
import { api, getErrorMessage } from '../api/request'
import type { AdvisorDetail, Guidance } from '../types/domain'
import { onSessionReset } from './useAuth'
import { useToast } from './useToast'

export type GuidancePayload = { content: string; adviceType: Guidance['adviceType']; suggestedTask?: string; retestReason?: string }

const student = ref<AdvisorDetail | null>(null)
const guidanceSaving = ref(false)

onSessionReset(() => {
  student.value = null
  guidanceSaving.value = false
})

export function useAdvisorDetail() {
  const { show: notice } = useToast()

  async function open(id: string) {
    try {
      const [detail, guidance] = await Promise.all([api.advisor.detail(id), api.advisor.guidance(id)])
      student.value = { id, detail, guidance: guidance as Guidance[] }
    } catch (e) {
      notice(getErrorMessage(e))
    }
  }

  async function sendGuidance(payload: GuidancePayload) {
    const detail = student.value
    if (!detail) return
    guidanceSaving.value = true
    try {
      const action = payload.adviceType === 'COMMENT' ? api.advisor.writeGuidance : api.advisor.writeAdvice
      const item = await action(detail.id, payload) as Guidance
      detail.guidance.push(item)
      notice('指导已发送并保存')
    } catch (e) {
      notice(getErrorMessage(e))
    } finally {
      guidanceSaving.value = false
    }
  }

  function close() { student.value = null }

  return { student, guidanceSaving, open, sendGuidance, close }
}
