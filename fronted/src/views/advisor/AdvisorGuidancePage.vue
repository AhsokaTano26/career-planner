<script setup lang="ts">
import PageHeader from '../../components/BasePageHeader.vue'
import { useAdvisorStudents } from '../../composables/useAdvisorStudents'
import { useAdvisorDetail } from '../../composables/useAdvisorDetail'
const { students, loading, error, load } = useAdvisorStudents()
const { open: openDetail } = useAdvisorDetail()
</script>
<template><PageHeader eyebrow="辅导工作" title="指导记录" description="选择学生后查看历史指导记录并发送新的建议。"><template #actions><button class="outline-btn" @click="load(1)">刷新学生列表</button></template></PageHeader><section class="card data-list-card"><div class="guidance-hint"><b>按学生查看记录</b><span>指导记录接口按学生查询；选择学生后可查看完整历史，并发送指导意见、建议任务或建议重新测评。</span></div><p v-if="loading" class="empty">正在读取学生数据…</p><p v-else-if="error" class="empty error-state">{{error}}</p><div v-else class="record-list enhanced-list"><div class="list-columns"><span>学生编号</span><span>学生</span><span>操作</span></div><article v-for="student in students" :key="student.id"><small>{{student.id}}</small><div><b>{{student.name}}</b><span>{{student.className||'未填写班级'}} · {{student.primaryGoal||'尚未设定主目标'}}</span></div><button class="outline-btn" @click="openDetail(student.id)">查看记录</button></article><p v-if="!students.length" class="empty">暂无所带学生。</p></div></section></template>
