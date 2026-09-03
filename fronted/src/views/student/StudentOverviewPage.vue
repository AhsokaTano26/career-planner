<script setup lang="ts">
import { useRouter } from 'vue-router'
import PageHeader from '../../components/BasePageHeader.vue'
import CountUp from '../../components/CountUp.vue'
import { useAuth } from '../../composables/useAuth'
import { useStudent } from '../../composables/useStudent'
const router = useRouter()
const { currentUser: user } = useAuth()
const { profile, completeness } = useStudent()
</script>
<template><PageHeader eyebrow="个人档案" title="个人档案概览" description="及时维护个人信息和成长经历，便于后续辅导与服务使用。"/><div class="journey-panel"><div class="journey-head"><div><span class="soft-pill">档案状态</span><h2>{{ user?.name || '我的生涯档案' }}</h2></div><b v-if="(completeness?.score ?? profile?.completeness) != null"><CountUp :to="Number(completeness?.score ?? profile?.completeness ?? 0)"/><small>%</small></b><b v-else>—</b></div><div class="progress-line"><span :style="{width:(completeness?.score ?? profile?.completeness ?? 0)+'%'}"></span></div><div class="journey-steps"><div v-for="item in (completeness?.dimensions ?? [])" :key="item.key" class="journey-step" :class="{done:item.filled}"><span class="step-dot">{{ item.filled?'✓':'·' }}</span><div><b>{{ item.name }}</b><small>{{ item.filled?'已完善':'待补充' }}</small></div></div></div></div><div class="dashboard-grid"><section class="card"><p class="eyebrow">下一步</p><h2>{{ completeness?.missing?.length ? '下一步：补充档案' : '档案信息已完善' }}</h2><div class="focus-task"><p>{{ completeness?.missing?.length ? `仍缺少：${completeness.missing.map(item=>item.name).join('、')}` : '可继续维护个人经历，为后续辅导提供完整的个人信息。' }}</p><button class="primary-btn" @click="router.push('/student/profile')">维护个人资料 →</button></div></section><section class="card data-card"><p class="eyebrow">完整度</p><b class="big-number">{{ completeness?.filled ?? 0 }}</b><span>/ {{ completeness?.total ?? 0 }} 已填写字段</span></section></div></template>
