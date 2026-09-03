<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import RouteTransition from '../components/RouteTransition.vue'
import { menuGroups, type MenuGroup } from '../router'
import type { Role } from '../types/domain'

const route = useRoute()
const router = useRouter()
const { currentUser: user, logout } = useAuth()
const role = (route.meta.role ?? 'STUDENT') as Role
const menus: MenuGroup[] = menuGroups[role as Exclude<Role, 'ADMIN'>]
const names: Record<Role, string> = { STUDENT: '学生工作台', ADVISOR: '辅导员工作台', ADMIN: '系统管理台' }

async function handleLogout() {
  await logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <aside class="side-nav"><div class="brand"><img class="brand-logo" src="../assets/cqu-logo.svg" alt="重庆大学校徽"><span class="brand-name">重庆大学<br>大数据与软件学院</span></div><div class="user-card"><div class="avatar">{{ (user?.name || names[role]).slice(0,1) }}</div><div><b>{{ user?.name || user?.username }}</b><small>{{ names[role] }} · 在线</small></div></div><nav v-for="group in menus" :key="group.group"><p class="nav-group">{{ group.group }}</p><button v-for="[path,label] in group.links" :key="path" :class="{active:route.path===path}" @click="router.push(path)"><i>◼</i>{{ label }}</button></nav><div class="side-foot">重庆大学大数据与软件学院<br>学生生涯发展服务系统</div></aside>
    <main class="main-content"><header class="topbar"><span>重庆大学大数据与软件学院 · {{ names[role] }} · 2026—2027 学年第一学期</span><div><button class="logout-btn" @click="handleLogout">↙ 退出登录</button></div></header><section class="page"><RouteTransition /></section></main>
  </div>
</template>
