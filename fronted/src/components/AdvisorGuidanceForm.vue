<script setup lang="ts">
import { ref } from 'vue'
import type { Guidance } from '../types/domain'

export type GuidanceFormPayload = { content: string; adviceType: Guidance['adviceType']; suggestedTask?: string; retestReason?: string }

defineProps<{ saving: boolean }>()
const emit = defineEmits<{ submit: [payload: GuidanceFormPayload] }>()

const type = ref<Guidance['adviceType']>('COMMENT')
const content = ref('')
const suggestedTask = ref('')
const retestReason = ref('')

function submit() {
  if (!content.value.trim()) return
  emit('submit', { content: content.value.trim(), adviceType: type.value, suggestedTask: suggestedTask.value.trim() || undefined, retestReason: retestReason.value.trim() || undefined })
}
</script>

<template>
  <form class="advisor-guidance-form" @submit.prevent="submit"><p class="eyebrow">填写指导意见</p><h3>发送新的指导</h3><label>类型<select v-model="type"><option value="COMMENT">指导意见</option><option value="SUGGEST_TASK">建议任务</option><option value="SUGGEST_RETEST">建议重新测评</option></select></label><label>指导内容<textarea v-model.trim="content" required maxlength="2000" placeholder="清晰说明观察、建议与下一步行动"></textarea></label><label v-if="type === 'SUGGEST_TASK'">建议任务<input v-model.trim="suggestedTask" required maxlength="500"></label><label v-if="type === 'SUGGEST_RETEST'">重新测评原因<input v-model.trim="retestReason" required maxlength="500"></label><div><button class="primary-btn" :disabled="saving || !content">{{ saving ? '正在发送…' : '发送指导 →' }}</button></div></form>
</template>
