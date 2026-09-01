<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { useToast } from '../composables/useToast'
import { defaultRouteName } from '../router'
import { decodeBase64 } from '../utils/base64'

const route = useRoute()
const router = useRouter()
const { loggedIn, role } = useAuth()
const { show: notice } = useToast()

const errorCode = computed(() => String(route.query.msg || '').trim())
const decoded = ref('')
const showDecoded = ref(false)

function toggleDecoded() {
  if (!showDecoded.value) {
    try {
      decoded.value = decodeBase64(errorCode.value)
    } catch {
      decoded.value = '错误码无效或已损坏，请尝试刷新页面重新操作。'
    }
    showDecoded.value = true
  } else {
    showDecoded.value = false
  }
}

async function copy() {
  if (!errorCode.value) return
  try {
    await navigator.clipboard.writeText(errorCode.value)
    notice('错误码已复制')
  } catch {
    notice('复制失败，请手动选择复制')
  }
}

function home() {
  router.push(loggedIn.value ? { name: defaultRouteName(role.value) } : { name: 'login' })
}

function reload() {
  window.location.reload()
}
</script>

<template>
  <main class="auth-shell">
    <header><span>重庆大学大数据与软件学院</span><span>学生生涯发展服务系统</span></header>
    <section class="status-page">
      <div class="status-code">500</div>
      <div class="status-card">
        <p class="eyebrow">SYSTEM ERROR</p>
        <h1>页面出错了</h1>
        <p>系统遇到一个未能自动恢复的错误。请将下方错误码提供给管理员或辅导员以便排查。</p>
        <p v-if="!errorCode" class="status-empty">未携带错误信息。您可以刷新页面重新操作。</p>
        <div v-else class="error-trace">
          <pre><code>{{ errorCode }}</code></pre>
          <template v-if="showDecoded"><small>解码内容</small><pre><code>{{ decoded }}</code></pre></template>
        </div>
        <div class="status-actions">
          <button type="button" class="outline-btn" @click="toggleDecoded">{{ showDecoded ? '隐藏解码内容' : '解码查看' }}</button>
          <button v-if="errorCode" type="button" class="outline-btn" @click="copy">复制错误码</button>
          <button type="button" class="outline-btn" @click="reload">刷新页面</button>
          <button type="button" class="primary-btn" @click="home">返回首页 →</button>
        </div>
      </div>
    </section>
  </main>
</template>
