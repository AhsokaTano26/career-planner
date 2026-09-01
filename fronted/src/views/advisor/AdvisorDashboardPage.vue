<script setup lang="ts">
import { useRouter } from 'vue-router'
import PageHeader from '../../components/BasePageHeader.vue'
import { useAdvisorStatistics } from '../../composables/useAdvisorStatistics'
const router = useRouter()
const { statistics, loading, error, load } = useAdvisorStatistics()
const statisticsMode = false
const label=(path:string)=>({graduate:'国内升学',employment:'就业发展',overseas:'出国留学'} as Record<string,string>)[path]||'待确定'
</script>
<template><PageHeader eyebrow="辅导工作" :title="statisticsMode?'群体统计':'辅导工作总览'" :description="statisticsMode?'查看学生档案、目标与复盘的总体情况。':'以学生状态、计划进度与复盘节奏组织日常指导。'"><template #actions><button class="outline-btn" @click="load">刷新</button></template></PageHeader><p v-if="loading" class="empty">正在读取统计数据…</p><p v-else-if="error" class="empty error-state">{{error}}</p><template v-else><div class="metric-grid advisor-metrics"><article><b>{{statistics?.totalStudents??0}}</b><span>所带学生</span></article><article><b>{{statistics?.assessedCount??0}}</b><span>已完成测评</span></article><article><b>{{statistics?.planMadeCount??0}}</b><span>已制定计划</span></article><article><b>{{statistics?.reviewedCount??0}}</b><span>本月已复盘</span></article></div><div class="advisor-overview-grid"><section class="advisor-subcard"><p class="eyebrow">任务完成情况</p><b class="advisor-rate">{{statistics?.taskCompletionRate??'—'}}<small v-if="statistics?.taskCompletionRate!==undefined">%</small></b><p>所带学生的平均任务完成率，仅统计存在任务的已确认计划。</p><button class="outline-btn" @click="router.push('/advisor/students')">查看学生列表</button></section><section class="advisor-subcard"><p class="eyebrow">发展路径分布</p><div class="path-bars"><div v-for="item in (statistics?.pathDistribution||[])" :key="item.path"><div><span>{{label(item.path)}}</span><b>{{item.count}}</b></div><i><em :style="{width:`${statistics?.totalStudents?item.count/statistics.totalStudents*100:0}%`}"></em></i></div><p v-if="!(statistics?.pathDistribution||[]).length" class="empty">暂无路径分布数据。</p></div></section></div></template></template>
