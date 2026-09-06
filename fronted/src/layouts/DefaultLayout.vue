<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import RouteTransition from '../components/RouteTransition.vue'
import { menuGroups } from '../router'
import type { Role } from '../types/domain'
import AppNavigation from '../components/AppNavigation.vue'

const route = useRoute()
const router = useRouter()
const { currentUser: user, logout } = useAuth()
const role = (route.meta.role ?? 'STUDENT') as Role
const menus = menuGroups[role as Exclude<Role, 'ADMIN'>].map(({ group, links }) => ({ title: group, links }))
const names: Record<Role, string> = { STUDENT: '学生工作台', ADVISOR: '辅导员工作台', ADMIN: '系统管理台' }

async function handleLogout() {
  await logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <AppNavigation :groups="menus" :active-path="route.path" :user-name="user?.name || user?.username" :workspace-name="names[role]" @navigate="router.push" @logout="handleLogout" />
    <main class="main-content"><header class="topbar"><span>重庆大学大数据与软件学院 · {{ names[role] }} · 2026—2027 学年第一学期</span><div><button class="logout-btn" @click="handleLogout">退出登录</button></div></header><section class="page"><RouteTransition /></section></main>
  </div>
</template>
