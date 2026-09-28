<script setup lang="ts">
import { computed, ref } from 'vue'
import { api, getErrorMessage } from '../../api/request'
import { onSessionReset } from '../../composables/useAuth'
import BaseModal from '../../components/BaseModal.vue'
import PageHeader from '../../components/BasePageHeader.vue'

type Method = 'GET' | 'POST' | 'PATCH' | 'DELETE'
type Endpoint = { id:string; group:'gateway'|'ai'; method:Method; path:string; desc:string; sample:Record<string,unknown> }

const endpoints:Endpoint[] = [
  { id:'gw-generate', group:'gateway', method:'POST', path:'/api/v1/gateway/generate', desc:'高阶文本生成（无 token）', sample:{ messages:[{ role:'user', content:'你好，请用一句话介绍你自己。' }], scene:'gateway_api', temperature:0.7, maxTokens:500 } },
  { id:'gw-chat', group:'gateway', method:'POST', path:'/api/v1/gateway/chat/completions', desc:'OpenAI 兼容 chat completions（无 token）', sample:{ messages:[{ role:'user', content:'你好' }], temperature:0.7, max_tokens:500, stream:false } },
  { id:'ai-agent', group:'ai', method:'POST', path:'/api/v1/ai/agent/invoke', desc:'智能体对话（LangGraph：工具调用 + RAG + 多轮记忆；需 JWT；STAFF 可指定任意 studentRef，学生仅本人）', sample:{ studentRef:'2026011301', sessionId:'playground-001', question:'我适合什么职业方向？' } },
]

const grouped = computed(() => {
  const gw = endpoints.filter(e => e.group === 'gateway')
  const ai = endpoints.filter(e => e.group === 'ai')
  return { gw, ai }
})

const active = ref<Endpoint | null>(null)
const bodyJson = ref('')
const sending = ref(false)
const response = ref('')
const errorMsg = ref('')
const successMsg = ref('')
// 稳定性：切号/401 时清空 playground 状态
onSessionReset(()=>{
  active.value=null; response.value=''; errorMsg.value=''; successMsg.value=''; sending.value=false
})

function open(ep:Endpoint) {
  active.value = ep
  bodyJson.value = JSON.stringify(ep.sample, null, 2)
  response.value = ''
  errorMsg.value = ''
  successMsg.value = ''
}

function close() {
  if (sending.value) return
  active.value = null
}

async function send() {
  if (!active.value) return
  const ep = active.value
  sending.value = true
  errorMsg.value = ''
  response.value = ''
  successMsg.value = ''
  try {
    const body = (() => { try { return JSON.parse(bodyJson.value || '{}') } catch { throw new Error('请求体 JSON 解析失败') } })()
    let data:any
    if (ep.id === 'gw-generate') data = await api.gateway.generate(body)
    else if (ep.id === 'gw-chat') data = await api.gateway.chatCompletions(body)
    else data = await api.ai.agentInvoke(body)
    response.value = JSON.stringify(data, null, 2)
    // 少数接口成功时返回空 Map，肉眼看不出成功；给个友好提示并隐藏空响应框。
    const isEmpty = data === undefined || data === null || (typeof data === 'object' && !Array.isArray(data) && Object.keys(data).length === 0)
    successMsg.value = isEmpty ? '✅ 请求成功（后端无返回内容）' : ''
  } catch (e) {
    errorMsg.value = getErrorMessage(e)
    successMsg.value = ''
  } finally {
    sending.value = false
  }
}

function methodClass(m:Method) { return `method-${m.toLowerCase()}` }
</script>

<template>
  <PageHeader eyebrow="系统管理" title="AI 接口调试台" description="一键触发所有 AI / Gateway 相关接口，调试无需样式精校。仅供开发与运维使用。" />
  <section class="card data-list-card ai-playground">
    <div class="section-head"><div><p class="eyebrow">Gateway（无需 JWT）</p><h2>Gateway 接口</h2></div></div>
    <div class="endpoint-grid">
      <article v-for="(ep, idx) in grouped.gw" :key="ep.id" :style="{ '--i': idx }" class="endpoint-card">
        <div class="endpoint-head">
          <span :class="['method-badge', methodClass(ep.method)]">{{ ep.method }}</span>
          <code>{{ ep.path }}</code>
        </div>
        <p>{{ ep.desc }}</p>
        <button class="outline-btn compact-btn" @click="open(ep)">调用</button>
      </article>
    </div>
  </section>
  <section class="card data-list-card ai-playground">
    <div class="section-head"><div><p class="eyebrow">AI 业务接口（需 JWT）</p><h2>AI 接口</h2></div></div>
    <div class="endpoint-grid">
      <article v-for="(ep, idx) in grouped.ai" :key="ep.id" :style="{ '--i': idx }" class="endpoint-card">
        <div class="endpoint-head">
          <span :class="['method-badge', methodClass(ep.method)]">{{ ep.method }}</span>
          <code>{{ ep.path }}</code>
        </div>
        <p>{{ ep.desc }}</p>
        <button class="outline-btn compact-btn" @click="open(ep)">调用</button>
      </article>
    </div>
  </section>

  <Transition name="modal">
    <BaseModal v-if="active" @close="close">
      <section class="modal-card admin-editor ai-playground-modal">
        <p class="eyebrow">接口调试</p>
        <h2><span v-if="active" :class="['method-badge', methodClass(active.method)]">{{ active.method }}</span> {{ active?.path }}</h2>
        <p v-if="active" class="muted">{{ active.desc }}</p>

        <label v-if="active && active.method !== 'GET'">请求体（JSON）
          <textarea v-model="bodyJson" rows="10" spellcheck="false"></textarea>
        </label>

        <div v-if="successMsg" class="empty success-state">{{ successMsg }}</div>

        <div v-if="errorMsg" class="empty error-state">{{ errorMsg }}</div>

        <details v-if="response && !successMsg" class="response-details" open>
          <summary>响应结果</summary>
          <textarea :value="response" rows="14" readonly spellcheck="false"></textarea>
        </details>

        <div class="modal-actions">
          <button type="button" class="outline-btn" :disabled="sending" @click="close">关闭</button>
          <button type="button" class="primary-btn" :disabled="sending" @click="send">{{ sending ? '发送中…' : '发送请求' }}</button>
        </div>
      </section>
    </BaseModal>
  </Transition>
</template>

<style scoped>
.ai-playground.endpoint-grid,
.ai-playground .endpoint-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
  padding: 18px 25px 22px;
}
.endpoint-card { border: 1px solid var(--line); padding: 14px; background: #fff; display: flex; flex-direction: column; gap: 10px; }
.endpoint-card p { color: var(--muted); font-size: 12px; margin: 0; }
.endpoint-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.endpoint-head code { font: 12px var(--mono); color: var(--ink); word-break: break-all; }
.method-badge { font: 700 10px var(--mono); padding: 3px 7px; border: 1px solid var(--line); letter-spacing: .04em; }
.method-badge.method-get { color: #176331; background: #e2f6ca; border-color: #8fc55a; }
.method-badge.method-post { color: #1f3a8a; background: #dbe4ff; border-color: #6f86d3; }
.method-badge.method-patch { color: #7a4b00; background: #ffe7c2; border-color: #d8a44a; }
.method-badge.method-delete { color: #a22d1a; background: #ffe1da; border-color: #f09682; }
.ai-playground-modal { width: min(720px, 100%); max-height: 90vh; overflow: auto; }
.ai-playground-modal h2 { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.ai-playground-modal .muted { color: var(--muted); font-size: 12px; margin: 0 0 10px; }
.ai-playground-modal textarea { width: 100%; margin-top: 6px; border: 1px solid var(--ink); background: #fff; padding: 10px; font: 12px var(--mono); resize: vertical; }
.ai-playground-modal .success-state { color: #176331; background: #e2f6ca; border: 1px solid #8fc55a; padding: 10px 14px; font-weight: 700; }
.response-details { margin-top: 10px; }
.response-details summary { cursor: pointer; font: 800 10px var(--mono); letter-spacing: .08em; color: var(--muted); padding: 6px 0; }
.modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 14px; }
</style>
