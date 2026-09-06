<script setup lang="ts">
import { computed } from 'vue'
import type { Guidance, StudentDetailView } from '../types/domain'
import { formatDateTime, pathLabel, statusLabel } from '../utils/format'

const props = defineProps<{ detail: StudentDetailView; studentId: string; guidance: Guidance[] }>()

const profile = computed(() => props.detail.profile ?? {})
const portrait = computed(() => props.detail.portrait ?? {})
const goal = computed(() => props.detail.goal ?? {})
const plan = computed(() => props.detail.plan ?? {})
const recommendation = computed(() => props.detail.recommendation ?? {})
const tasks = computed(() => props.detail.tasks ?? [])
const reviews = computed(() => props.detail.reviews ?? [])
const recommendations = computed(() => recommendation.value.results ?? [])
const adviceLabel = (value: Guidance['adviceType']) => ({ COMMENT: '指导意见', SUGGEST_TASK: '建议任务', SUGGEST_RETEST: '建议重新测评' } as Record<Guidance['adviceType'], string>)[value]
</script>

<template>
  <div class="advisor-detail-scroll">
    <section class="advisor-detail-section"><h3>档案概览</h3><div class="detail-kv"><div><small>档案完整度</small><b>{{ profile.completeness ?? '—' }}<i v-if="profile.completeness !== undefined">%</i></b></div><div><small>发展意向</small><b>{{ pathLabel(String(profile.developmentIntention || '')) }}</b></div><div><small>专业大类</small><b>{{ String(profile.majorCategory || '未填写') }}</b></div><div><small>兴趣偏好</small><b>{{ Array.isArray(profile.interestPrefs) ? profile.interestPrefs.join('、') : '未填写' }}</b></div></div></section>
    <section v-if="portrait.summary || portrait.completeness !== undefined" class="advisor-detail-section"><h3>能力画像</h3><p>{{ String(portrait.summary || '暂无画像摘要') }}</p><div class="detail-tags"><span v-for="item in (portrait.strengths ?? [])" :key="String(item)">优势：{{ item }}</span><span v-for="item in (portrait.explore ?? [])" :key="String(item)">待探索：{{ item }}</span></div></section>
    <section class="advisor-detail-section detail-split"><div><h3>目标与计划</h3><p><b>主目标：</b>{{ String(goal.primary?.name || '尚未设定') }}</p><p><b>备选目标：</b>{{ String(goal.backup?.name || '尚未设定') }}</p><p><b>计划：</b>{{ String(plan.goalSummary || '尚未制定') }}</p><p><b>状态：</b>{{ statusLabel(String(plan.status || '')) }}</p></div><div><h3>推荐方向</h3><p v-for="item in recommendations.slice(0, 3)" :key="String(item.directionId)"><b>第 {{ item.rank ?? '—' }} 名</b> {{ String(item.directionId || '方向') }} · {{ item.score ?? '—' }} 分</p><p v-if="!recommendations.length">暂无推荐结果。</p></div></section>
    <section class="advisor-detail-section"><h3>计划任务</h3><div class="detail-timeline"><article v-for="(task,index) in tasks" :key="String(task.id)" :style="{ '--i': index }"><div><b>{{ String(task.title || '未命名任务') }}</b><small>{{ String(task.month || '未排期') }} · {{ String(task.type || task.taskType || '学习任务') }} · {{ statusLabel(String(task.status || '')) }}</small></div><span>{{ task.deadline ? formatDateTime(String(task.deadline)) : '无截止日期' }}</span></article><p v-if="!tasks.length">暂无计划任务。</p></div></section>
    <section class="advisor-detail-section"><h3>阶段复盘</h3><div class="detail-timeline"><article v-for="(review,index) in reviews" :key="String(review.id)" :style="{ '--i': index }"><div><b>{{ String(review.cycle || '阶段复盘') }}</b><small>{{ statusLabel(String(review.status || '')) }} · {{ review.submittedAt ? formatDateTime(String(review.submittedAt)) : '未提交' }}</small></div><span>{{ review.advisorRequested ? '已请求指导' : '未申请指导' }}</span></article><p v-if="!reviews.length">暂无阶段复盘。</p></div></section>
    <section class="advisor-detail-section"><h3>历史指导记录</h3><div class="guidance-history"><article v-for="(item,index) in guidance" :key="item.id" :style="{ '--i': index }"><div><em>{{ adviceLabel(item.adviceType) }}</em><time>{{ formatDateTime(item.createdAt) }}</time></div><b>{{ item.content }}</b><p v-if="item.suggestedTask">建议任务：{{ item.suggestedTask }}</p><p v-if="item.retestReason">重新测评原因：{{ item.retestReason }}</p></article><p v-if="!guidance.length">暂无历史指导记录。</p></div></section>
  </div>
</template>
