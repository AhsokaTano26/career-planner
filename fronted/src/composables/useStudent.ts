import { onMounted, ref, watch } from 'vue'
import { api, getErrorMessage } from '../api/request'
import type { Completeness, ConsentStatus, Experience, ExperienceDraft, Profile, ProfileForm } from '../types/domain'
import { useAuth } from './useAuth'
import { useToast } from './useToast'

// Module-level singleton state shared across the student views.
const profile = ref<Profile | null>(null)
const completeness = ref<Completeness | null>(null)
const experiences = ref<Experience[]>([])
const consent = ref<ConsentStatus | null>(null)
const consentAgreed = ref<boolean | null>(null)

export function useStudent() {
  const auth = useAuth()
  const { show: notice } = useToast()
  const { saving } = auth

  async function load() {
    if (auth.role.value !== 'STUDENT' || auth.forcePasswordChange.value) return
    const [p, c, e, consentStatus] = await Promise.all([
      api.student.me(),
      api.student.completeness(),
      api.student.experiences(),
      api.auth.consentStatus(),
    ])
    profile.value = p
    completeness.value = c
    experiences.value = e
    consent.value = consentStatus
    consentAgreed.value = consentStatus.agreed
  }

  async function saveProfile(form: ProfileForm) {
    saving.value = true
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
    } catch (e) {
      notice(getErrorMessage(e))
    } finally {
      saving.value = false
    }
  }

  async function saveExperience(draft: ExperienceDraft) {
    saving.value = true
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
    } catch (e) {
      notice(getErrorMessage(e))
    } finally {
      saving.value = false
    }
  }

  async function removeExperience(id: string) {
    if (!confirm('确定删除这条经历吗？')) return
    try {
      await api.student.deleteExperience(id)
      experiences.value = experiences.value.filter(item => item.id !== id)
      notice('经历已删除')
    } catch (e) {
      notice(getErrorMessage(e))
    }
  }

  async function saveConsent() {
    if (!consent.value?.currentVersion) return
    saving.value = true
    try {
      consent.value = await api.auth.consent({ version: consent.value.currentVersion })
      consentAgreed.value = consent.value.agreed
      notice('隐私授权已保存')
    } catch (e) {
      notice(getErrorMessage(e))
    } finally {
      saving.value = false
    }
  }

  async function requestDeletion(reason: string) {
    saving.value = true
    try {
      await api.student.requestDeletion(reason)
      notice('删除申请已提交')
    } catch (e) {
      notice(getErrorMessage(e))
    } finally {
      saving.value = false
    }
  }

  onMounted(() => { load() })
  // When the mandatory password change completes, the gate lifts and data loads once.
  watch(() => auth.forcePasswordChange.value, (forced) => {
    if (!forced && auth.loggedIn.value) load()
  })

  return { profile, completeness, experiences, consent, consentAgreed, saving, load, saveProfile, saveExperience, removeExperience, saveConsent, requestDeletion }
}
