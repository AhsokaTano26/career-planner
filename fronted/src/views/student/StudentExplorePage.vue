<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../../components/BasePageHeader.vue'
import { api, getErrorMessage } from '../../api/request'
import { useStudent } from '../../composables/useStudent'
import { useToast } from '../../composables/useToast'
import {
  PATH_OPTIONS,
  PROBE_LIMIT,
  TAG_LIMIT,
  addProbeAnswer,
  buildExplorationRequest,
  emptyState,
  probesRemaining,
  remainingInputs,
  selectPath,
  tagsRemaining,
  toggleTag,
  type ExploreAnswer,
  type ExploreState,
  type PathValue,
} from './studentExplore'

type Row = Record<string, any>
type ProbeMessage = { role: 'ai' | 'user'; text: string; aiGenerated?: boolean }

const { show } = useToast()
const student = useStudent()

const stage = ref<'welcome' | 'path' | 'tags' | 'probe' | 'result'>('welcome')
const state = ref<ExploreState>(emptyState())
const tagQuestions = ref<Row[]>([])
const busy = ref<'load' | 'submit' | 'probe' | ''>('')
const result = ref<Row | null>(null)
const portraitSource = computed(() => result.value?.portraitSource as string | undefined)
const committedPath = ref<string>('')
const probe = ref<Row | null>(null)
const probeMessages = ref<ProbeMessage[]>([])
const probeExhausted = ref(false)

const usedCount = computed(() => (state.value.path ? 1 : 0) + state.value.tagIds.length + state.value.probeAnswers.length)
const remaining = computed(() => remainingInputs(state.value))
const tagsLeft = computed(() => tagsRemaining(state.value))
const probesLeft = computed(() => probesRemaining(state.value))
const canSubmit = computed(() => Boolean(state.value.path))

const dimensionScores = computed<Row[]>(() => result.value?.dimensionScores ?? [])
const portrait = computed<Row | null>(() => result.value?.portrait ?? null)
const portraitDimensions = computed<Row[]>(() => portrait.value?.dimensions ?? [])
const recommendation = computed<Row | null>(() => result.value?.recommendation ?? null)
const recommendations = computed<Row[]>(() => recommendation.value?.results ?? [])

const pathLabel = computed(() => PATH_OPTIONS.find((o) => o.value === state.value.path)?.label ?? '')

function list(data: unknown): Row[] {
  if (Array.isArray(data)) return data
  const any = data as Row
  return Array.isArray(any?.list) ? any.list : (Array.isArray(any?.items) ? any.items : [])
}

async function loadTags() {
  busy.value = 'load'
  try {
    await student.load()
    const questionnaires = list(await api.student.questionnaires())
    const explore = questionnaires.find((q) => q.type === 'EXPLORE' && q.status === 'PUBLISHED')
    if (!explore?.id) {
      tagQuestions.value = []
      return
    }
    const detail = (await api.student.questionnaire(String(explore.id))) as Row
    tagQuestions.value = list((detail as Row)?.questions).filter((q) => q?.dim === 'EXPLORE_TAG')
  } catch (e) {
    show(getErrorMessage(e))
  } finally {
    busy.value = ''
  }
}

onMounted(loadTags)

function choosePath(value: PathValue) {
  state.value = selectPath(state.value, value)
  stage.value = 'tags'
}

function chooseTag(id: string) {
  state.value = toggleTag(state.value, id)
}

function selected(id: string) {
  return state.value.tagIds.includes(id)
}

function answeredIds(): string[] {
  return [...state.value.tagIds, ...state.value.probeAnswers.map((a) => a.questionId)]
}

async function requestProbe() {
  if (busy.value) return
  busy.value = 'probe'
  try {
    const payload: { path?: string; answeredQuestionIds: string[]; askedCount: number } = {
      answeredQuestionIds: answeredIds(),
      askedCount: state.value.probeAnswers.length,
    }
    if (state.value.path) payload.path = state.value.path
    const next = (await api.student.createExplorationProbe(payload)) as Row
    if (next?.exhausted) {
      probeExhausted.value = true
      probe.value = null
      return
    }
    probe.value = next
    if (next?.intro || next?.questionText) {
      probeMessages.value.push({
        role: 'ai',
        text: [next.intro, next.questionText].filter(Boolean).join(''),
        aiGenerated: next.aiGenerated !== false,
      })
    }
  } catch (e) {
    show(getErrorMessage(e))
  } finally {
    busy.value = ''
  }
}

async function startProbe() {
  probeExhausted.value = false
  stage.value = 'probe'
  if (!probe.value && !probeMessages.value.length) {
    await requestProbe()
  }
}

async function answerProbe(optionIndex: number) {
  if (!probe.value?.questionId || busy.value) return
  state.value = addProbeAnswer(state.value, String(probe.value.questionId), optionIndex)
  const chosen = (probe.value.options as Row[] | undefined)?.[optionIndex]
  if (chosen?.text) {
    probeMessages.value.push({ role: 'user', text: String(chosen.text) })
  }
  probe.value = null
  if (probesRemaining(state.value) === 0) {
    probeExhausted.value = true
    return
  }
  await requestProbe()
}

async function submit() {
  if (!canSubmit.value || busy.value) return
  busy.value = 'submit'
  try {
    if (state.value.path && state.value.path !== committedPath.value) {
      await student.saveProfile({
        gender: '', hometown: '', birthday: '', phone: '',
        math: '', english: '', programming: '', academicNote: '',
        abilityProgramming: '', abilityMath: '', abilityEnglish: '', communication: '', organization: '',
        interests: '', values: '', constraints: '',
        developmentIntention: state.value.path,
      })
      committedPath.value = state.value.path
    }
    const req = buildExplorationRequest(state.value)
    const payload: { path?: string; answers: ExploreAnswer[] } = { answers: req.answers }
    if (req.path) payload.path = req.path
    result.value = (await api.student.createExploration(payload)) as Row
    stage.value = 'result'
    show('已生成个性化方向推荐')
  } catch (e) {
    show(getErrorMessage(e))
  } finally {
    busy.value = ''
  }
}

function restart() {
  state.value = emptyState()
  result.value = null
  committedPath.value = ''
  probe.value = null
  probeMessages.value = []
  probeExhausted.value = false
  stage.value = 'welcome'
}

function flatLines(values: unknown): string {
  return Array.isArray(values) ? values.filter(Boolean).join('；') : '—'
}

function confidenceCn(value: unknown): string {
  const map: Record<string, string> = { HIGH: '匹配度高', MEDIUM: '匹配度中', LOW: '匹配度一般' }
  return map[String(value)] ?? String(value ?? '—')
}
</script>

<template>
  <PageHeader
    eyebrow="生涯探索"
    title="快速定位你的发展方向"
    description="3 步以内完成探索：选择方向、点选兴趣，随时查看个性化推荐。"
  />

  <section v-if="stage === 'welcome'" class="card">
    <p class="eyebrow">第 1 步 · 欢迎</p>
    <h2>只需几次点选，即可完成探索</h2>
    <p class="muted">系统会根据你的选择生成画像与方向推荐。本次探索累计主动输入不超过 10 次（路径 1 + 兴趣 ≤3 + 后续引导）。</p>
    <div class="form-actions">
      <button class="primary-btn" @click="stage = 'path'">开始探索</button>
    </div>
  </section>

  <section v-if="stage === 'path'" class="card">
    <p class="eyebrow">第 2 步 · 选择发展方向（还可以选 {{ remaining }} 项）</p>
    <h2>你现在更想走哪条路？</h2>
    <div class="direction-grid">
      <article v-for="option in PATH_OPTIONS" :key="option.value">
        <h3>{{ option.label }}</h3>
        <p v-if="option.value === 'undecided'">还没想好，先看看兴趣再定。</p>
        <p v-else>将为你优先匹配该路径的发展方向。</p>
        <button class="primary-btn" @click="choosePath(option.value)">选择</button>
      </article>
    </div>
  </section>

  <section v-if="stage === 'tags'" class="development-stack">
    <article class="card">
      <p class="eyebrow">第 3 步 · 点选兴趣（已选 {{ pathLabel }}，剩余可选项 {{ tagsLeft }} 个）</p>
      <h2>哪些领域让你感兴趣？</h2>
      <p v-if="busy === 'load'" class="muted">正在读取兴趣标签…</p>
      <div v-else class="choice-column">
        <button
          v-for="question in tagQuestions"
          :key="question.id"
          :class="{ selected: selected(String(question.id)) }"
          :disabled="!selected(String(question.id)) && tagsLeft === 0"
          @click="chooseTag(String(question.id))"
        >
          {{ question.text }}
        </button>
        <p v-if="!tagQuestions.length" class="empty">暂无兴趣标签，请联系管理员配置探索问卷。</p>
      </div>
      <div class="form-actions">
        <button class="outline-btn" @click="stage = 'path'">返回上一步</button>
        <button class="outline-btn" :disabled="busy === 'probe'" @click="startProbe">
          {{ busy === 'probe' ? '正在准备引导…' : '继续聊聊（智能引导）' }}
        </button>
        <button class="primary-btn" :disabled="!canSubmit || busy === 'submit'" @click="submit">
          {{ busy === 'submit' ? '正在生成推荐…' : '就按现在的出结果' }}
        </button>
      </div>
      <p class="muted">最多选择 {{ TAG_LIMIT }} 个兴趣标签；可跳过标签直接出结果（只选路径），也可以让智能引导继续深挖（最多 {{ PROBE_LIMIT }} 问）。</p>
    </article>
  </section>

  <section v-if="stage === 'probe'" class="development-stack">
    <article class="card">
      <p class="eyebrow">第 4 步 · 智能引导（还可回答 {{ probesLeft }} 问，本次已用 {{ usedCount }} / 10 次）</p>
      <h2>聊聊你的偏好</h2>
      <p class="muted">对话内容仅自己可见，辅导员端不可见。</p>
      <div class="simple-list">
        <div v-for="(msg, index) in probeMessages" :key="index">
          <b>{{ msg.role === 'ai' ? '智能引导' : '我的选择' }}{{ msg.role === 'ai' && msg.aiGenerated === false ? '（模板）' : '' }}</b>
          <p>{{ msg.text }}</p>
        </div>
        <p v-if="busy === 'probe'" class="muted">正在组织下一个问题…</p>
        <p v-if="probeExhausted" class="muted">引导问题已问完，可直接出结果。</p>
      </div>
      <div v-if="probe && !probeExhausted" class="choice-column">
        <button
          v-for="(option, optionIndex) in (probe.options as Row[])"
          :key="option.id || optionIndex"
          :disabled="busy === 'probe'"
          @click="answerProbe(optionIndex)"
        >
          {{ option.text }}
        </button>
      </div>
      <div class="form-actions">
        <button class="outline-btn" @click="stage = 'tags'">返回标签</button>
        <button class="primary-btn" :disabled="!canSubmit || busy === 'submit'" @click="submit">
          {{ busy === 'submit' ? '正在生成推荐…' : '就按现在的出结果' }}
        </button>
      </div>
    </article>
  </section>

  <section v-if="stage === 'result'" class="development-stack">
    <article class="card">
      <div class="section-head">
        <div>
          <p class="eyebrow">探索结果 · 本次主动输入 {{ usedCount }} 次</p>
          <h2>你的个性化方向推荐</h2>
        </div>
        <button class="outline-btn" @click="restart">重新探索</button>
      </div>
      <p v-if="portraitSource === 'LEGACY'" class="muted">
        本次未填写兴趣标签，画像沿用了你之前完成的测评结果；如需更新，请重新选择标签后再次出结果。
      </p>
      <p v-else-if="portraitSource === 'PROFILE'" class="muted">
        本次未填写兴趣标签，画像按你的档案估算生成；补充兴趣标签可获得更精准的推荐。
      </p>
    </article>

    <article class="card">
      <p class="eyebrow">六维能力概览</p>
      <h2>{{ portrait?.summary ?? '画像生成中' }}</h2>
      <div class="metric-grid">
        <div v-for="item in dimensionScores.length ? dimensionScores : portraitDimensions" :key="item.dimensionCode || item.key">
          <small>{{ item.dimensionName || item.name }}</small>
          <b>{{ Number(item.score ?? 0).toFixed(1) }}</b>
        </div>
      </div>
      <div v-if="portrait" class="two-column">
        <div><h3>优势</h3><p>{{ flatLines(portrait.strengths) }}</p></div>
        <div><h3>待探索</h3><p>{{ flatLines(portrait.explore) }}</p></div>
      </div>
    </article>

    <article class="card">
      <p class="eyebrow">推荐结果 · {{ recommendation?.ruleVersionName ?? '' }}</p>
      <h2>为你匹配的发展方向</h2>
      <div v-if="recommendations.length" class="simple-list">
        <div v-for="item in recommendations" :key="item.resultId">
          <b>第 {{ item.rank }} 位 · {{ item.directionName || item.directionId }}</b>
          <span>匹配度 {{ Number(item.score ?? 0).toFixed(1) }} · {{ confidenceCn(item.confidence) }}</span>
          <p class="muted">{{ flatLines(item.reasons) }}</p>
        </div>
      </div>
      <p v-else class="empty">当前方向库暂无匹配结果，可稍后重试或联系辅导员。</p>
    </article>
  </section>
</template>

<style scoped>
.muted {
  color: var(--muted, #6f7269);
  font-size: 0.875rem;
  line-height: 1.6;
}
</style>
