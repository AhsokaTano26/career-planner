<script setup lang="ts">
import BaseModal from './BaseModal.vue'
import AdvisorStudentTimeline from './AdvisorStudentTimeline.vue'
import AdvisorGuidanceForm, { type GuidanceFormPayload } from './AdvisorGuidanceForm.vue'
import type { AdvisorDetail } from '../types/domain'

const props = defineProps<{ student: AdvisorDetail; saving: boolean }>()
// G4：submit 只上报 payload，由调用方 await 发送结果后再关闭；失败留窗内（不再先关窗）。
const emit = defineEmits<{ close: []; submit: [payload: GuidanceFormPayload] }>()
</script>

<template>
  <BaseModal @close="emit('close')">
    <section class="modal-card advisor-detail-modal">
      <header><div><p class="eyebrow">学生详情</p><h2>{{ String(student.detail.profile?.name || '学生详情') }}</h2><p>{{ String(student.detail.profile?.className || '未填写班级') }} · {{ student.id }}</p></div><button class="outline-btn" @click="emit('close')">关闭</button></header>
      <AdvisorStudentTimeline :detail="student.detail" :student-id="student.id" :guidance="student.guidance" />
      <AdvisorGuidanceForm :saving="saving" @submit="emit('submit', $event)" />
    </section>
  </BaseModal>
</template>
