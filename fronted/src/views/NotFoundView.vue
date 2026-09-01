<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { defaultRouteName } from '../router'

const { loggedIn, role } = useAuth()
const route = useRoute()
const router = useRouter()
function home() {
  router.push(loggedIn.value ? { name: defaultRouteName(role.value) } : { name: 'login' })
}
function back() {
  if (window.history.length > 1) router.back()
  else home()
}
</script>

<template>
  <main class="auth-shell">
    <header><span>重庆大学大数据与软件学院</span><span>学生生涯发展服务系统</span></header>
    <section class="status-page">
      <div class="status-code">404</div>
      <div class="status-card">
        <p class="eyebrow">PAGE NOT FOUND</p>
        <h1>页面不存在</h1>
        <p>您访问的地址不存在，或对应内容已被移除。请检查地址是否正确。</p>
        <code class="status-path">{{ route.fullPath }}</code>
        <div class="status-actions">
          <button type="button" class="outline-btn" @click="back">返回上一页</button>
          <button type="button" class="primary-btn" @click="home">返回首页 →</button>
        </div>
      </div>
    </section>
  </main>
</template>
