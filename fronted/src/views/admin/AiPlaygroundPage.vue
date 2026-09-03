<script setup lang="ts">
import { computed, ref } from 'vue'
import { api, getErrorMessage } from '../../api/request'
import BaseModal from '../../components/BaseModal.vue'
import PageHeader from '../../components/BasePageHeader.vue'

type Method = 'GET' | 'POST' | 'PATCH' | 'DELETE'
type Endpoint = { id:string; group:'gateway'|'ai'; method:Method; path:string; desc:string; sample:Record<string,unknown>; pathParam?:string; queryParams?:string[] }

const endpoints:Endpoint[] = [
  { id:'gw-generate', group:'gateway', method:'POST', path:'/api/v1/gateway/generate', desc:'高阶文本生成（无 token）', sample:{ messages:[{ role:'user', content:'你好，请用一句话介绍你自己。' }], scene:'gateway_api', temperature:0.7, maxTokens:500 } },
  { id:'gw-chat', group:'gateway', method:'POST', path:'/api/v1/gateway/chat/completions', desc:'OpenAI 兼容 chat completions（无 token）', sample:{ messages:[{ role:'user', content:'你好' }], temperature:0.7, max_tokens:500, stream:false } },
  { id:'ai-chat', group:'ai', method:'POST', path:'/api/v1/ai/chat', desc:'AI 生涯问答（需 JWT；studentRef 留空走登录用户，如需覆盖需与 JWT 一致）', sample:{ sessionId:'sess-demo', question:'请帮我推荐几个方向' } },
  { id:'ai-chat-fb', group:'ai', method:'POST', path:'/api/v1/ai/chat/{messageId}/feedback', desc:'针对某条消息的反馈', pathParam:'messageId', sample:{ feedbackType:'HELPFUL', comment:'回答很有帮助' } },
  { id:'ai-chat-fb-fb', group:'ai', method:'POST', path:'/api/v1/ai/chat/feedback', desc:'fallback 反馈（最新消息）', sample:{ feedbackType:'NEUTRAL' } },
  { id:'ai-chat-history', group:'ai', method:'GET', path:'/api/v1/ai/chat/history', desc:'历史记录分页', queryParams:['page','size'], sample:{} },
  { id:'ai-review-sum', group:'ai', method:'POST', path:'/api/v1/ai/review/summarize', desc:'复盘摘要', sample:{ reviewId:'rev-001' } },
  { id:'ai-rec-explain', group:'ai', method:'POST', path:'/api/v1/ai/recommendation/explain', desc:'推荐解释', sample:{ runId:'run-001', studentRef:'STU-001', profile:{ interest:0.72, values:0.65, ability:0.80, academic:0.78, tendency:0.60, practice:0.55 }, results:[ { directionId:'DIR-FE', score:0.86, rank:1 }, { directionId:'DIR-BE', score:0.81, rank:2 }, { directionId:'DIR-DS', score:0.74, rank:3 } ] } },
  { id:'ai-plan-gen', group:'ai', method:'POST', path:'/api/v1/ai/plan/generate', desc:'计划生成', sample:{ directionId:'dir-001' } },
  { id:'ai-pdf-parse', group:'ai', method:'POST', path:'/api/v1/ai/pdf/parse', desc:'PDF 解析（JSON：jobId + 远程 fileUrl + filename；后端 fetch(fileUrl) 拿数据）', sample:{ jobId:'job-001', fileUrl:'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf', filename:'dummy.pdf' } },
]

const grouped = computed(() => {
  const gw = endpoints.filter(e => e.group === 'gateway')
  const ai = endpoints.filter(e => e.group === 'ai')
  return { gw, ai }
})

const active = ref<Endpoint | null>(null)
const pathParam = ref('')
const queryJson = ref('{}')
const bodyJson = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
const fileName = ref('')
const sending = ref(false)
const response = ref('')
const errorMsg = ref('')
const historyLoading = ref(false)
const historyPreview = ref('')
const historyError = ref('')
const historyList = ref<{messageId:string;role:string;content:string;createdAt?:string}[]>([])
const historySelectedId = ref('')
const successMsg = ref('')

function open(ep:Endpoint) {
  active.value = ep
  queryJson.value = '{}'
  bodyJson.value = JSON.stringify(ep.sample, null, 2)
  response.value = ''
  errorMsg.value = ''
  successMsg.value = ''
  historyPreview.value = ''
  historyError.value = ''
  historyList.value = []
  historySelectedId.value = ''
  pathParam.value = ''
  // 打开「针对某条消息的反馈」时，自动调用 chatHistory 预填 messageId 并展示历史下拉，
  // 避免用户不知道该填哪个 ID。加载失败则留空，提示手动输入。
  if (ep.id === 'ai-chat-fb') autoFillMessageId()
  fileName.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

/** 打开反馈模态时，自动从 chatHistory（Phase 1 后返回分页列表）拉取最新消息，
 *  默认选中最新一条 assistant；用户也可在下拉里切换或手动改文本框。 */
async function autoFillMessageId() {
  historyLoading.value = true
  historyError.value = ''
  historyPreview.value = ''
  historyList.value = []
  historySelectedId.value = ''
  pathParam.value = ''
  try {
    const data:any = await api.ai.chatHistory({ page: 1, size: 20 })
    const list:any[] = Array.isArray(data?.list) ? data.list : []
    // 仅展示 assistant 消息（feedback 只能给 assistant 加）
    historyList.value = list.filter((m:any) => m && m.role === 'assistant')
    if (historyList.value.length === 0) {
      historyPreview.value = '（暂无历史 assistant 回答，请先调一次 /ai/chat 再来）'
      return
    }
    // 默认选最新一条
    const first = historyList.value[0]
    historySelectedId.value = String(first.messageId ?? '')
    pathParam.value = historySelectedId.value
    applyHistoryPreview()
  } catch (e) {
    historyError.value = `自动获取失败，请手动输入 messageId：${getErrorMessage(e)}`
    historyPreview.value = ''
  } finally {
    historyLoading.value = false
  }
}

function applyHistoryPreview() {
  const m = historyList.value.find((x) => x.messageId === historySelectedId.value)
  if (!m) { historyPreview.value = ''; return }
  const text = String(m.content ?? '').trim()
  historyPreview.value = text ? `💬 预览：${text.slice(0, 50)}${text.length > 50 ? '…' : ''}` : '（该消息无内容预览）'
}

function onSelectHistory(id:string) {
  historySelectedId.value = id
  pathParam.value = id
  applyHistoryPreview()
}

function close() {
  if (sending.value) return
  active.value = null
}

function resolvePath(ep:Endpoint) {
  let p = ep.path
  if (ep.pathParam) p = p.replace(`{${ep.pathParam}}`, encodeURIComponent(pathParam.value || ''))
  if (ep.method === 'GET' && queryJson.value.trim()) {
    try {
      const obj = JSON.parse(queryJson.value)
      const params = new URLSearchParams()
      Object.entries(obj).forEach(([k, v]) => { if (v !== '' && v !== null && v !== undefined) params.set(k, String(v)) })
      const qs = params.toString()
      if (qs) p += `?${qs}`
    } catch { /* keep as is */ }
  }
  return p
}

async function send() {
  if (!active.value) return
  const ep = active.value
  sending.value = true
  errorMsg.value = ''
  response.value = ''
  successMsg.value = ''
  try {
    const internalPath = resolvePath(ep).replace(/^\/api\/v1/, '')
    let data:any
    if (ep.method === 'GET') {
      const queryObj = (() => { try { return JSON.parse(queryJson.value || '{}') } catch { throw new Error('查询参数 JSON 解析失败') } })()
      if (ep.id === 'ai-chat-history') data = await api.ai.chatHistory(queryObj)
      else data = await (api as any).request(internalPath)
    } else {
      const body = (() => { try { return JSON.parse(bodyJson.value || '{}') } catch { throw new Error('请求体 JSON 解析失败') } })()
      if (ep.id === 'ai-pdf-parse') {
        // /ai/pdf/parse 后端契约：application/json（jobId + fileUrl + filename），后端 fetch(fileUrl) 拿数据。
        // 不要走 multipart（之前 formPost 会让 Spring 抛 HttpMessageNotReadableException → "未预期系统错误"）。
        const file = fileInput.value?.files?.[0]
        if (file && !body.filename) body.filename = file.name
        data = await api.ai.pdfParse(body)
      } else if (ep.id === 'gw-generate') data = await api.gateway.generate(body)
      else if (ep.id === 'gw-chat') data = await api.gateway.chatCompletions(body)
      else if (ep.id === 'ai-chat') {
        data = await api.ai.chat(body)
      }
      else if (ep.id === 'ai-chat-fb') {
        if (!pathParam.value) throw new Error('请填写 messageId 路径参数')
        data = await api.ai.chatFeedback(pathParam.value, body)
      }
      else if (ep.id === 'ai-chat-fb-fb') data = await api.ai.chatFeedbackFallback(body)
      else if (ep.id === 'ai-review-sum') data = await api.ai.reviewSummarize(body)
      else if (ep.id === 'ai-rec-explain') data = await api.ai.recommendationExplain(body)
      else if (ep.id === 'ai-plan-gen') data = await api.ai.planGenerate(body)
      else data = await (api as any).request(internalPath, { method: ep.method, body: JSON.stringify(body) })
    }
    response.value = JSON.stringify(data, null, 2)
    // 后端部分接口（如反馈、计划生成兜底）成功时返回空 Map，肉眼看不出成功。
    // 这里统一给个友好提示，并隐藏空响应框，避免误以为失败。
    const isEmpty = data === undefined || data === null || (typeof data === 'object' && !Array.isArray(data) && Object.keys(data).length === 0)
    successMsg.value = isEmpty ? '✅ 请求成功（后端无返回内容）' : ''
  } catch (e) {
    errorMsg.value = getErrorMessage(e)
    successMsg.value = ''
  } finally {
    sending.value = false
  }
}

function pickFile() {
  const f = fileInput.value?.files?.[0]
  fileName.value = f ? f.name : ''
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

        <label v-if="active?.pathParam">路径参数 {{ active.pathParam }}
          <select v-if="active.id === 'ai-chat-fb' && historyList.length > 0" :value="historySelectedId" @change="(e:any) => onSelectHistory(String(e.target.value))">
            <option v-for="m in historyList" :key="m.messageId" :value="m.messageId">{{ (m.messageId || '').slice(0, 8) }} — {{ (m.content || '').slice(0, 40) }}{{ (m.content || '').length > 40 ? '…' : '' }}</option>
          </select>
          <input v-model.trim="pathParam" :disabled="historyLoading" :placeholder="historyLoading ? '正在加载历史消息…' : `输入 ${active.pathParam}`" />
          <small v-if="active.id === 'ai-chat-fb' && historyPreview" class="history-preview">{{ historyPreview }}</small>
          <small v-if="active.id === 'ai-chat-fb' && historyError" class="history-error">{{ historyError }}</small>
        </label>

        <label v-if="active && active.method === 'GET' && active.queryParams?.length">查询参数（JSON）
          <textarea v-model="queryJson" rows="3" placeholder='{"page":1,"size":20}'></textarea>
        </label>

        <label v-if="active && active.method !== 'GET' && active.id !== 'ai-pdf-parse'">请求体（JSON）
          <textarea v-model="bodyJson" rows="10" spellcheck="false"></textarea>
        </label>

        <label v-if="active?.id === 'ai-pdf-parse'">选择 PDF 文件（可选 — 仅用于自动填 filename 字段）
          <input ref="fileInput" type="file" accept="application/pdf" @change="pickFile" />
          <small v-if="fileName">已选择：{{ fileName }}（会被写入请求体 filename）</small>
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
.ai-playground-modal input[type=file] { margin-top: 6px; }
.ai-playground-modal .success-state { color: #176331; background: #e2f6ca; border: 1px solid #8fc55a; padding: 10px 14px; font-weight: 700; }
.ai-playground-modal .history-preview { color: var(--muted); font-size: 11px; margin-top: 6px; }
.ai-playground-modal .history-error { color: #a22d1a; font-size: 11px; margin-top: 6px; }
.response-details { margin-top: 10px; }
.response-details summary { cursor: pointer; font: 800 10px var(--mono); letter-spacing: .08em; color: var(--muted); padding: 6px 0; }
.modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 14px; }
</style>
