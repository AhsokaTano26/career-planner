<script setup lang="ts">
import { ref } from 'vue'
import AdminWorkbench from '../views/admin/AdminWorkbenchView.vue'
import type { User } from '../api/request'

defineProps<{ user:User|null }>()
const emit = defineEmits<{ notice:[message:string]; logout:[] }>()
const active = ref('admin-overview')
const groups = [
  { title:'平台配置', links:[['admin-overview','运行总览'],['users','用户管理'],['whitelist','白名单'],['relations','师生关系'],['admin-directions','方向库'],['abilities','能力标签'],['templates','任务模板']] },
  { title:'内容与治理', links:[['curricula','培养方案'],['weights','推荐权重'],['exports','导出任务'],['logs','审计日志']] },
]
</script>

<template>
  <div class="app-shell"><aside class="side-nav"><div class="brand"><img class="brand-logo" src="../assets/cqu-logo.svg" alt="重庆大学校徽"><span class="brand-name">重庆大学<br>大数据与软件学院</span></div><div class="user-card"><div class="avatar">{{ (user?.name || '系统管理员').slice(0,1) }}</div><div><b>{{ user?.name || user?.username || '系统管理员' }}</b><small>系统管理台 · 在线</small></div></div><nav v-for="group in groups" :key="group.title"><p class="nav-group">{{group.title}}</p><button v-for="[id,label] in group.links" :key="id" :class="{active:active===id}" @click="active=id"><i>◼</i>{{label}}</button></nav><div class="side-foot">重庆大学大数据与软件学院<br>学生生涯发展服务系统</div></aside><main class="main-content"><header class="topbar"><span>大数据与软件学院 · 系统管理台 · 2026—2027 学年第一学期</span><button class="logout-btn" @click="emit('logout')">↙ 退出登录</button></header><section class="page fade-in"><div class="page-title"><div><p class="eyebrow">系统管理</p><h1>{{ groups.flatMap(group=>group.links).find(link=>link[0]===active)?.[1] }}</h1><p>面向重庆大学大数据与软件学院的数据维护、导入审核、导出与审计。</p></div></div><AdminWorkbench :module="active" @notice="emit('notice',$event)"/></section></main></div>
</template>
