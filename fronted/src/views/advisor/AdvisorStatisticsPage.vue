<script setup lang="ts">
import PageHeader from '../../components/BasePageHeader.vue'
import CountUp from '../../components/CountUp.vue'
import { useAdvisorStatistics } from '../../composables/useAdvisorStatistics'

const { statistics, loading, error, load } = useAdvisorStatistics()

const pathName = (value?: string) => ({ graduate: '国内升学', employment: '就业发展', overseas: '出国留学', undecided: '待确定' } as Record<string, string>)[value || ''] || '待确定'
// G5：单口径统计——全部指标来自 GET /advisor/statistics，不再前端拉全量自算。
const maxCount = () => Math.max(1, ...(statistics.value?.pathDistribution || []).map((item) => item.count))
</script>

<template>
  <PageHeader eyebrow="辅导工作" title="群体统计" description="所带学生的测评、计划与复盘总体情况（以后端统计口径为准）。">
    <template #actions><button class="outline-btn" @click="load">刷新统计</button></template>
  </PageHeader>
  <p v-if="loading" class="empty">正在汇总学生数据…</p>
  <p v-else-if="error" class="empty error-state">{{ error }}</p>
  <template v-else>
    <div class="metric-grid advisor-metrics analysis-metrics">
      <article><b><CountUp :to="statistics?.totalStudents ?? 0" /></b><span>所带学生</span></article>
      <article><b><CountUp :to="statistics?.assessedCount ?? 0" /></b><span>已完成测评</span></article>
      <article><b><CountUp :to="statistics?.planMadeCount ?? 0" /></b><span>已制定计划</span></article>
      <article><b><CountUp :to="statistics?.reviewedCount ?? 0" /></b><span>本月已复盘</span></article>
    </div>
    <section class="analysis-grid">
      <article class="analysis-card"><div class="analysis-card-head"><div><p class="eyebrow">方向对比</p><h2>发展路径分布</h2></div><span>{{ (statistics?.pathDistribution || []).length }} 类去向</span></div><div class="analysis-bars"><div v-for="item in (statistics?.pathDistribution || [])" :key="item.path"><div><b>{{ pathName(item.path) }}</b><span>{{ item.count }} 人</span></div><i><em :style="{ width: `${item.count / maxCount() * 100}%` }" /></i></div></div><p v-if="!(statistics?.pathDistribution || []).length" class="empty">暂无路径分布数据。</p></article>
      <article class="analysis-card"><div class="analysis-card-head"><div><p class="eyebrow">任务进度</p><h2>平均任务完成率</h2></div><span>已确认计划</span></div><div class="analysis-bars"><div><div><b>完成率</b><span v-if="statistics?.taskCompletionRate !== undefined">{{ statistics?.taskCompletionRate }}%</span><span v-else>暂无任务数据</span></div><i><em :style="{ width: `${statistics?.taskCompletionRate ?? 0}%` }" /></i></div></div><p class="analysis-note">所带学生的平均任务完成率，仅统计存在任务的已确认计划。</p></article>
    </section>
  </template>
</template>
