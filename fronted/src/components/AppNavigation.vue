<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AppIcon from './AppIcon.vue'

type NavigationGroup = { title: string; links: [string, string][] }

defineProps<{
  groups: NavigationGroup[]
  activePath: string
  userName?: string
  workspaceName: string
}>()

const emit = defineEmits<{ navigate: [path: string]; logout: [] }>()
const open = ref(false)

function close() { open.value = false }
function navigate(path: string) { emit('navigate', path); close() }
function onKeydown(event: KeyboardEvent) { if (event.key === 'Escape') close() }

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <button class="mobile-nav-trigger" type="button" data-testid="mobile-nav-trigger" :aria-expanded="open" aria-controls="workspace-navigation" aria-label="打开导航菜单" @click="open = !open">
    <AppIcon :name="open ? 'close' : 'menu'" />
    <span>导航</span>
  </button>
  <aside id="workspace-navigation" class="side-nav" :class="{ 'mobile-open': open }" aria-label="工作台导航">
    <div class="brand"><img class="brand-logo" src="../assets/cqu-logo.svg" alt="重庆大学校徽"><span class="brand-name">重庆大学<br>大数据与软件学院</span></div>
    <div class="user-card"><div class="avatar">{{ (userName || workspaceName).slice(0, 1) }}</div><div><b>{{ userName || workspaceName }}</b><small>{{ workspaceName }} · 在线</small></div></div>
    <nav v-for="group in groups" :key="group.title" :aria-label="group.title"><p class="nav-group">{{ group.title }}</p><button v-for="[path, label] in group.links" :key="path" type="button" :data-path="path" :class="{ active: activePath === path }" :aria-current="activePath === path ? 'page' : undefined" @click="navigate(path)"><AppIcon name="grid" /><span>{{ label }}</span></button></nav>
    <div class="side-foot"><span>重庆大学大数据与软件学院<br>学生生涯发展服务系统</span><button type="button" aria-label="退出登录" @click="emit('logout')"><AppIcon name="logout" />退出</button></div>
  </aside>
</template>
