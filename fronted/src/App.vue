<script setup lang="ts">
import { watch } from 'vue'
import { MotionConfig } from 'motion-v'
import { useRouter } from 'vue-router'
import PasswordChangeDialog from './components/PasswordChangeDialog.vue'
import AdvisorStudentDialog from './components/AdvisorStudentDialog.vue'
import { getErrorMessage } from './api/request'
import { useAuth } from './composables/useAuth'
import { useAdvisorDetail } from './composables/useAdvisorDetail'
import { useToast } from './composables/useToast'
import { defaultRouteName } from './router'

const { loggedIn, role, forcePasswordChange, forcedPasswordSaving, completeForcedPasswordChange } = useAuth()
const { student: advisorDetail, guidanceSaving, sendGuidance, close: closeAdvisorDetail } = useAdvisorDetail()
const { toast, show: notice } = useToast()
const router = useRouter()

// While the initial password must still be changed, park the user on the role's
// default page behind the mandatory dialog instead of letting them roam.
watch(forcePasswordChange, (forced) => {
  if (forced && loggedIn.value) {
    router.replace({ name: defaultRouteName(role.value) }).catch(() => { /* 已在目标页时导航取消属正常情况 */ })
  }
})

async function handleForcedPasswordChange(oldPassword: string, newPassword: string) {
  try {
    await completeForcedPasswordChange(oldPassword, newPassword)
    notice('初始密码已修改，请继续使用系统')
  } catch (e) {
    notice(getErrorMessage(e))
  }
}
</script>

<template>
  <MotionConfig :reducedMotion="'user'">
    <RouterView />
    <PasswordChangeDialog
      v-if="forcePasswordChange"
      mandatory
      :saving="forcedPasswordSaving"
      @submit="handleForcedPasswordChange"
    />
    <AdvisorStudentDialog
      v-if="advisorDetail"
      :student="advisorDetail"
      :saving="guidanceSaving"
      @close="closeAdvisorDetail"
      @submit="sendGuidance"
    />
    <div v-if="toast" class="toast">✓ {{ toast }}</div>
  </MotionConfig>
</template>
