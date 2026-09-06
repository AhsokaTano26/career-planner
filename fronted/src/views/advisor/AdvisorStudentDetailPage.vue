<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../../components/BasePageHeader.vue'
import AdvisorStudentTimeline from '../../components/AdvisorStudentTimeline.vue'
import AdvisorGuidanceForm from '../../components/AdvisorGuidanceForm.vue'
import { useAdvisorDetail } from '../../composables/useAdvisorDetail'

const route = useRoute()
const router = useRouter()
const { student, guidanceSaving, open, sendGuidance, close } = useAdvisorDetail()

function currentId() { return String(route.params.id || '') }

onMounted(() => { if (currentId()) open(currentId()) })
watch(() => route.params.id, (id) => { if (id) open(String(id)) })

async function submit(payload: Parameters<typeof sendGuidance>[0]) {
  // sendGuidance 成功后已全量重拉时间线，失败已 toast；页面无需额外处理
  await sendGuidance(payload)
}

function back() { close(); router.push({ name: 'advisor-students' }) }
</script>

<template>
  <PageHeader eyebrow="辅导工作" title="学生详情" description="只读时间线：档案、画像、推荐、目标计划、任务、复盘与指导记录。">
    <template #actions><button class="outline-btn" @click="back">返回学生列表</button></template>
  </PageHeader>
  <section v-if="student && String(route.params.id) === student.id" class="card data-list-card">
    <AdvisorStudentTimeline :detail="student.detail" :student-id="student.id" :guidance="student.guidance" />
    <AdvisorGuidanceForm :saving="guidanceSaving" @submit="submit" />
  </section>
  <p v-else class="empty">正在读取学生详情…</p>
</template>
