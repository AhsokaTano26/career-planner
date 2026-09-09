<script setup lang="ts">
import { MotionConfig } from 'motion-v'
import AdvisorStudentDialog from './components/AdvisorStudentDialog.vue'
import type { GuidanceFormPayload } from './components/AdvisorGuidanceForm.vue'
import { useAdvisorDetail } from './composables/useAdvisorDetail'
import { useToast } from './composables/useToast'

const { student: advisorDetail, guidanceSaving, sendGuidance, close: closeAdvisorDetail } = useAdvisorDetail()
const { toast } = useToast()

// G4：等发送结果，成功才关窗；失败留窗内（useAdvisorDetail 内已 toast）。
async function handleGuidanceSubmit(payload: GuidanceFormPayload) {
  if (await sendGuidance(payload)) closeAdvisorDetail()
}
</script>

<template>
  <MotionConfig :reducedMotion="'user'">
    <Transition name="page" mode="out-in">
      <RouterView />
    </Transition>
    <Transition name="modal">
      <AdvisorStudentDialog
        v-if="advisorDetail"
        :student="advisorDetail"
        :saving="guidanceSaving"
        @close="closeAdvisorDetail"
        @submit="handleGuidanceSubmit"
      />
    </Transition>
    <Transition name="toast">
      <div v-if="toast" class="toast">✓ {{ toast }}</div>
    </Transition>
  </MotionConfig>
</template>
