<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import RouteTransition from '../components/RouteTransition.vue'

const route = useRoute()
const router = useRouter()
const { currentUser: user, logout } = useAuth()

const groups: { title: string; links: [string, string][] }[] = [
  { title: '平台配置', links: [['/admin/overview', '运行总览'], ['/admin/users', '用户管理'], ['/admin/whitelist', '白名单'], ['/admin/relations', '师生关系'], ['/admin/directions', '方向库'], ['/admin/abilities', '能力标签'], ['/admin/templates', '任务模板']] },
  { title: '内容与治理', links: [['/admin/curricula', '培养方案'], ['/admin/weights', '推荐权重'], ['/admin/exports', '导出任务'], ['/admin/logs', '审计日志']] },
  { title: 'AI 管理', links: [['/admin/ai-playground', 'AI 服务调试']] },
]
const currentTitle = computed(() => groups.flatMap(group => group.links).find(([path]) => route.path === path)?.[1] || '管理工作台')

async function handleLogout() {
  await logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="app-shell"><aside class="side-nav"><div class="brand"><img class="brand-logo" src="../assets/cqu-logo.svg" alt="重庆大学校徽"><span class="brand-name">重庆大学<br>大数据与软件学院</span></div><div class="user-card"><div class="avatar">{{ (user?.name || '系统管理员').slice(0,1) }}</div><div><b>{{ user?.name || user?.username || '系统管理员' }}</b><small>系统管理台 · 在线</small></div></div><nav v-for="group in groups" :key="group.title"><p class="nav-group">{{group.title}}</p><button v-for="[path,label] in group.links" :key="path" :class="{active:route.path===path}" @click="router.push(path)"><i>◼</i>{{label}}</button></nav><div class="side-foot">重庆大学大数据与软件学院<br>学生生涯发展服务系统</div></aside><main class="main-content"><header class="topbar"><span>大数据与软件学院 · 系统管理台 · 2026—2027 学年第一学期</span><button class="logout-btn" @click="handleLogout">↙ 退出登录</button></header><section class="page"><div class="page-title"><div><p class="eyebrow">系统管理</p><h1>{{ currentTitle }}</h1><p>面向重庆大学大数据与软件学院的数据维护、导入审核、导出与审计。</p></div></div><RouteTransition /></section></main></div>
</template>
