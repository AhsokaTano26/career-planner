<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { defaultRouteName } from '../router'

const { loggedIn, role } = useAuth()
const router = useRouter()
function home() {
  router.push(loggedIn.value ? { name: defaultRouteName(role.value) } : { name: 'login' })
}
</script>

<template>
  <main class="auth-shell">
    <header><span>重庆大学大数据与软件学院</span><span>学生生涯发展服务系统</span></header>
    <section class="not-found">
      <p class="eyebrow">404</p>
      <h1>页面不存在</h1>
      <p>您访问的地址不存在或已被移除。</p>
      <button class="primary-btn" @click="home">返回首页 →</button>
    </section>
  </main>
</template>
