import { onMounted, ref } from 'vue'
import { api, getErrorMessage } from '../api/request'
import type { Completeness, ConsentStatus, Experience, ExperienceDraft, Profile, ProfileForm } from '../types/domain'
import { onSessionReset, useAuth } from './useAuth'
import { useToast } from './useToast'

// Module-level singleton state shared across the student views.
const profile = ref<Profile | null>(null)
const completeness = ref<Completeness | null>(null)
const experiences = ref<Experience[]>([])
const consent = ref<ConsentStatus | null>(null)
const consentAgreed = ref<boolean | null>(null)
// 稳定性：序号计数器必须与单例同级（模块级），否则多组件各计各的防不住覆盖
let loadSeq = 0
// saving 操作集合同样模块级，保证跨组件并发互斥计数
const savingOps = new Set<string>()
const saving = ref(false)

onSessionReset(() => {
  profile.value = null
  completeness.value = null
  experiences.value = []
  consent.value = null
  consentAgreed.value = null
  loadSeq++
  savingOps.clear()
  saving.value = false
})

export function useStudent() {
  const auth = useAuth()
  const { show: notice } = useToast()
  // 稳定性：本模块独占 saving（此前复用 useAuth().saving，与改密/账户按钮互锁误伤）；
  // 多操作并发以集合计数，任一先完成不再提前解禁其他。
  const savingRef = saving
  function beginSaving(key: string) { savingOps.add(key); savingRef.value = true }
  function endSaving(key: string) { savingOps.delete(key); savingRef.value = savingOps.size > 0 }

  async function load() {
    if (auth.role.value !== 'STUDENT') return
    // 稳定性：序号防卫，慢响应不得覆盖新请求的结果
    const seq = ++loadSeq
    // 复审 Batch4：弱依赖降级——完整度/经历/知情同意任一抖动不再掀翻整个工作台（此前 Promise.all
    // 任一 reject 即抛到全局 unhandledrejection，切到 /error 页）；核心档案失败仍抛给调用方处理。
    const [p, c, e, consentStatus] = await Promise.all([
      api.student.me(),
      api.student.completeness().catch(() => null),
      api.student.experiences().catch(() => null),
      api.auth.consentStatus().catch(() => null),
    ])
    if (seq !== loadSeq) return
    profile.value = p
    if (c) completeness.value = c
    if (e) experiences.value = e
    if (consentStatus) {
      consent.value = consentStatus
      consentAgreed.value = consentStatus.agreed
    }
  }

  async function saveProfile(form: ProfileForm): Promise<boolean> {
    beginSaving('profile')
    try {
      const number = (v: string) => v.trim() ? Number(v) : undefined
      const tags = (v: string) => v.split(/[、,，]/).map(item => item.trim()).filter(Boolean)
      profile.value = await api.student.update({
        basic: { gender: form.gender || undefined, hometown: form.hometown || undefined, birthday: form.birthday || undefined, phone: form.phone || undefined },
        academic: { math: number(form.math), english: number(form.english), programming: number(form.programming), note: form.academicNote || undefined },
        abilitySelf: { programming: number(form.abilityProgramming), math: number(form.abilityMath), english: number(form.abilityEnglish), communication: number(form.communication), organization: number(form.organization) },
        interestPrefs: tags(form.interests),
        values: tags(form.values),
        developmentIntention: form.developmentIntention,
        constraints: tags(form.constraints),
      })
      completeness.value = await api.student.completeness()
      notice('个人资料已保存')
      return true
    } catch (e) {
      notice(getErrorMessage(e))
      return false
    } finally {
      endSaving('profile')
    }
  }

  async function saveExperience(draft: ExperienceDraft): Promise<boolean> {
    beginSaving('experience')
    try {
      const data = {
        type: draft.type,
        title: draft.title,
        startDate: draft.startDate,
        endDate: draft.endDate || undefined,
        description: draft.description || undefined,
        attachment: draft.attachment || undefined,
      }
      const item = draft.id ? await api.student.updateExperience(draft.id, data) : await api.student.addExperience(data)
      const index = experiences.value.findIndex(current => current.id === item.id)
      index < 0 ? experiences.value.unshift(item) : experiences.value.splice(index, 1, item)
      notice('经历已保存')
      return true
    } catch (e) {
      notice(getErrorMessage(e))
      return false
    } finally {
      endSaving('experience')
    }
  }

  async function removeExperience(id: string): Promise<boolean> {
    try {
      await api.student.deleteExperience(id)
      experiences.value = experiences.value.filter(item => item.id !== id)
      notice('经历已删除')
      return true
    } catch (e) {
      notice(getErrorMessage(e))
      return false
    }
  }

  async function saveConsent() {
    if (!consent.value?.currentVersion) return
    beginSaving('consent')
    try {
      consent.value = await api.auth.consent({ version: consent.value.currentVersion })
      consentAgreed.value = consent.value.agreed
      notice('隐私授权已保存')
    } catch (e) {
      notice(getErrorMessage(e))
    } finally {
      endSaving('consent')
    }
  }

  async function requestDeletion(reason: string): Promise<boolean> {
    beginSaving('deletion')
    try {
      await api.student.requestDeletion(reason)
      notice('删除申请已提交')
      return true
    } catch (e) {
      notice(getErrorMessage(e))
      return false
    } finally {
      endSaving('deletion')
    }
  }

  onMounted(() => { load() })

  return { profile, completeness, experiences, consent, consentAgreed, saving, load, saveProfile, saveExperience, removeExperience, saveConsent, requestDeletion }
}
