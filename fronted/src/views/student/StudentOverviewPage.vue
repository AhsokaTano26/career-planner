<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../../components/BasePageHeader.vue'
import CountUp from '../../components/CountUp.vue'
import { useAuth } from '../../composables/useAuth'
import { useStudent } from '../../composables/useStudent'
import { nextStudentAction } from './studentJourney'
const router = useRouter()
const { currentUser: user } = useAuth()
const { profile, completeness } = useStudent()
const nextAction = computed(() => nextStudentAction(completeness.value?.missing ?? []))
</script>
<template>
  <PageHeader eyebrow="个人档案" title="个人档案概览" description="及时维护个人信息和成长经历，便于后续辅导与服务使用。"/>
  <section class="journey-panel" aria-labelledby="journey-title">
    <div class="journey-head"><div><span class="soft-pill">档案状态</span><h2 id="journey-title">{{ user?.name || '我的生涯档案' }}</h2></div><b v-if="(completeness?.score ?? profile?.completeness) != null"><CountUp :to="Number(completeness?.score ?? profile?.completeness ?? 0)"/><small>%</small></b><b v-else>—</b></div>
    <div class="progress-line" role="progressbar" aria-label="档案完整度" :aria-valuenow="Number(completeness?.score ?? profile?.completeness ?? 0)" aria-valuemin="0" aria-valuemax="100"><span :style="{width:(completeness?.score ?? profile?.completeness ?? 0)+'%'}"></span></div>
    <div class="journey-steps"><div v-for="item in (completeness?.dimensions ?? [])" :key="item.key" class="journey-step" :class="{done:item.filled}"><span class="step-dot" aria-hidden="true">{{ item.filled?'✓':'·' }}</span><div><b>{{ item.name }}</b><small>{{ item.filled?'已完善':'待补充' }}</small></div></div></div>
  </section>
  <div class="dashboard-grid">
    <section class="card focus-card"><p class="eyebrow">下一步</p><h2>{{ nextAction.title }}</h2><div class="focus-task"><p>{{ nextAction.description }}</p><button class="primary-btn" @click="router.push(nextAction.path)">{{ nextAction.label }} <span aria-hidden="true">→</span></button></div></section>
    <section class="card data-card"><p class="eyebrow">完整度</p><b class="big-number">{{ completeness?.filled ?? 0 }}</b><span>/ {{ completeness?.total ?? 0 }} 已填写字段</span></section>
  </div>
</template>
