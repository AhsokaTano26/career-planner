import { ref } from 'vue'
import { api, getErrorMessage } from '../api/request'
import type { AdvisorDetail, Guidance, StudentDetailView } from '../types/domain'
import { onSessionReset } from './useAuth'
import { useToast } from './useToast'

export type GuidancePayload = { content: string; adviceType: Guidance['adviceType']; suggestedTask?: string; retestReason?: string }

const student = ref<AdvisorDetail | null>(null)
const guidanceSaving = ref(false)
// 稳定性：详情并发序号，旧学生慢响应不得覆盖新学生（页面另有 id 比对兜底）
let openSeq = 0

onSessionReset(() => {
  student.value = null
  guidanceSaving.value = false
})

export function useAdvisorDetail() {
  const { show: notice } = useToast()

  async function open(id: string) {
    const seq = ++openSeq
    try {
      const [detail, guidance] = await Promise.all([api.advisor.detail(id), api.advisor.guidance(id)])
      if (seq !== openSeq) return
      student.value = { id, detail: detail as StudentDetailView, guidance: guidance as Guidance[] }
    } catch (e) {
      if (seq !== openSeq) return
      notice(getErrorMessage(e))
    }
  }

  /**
   * 发送指导：成功后全量重拉时间线并返回 true，失败留调用方处理（返回 false）。
   * G4：调用方须 await 结果后再关闭弹窗/刷新，避免“先关窗不等结果”。
   */
  async function sendGuidance(payload: GuidancePayload): Promise<boolean> {
    const detail = student.value
    if (!detail) return false
    guidanceSaving.value = true
    try {
      const action = payload.adviceType === 'COMMENT' ? api.advisor.writeGuidance : api.advisor.writeAdvice
      await action(detail.id, payload)
      await open(detail.id)
      notice('指导已发送并保存')
      return true
    } catch (e) {
      notice(getErrorMessage(e))
      return false
    } finally {
      guidanceSaving.value = false
    }
  }

  function close() { student.value = null }

  return { student, guidanceSaving, open, sendGuidance, close }
}
