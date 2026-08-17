<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api, clearAuthSession, getErrorMessage, hasAccessToken, hasRefreshToken, setAuthTokens, type Completeness, type ConsentStatus, type Experience, type Profile, type User } from './api'

type Role = 'STUDENT' | 'ADVISOR' | 'ADMIN'
type Item = { id:string; title:string; desc:string; status?:string; meta?:string }
type AdvisorStudent = { id:string; name:string; className?:string; completeness?:number; assessed?:boolean; path?:string; direction?:string; primaryGoal?:string; planRate?:number; lastReview?:string; askGuidance?:boolean; status?:string }
type AdvisorAttention = { student:AdvisorStudent; reasons:string[] }
type AdvisorStatistics = { totalStudents?:number; assessedCount?:number; planMadeCount?:number; reviewedCount?:number; taskCompletionRate?:number; pathDistribution?:{path:string;count:number}[] }
type Guidance = { id:string; studentId:string; content:string; adviceType:string; suggestedTask?:string; retestReason?:string; createdAt?:string }
const role = ref<Role>('STUDENT'), active = ref('overview'), loggedIn = ref(false), authMode = ref<'login'|'register'|'reset'>('login')
const showForm = ref(false), toast = ref(''), keyword = ref(''), loading = ref(false), error = ref('')
const account = ref(''), password = ref(''), studentNo = ref(''), name = ref(''), className = ref(''), verifyCode = ref('')
const experienceType = ref('project'), experienceTitle = ref(''), experienceStart = ref(''), experienceEnd = ref(''), experienceDescription = ref(''), experienceAttachment = ref('')
const editingExperienceId = ref<string | null>(null), profileEditor = ref(false), profileSaving = ref(false)
const consentStatus = ref<ConsentStatus | null>(null), consentAgreed = ref<boolean | null>(null), consentSubmitting = ref(false), privacyAction = ref<'password'|'delete'|null>(null), privacySubmitting = ref(false), oldPassword = ref(''), newPassword = ref(''), deletionReason = ref('')
const advisorStudent = ref<{id:string;detail:unknown;guidance:Guidance[]} | null>(null), guidanceContent=ref(''), guidanceType=ref<'COMMENT'|'SUGGEST_TASK'|'SUGGEST_RETEST'>('COMMENT'), guidanceSuggestedTask=ref(''), guidanceRetestReason=ref(''), guidanceSubmitting=ref(false)
const advisorStudents = ref<AdvisorStudent[]>([]), advisorAttention = ref<AdvisorAttention[]>([]), advisorStatistics = ref<AdvisorStatistics | null>(null)
const advisorLoading = ref(false), advisorError = ref(''), advisorTotal = ref(0), advisorPage = ref(1), advisorTotalPages = ref(1)
const advisorFilters = ref({ keyword:'', path:'', goalStatus:'', reviewStatus:'', guidanceRequested:'', sort:'-createdAt' })
const profileForm = ref({ gender:'', hometown:'', birthday:'', phone:'', math:'', english:'', programming:'', academicNote:'', abilityProgramming:'', abilityMath:'', abilityEnglish:'', communication:'', organization:'', interests:'', values:'', developmentIntention:'undecided', constraints:'' })
const currentUser = ref<User | null>(null), profile = ref<Profile | null>(null), completeness = ref<Completeness | null>(null), experiences = ref<Experience[]>([])
const remoteRecords = ref<Record<string, Item[]>>({}), remoteLoading = ref(false), remoteError = ref(''), remoteSearch = ref('')
const createValues = ref<Record<string,string>>({}), creating = ref(false)
const unsupportedPages = ['assessment','portrait','directions','recommendations','compare','favorites','goals','plans','tasks','reviews','reminders','chat','questionnaires','models']
const roleNames: Record<Role, string> = { STUDENT:'学生工作台', ADVISOR:'辅导员工作台', ADMIN:'系统管理台' }
const menus: Record<Role, {group:string; links:[string,string][]}[]> = {
  STUDENT:[{group:'我的生涯',links:[['overview','生涯总览'],['profile','个人资料'],['experiences','经历管理'],['assessment','生涯测评'],['portrait','能力画像']]},{group:'探索与行动',links:[['directions','路径探索'],['recommendations','方向推荐'],['compare','方向对比'],['favorites','我的收藏'],['goals','目标管理'],['plans','学期计划'],['tasks','任务看板'],['reviews','阶段复盘']]},{group:'支持与设置',links:[['reminders','任务提醒'],['chat','AI 咨询'],['privacy','隐私与账户']]}],
  ADVISOR:[{group:'辅导工作',links:[['advisor-overview','工作总览'],['students','学生列表'],['attention','重点关注'],['guidance','指导记录'],['statistics','群体统计']]}],
  ADMIN:[{group:'平台配置',links:[['admin-overview','运行总览'],['users','用户管理'],['whitelist','白名单'],['relations','师生关系'],['questionnaires','问卷管理'],['admin-directions','方向库'],['abilities','能力标签'],['templates','任务模板']]},{group:'内容与治理',links:[['curricula','培养方案'],['weights','推荐权重'],['models','模型与提示词'],['exports','导出任务'],['logs','审计日志']]}]
}
const scores = [{label:'兴趣倾向',value:82},{label:'价值取向',value:76},{label:'能力基础',value:68},{label:'学业基础',value:74},{label:'发展倾向',value:79},{label:'实践经历',value:56}]
const tasks = ref<Item[]>([{id:'T-001',title:'完成数据分析小项目',desc:'使用公开数据完成选题、分析与展示',status:'进行中',meta:'截止 10/28'},{id:'T-002',title:'参加一次技术分享',desc:'记录三个感兴趣的技术方向',status:'已完成',meta:'10/12'},{id:'T-003',title:'完成算法基础练习',desc:'每周完成 3 道基础题',status:'未开始',meta:'11 月'}])
const records: Record<string, Item[]> = {
  profile:[{id:'P-01',title:'基本信息',desc:'专业、年级、兴趣方向与发展期待',status:'完整度 92%'},{id:'P-02',title:'教育与经历',desc:'课程基础、项目与社团经历',status:'可编辑'}],
  assessment:[{id:'A-01',title:'霍兰德兴趣测评',desc:'30 题 · 约 8 分钟',status:'已完成'},{id:'A-02',title:'职业价值观测评',desc:'24 题 · 约 6 分钟',status:'已完成'},{id:'A-03',title:'能力自评与专业认知',desc:'28 题 · 约 8 分钟',status:'待完成'}],
  directions:[{id:'PATH-01',title:'国内升学',desc:'以课程、科研体验和升学信息准备为主',status:'3 个方向'},{id:'PATH-02',title:'就业发展',desc:'通过项目、实习与能力作品逐步验证',status:'4 个方向'},{id:'PATH-03',title:'出国留学',desc:'关注语言能力、课程成绩与申请节奏',status:'2 个方向'}],
  recommendations:[{id:'R-01',title:'智能计算与算法方向',desc:'兴趣、能力基础与发展倾向匹配度较高',status:'82 / 100'},{id:'R-02',title:'数据工程与分析方向',desc:'适合通过项目进一步确认真实偏好',status:'76 / 100'},{id:'R-03',title:'软件工程与产品开发',desc:'建议补充协作实践与用户视角',status:'71 / 100'}],
  goals:[{id:'G-01',title:'主目标：探索算法工程发展方向',desc:'本学期以课程、项目和访谈积累决策证据',status:'进行中'},{id:'G-02',title:'备选目标：数据分析方向',desc:'结合项目体验比较方向匹配度',status:'备选'}],
  plans:[{id:'PLAN-01',title:'2026-2027 第一学期计划',desc:'5 项可验证任务 · 当前完成 2 项',status:'已确认'},{id:'PLAN-02',title:'AI 生成计划草案',desc:'可修改任务、月份和完成标准后确认',status:'草稿'}],
  reviews:[{id:'REV-01',title:'十月阶段复盘',desc:'记录收获、困难、证据与下阶段调整',status:'草稿'},{id:'REV-02',title:'学期末复盘',desc:'完成后可生成下一阶段计划建议',status:'待开始'}],
  experiences:[{id:'EXP-01',title:'校园数据分析训练营',desc:'2026-09 · 负责公开数据清洗与图表呈现',status:'已验证'},{id:'EXP-02',title:'计算机协会技术分享',desc:'记录 AI 工具链与学习路径',status:'草稿'}],
  compare:[{id:'CMP-01',title:'智能计算与算法 vs 数据工程与分析',desc:'适配点：逻辑分析与持续学习；差距：项目经历与数据工具',status:'已选择 2 项'}],
  favorites:[{id:'FAV-01',title:'智能计算与算法',desc:'就业方向 · 关联能力 8 项 · 关联课程 12 门',status:'已收藏'},{id:'FAV-02',title:'数据工程与分析',desc:'就业方向 · 关联能力 7 项 · 关联课程 10 门',status:'已收藏'}],
  reminders:[{id:'REM-01',title:'算法基础练习即将开始',desc:'任务 T-003 将在 3 天后进入计划周期',status:'待处理'},{id:'REM-02',title:'阶段复盘待完善',desc:'十月复盘草稿已保存 7 天',status:'提醒'}],
  students:[{id:'2026011301',title:'李明 · 计算机科学与技术',desc:'主目标：算法工程方向 · 计划完成 40%',status:'需要关注'},{id:'2026011302',title:'张晨 · 软件工程',desc:'已申请指导 · 距上次复盘 38 天',status:'申请指导'},{id:'2026011315',title:'王琪 · 网络工程',desc:'暂无主目标 · 画像完整度 62%',status:'待跟进'}],
  attention:[{id:'AT-01',title:'张晨',desc:'主动申请方向选择指导，已附复盘内容',status:'高优先级'},{id:'AT-02',title:'王琪',desc:'连续 30 天未更新计划或复盘',status:'提醒'}],
  guidance:[{id:'GD-01',title:'李明 · 方向探索建议',desc:'建议先完成数据分析项目，再确定主目标',status:'已发送'},{id:'GD-02',title:'张晨 · 复盘反馈',desc:'已针对实习焦虑提供资源与行动建议',status:'已发送'}],
  users:[{id:'U-1001',title:'李明',desc:'学生 · 计算机学院 · 状态正常',status:'已启用'},{id:'U-2001',title:'王老师',desc:'辅导员 · 负责 86 名学生',status:'已启用'}],
  whitelist:[{id:'WL-01',title:'2026011309',desc:'计算机学院 · 校验码已加密 · 未使用',status:'可注册'},{id:'WL-02',title:'2026011310',desc:'软件学院 · 由 CSV 导入',status:'可注册'}],
  relations:[{id:'REL-01',title:'王老师 — 李明',desc:'计算机学院 · 2026 级',status:'有效'},{id:'REL-02',title:'王老师 — 张晨',desc:'软件工程专业 · 2026 级',status:'有效'}],
  questionnaires:[{id:'Q-01',title:'生涯探索基础测评 V1.2',desc:'4 个题组 · 发布于 2026-08-01',status:'已发布'},{id:'Q-02',title:'能力自评补充问卷 V1.0',desc:'18 题 · 等待审核',status:'草稿'}],
  'admin-directions':[{id:'D-01',title:'智能计算与算法',desc:'就业路径 · 已配置能力要求与推荐权重',status:'已发布'},{id:'D-02',title:'数据工程与分析',desc:'就业路径 · 最近更新 2026-08-02',status:'已发布'}],
  abilities:[{id:'AB-01',title:'算法与数据结构',desc:'类别：专业基础 · 关联 12 个方向',status:'启用'},{id:'AB-02',title:'沟通协作',desc:'类别：通用能力 · 关联 9 个任务',status:'启用'}],
  templates:[{id:'TP-01',title:'数据分析项目实践',desc:'适用：数据方向 · 4 周 · 可验证产出',status:'已发布'},{id:'TP-02',title:'专业访谈记录',desc:'适用：全路径 · 1 周',status:'已发布'}],
  curricula:[{id:'CV-01',title:'计算机科学与技术培养方案 2026',desc:'82 门课程 · 解析置信度 96%',status:'已发布'},{id:'JOB-07',title:'培养方案 PDF 导入任务',desc:'6 条待人工校核条目',status:'待审核'}],
  weights:[{id:'W-01',title:'推荐规则 V1.0',desc:'兴趣 20% · 价值观 15% · 能力 25% · 学业 15% · 倾向 20% · 经历 5%',status:'已发布'}],
  models:[{id:'M-01',title:'推荐解释模型',desc:'Prompt V1.3 · 最近调用成功率 99.1%',status:'已发布'},{id:'M-02',title:'计划生成模型',desc:'Prompt V1.1 · 输出需学生确认',status:'已发布'}],
  exports:[{id:'EX-01',title:'2026 秋季学生画像汇总',desc:'CSV · 1,248 条 · 创建于今天 10:24',status:'已完成'}],
  logs:[{id:'LOG-01',title:'AI 计划生成',desc:'操作人：李明 · 耗时 1.2s · Prompt V1.1',status:'成功'},{id:'LOG-02',title:'方向库发布',desc:'操作人：系统管理员 · 方向 D-02',status:'成功'}]
}
const titles: Record<string,[string,string,string]> = { overview:['CAREER / OVERVIEW','从了解自己开始','每一份认真回答，都会让你的下一步更清晰。'], 'advisor-overview':['ADVISOR / DASHBOARD','把需要的支持，送到学生身边','以学生状态、计划进度与复盘节奏组织指导工作。'], 'admin-overview':['ADMIN / OPERATIONS','配置、治理与可追溯','所有发布、导入、AI 调用和导出都保留操作记录。'], portrait:['PROFILE / SIX DIMENSIONS','此刻的你，有很多可能','能力画像会随着测评、经历和反馈持续更新。'], statistics:['ADVISOR / STATISTICS','群体生涯状态','查看学院学生的画像、目标与复盘分布。'], chat:['AI / CONSULTATION','带着问题来聊聊','智能生成内容只作为探索参考，不替代重要决定。'], privacy:['ACCOUNT / PRIVACY','隐私与账户','管理授权、密码、数据导出与删除申请。'] }
const currentTitle = computed(() => titles[active.value] || ['MODULE / '+active.value.toUpperCase(), menuLabel(active.value), ''])
const adminLoaders: Record<string, () => Promise<unknown>> = {
  users: api.admin.users, whitelist: api.admin.whitelist, relations: api.admin.relations, 'admin-directions': api.admin.directions,
  abilities: api.admin.abilities, templates: api.admin.templates, curricula: api.admin.curricula, weights: api.admin.weights,
  exports: api.admin.exports, logs: api.admin.operationLogs,
  'admin-overview': api.admin.exports,
}
const hasRemoteModule = computed(() => Boolean(adminLoaders[active.value]))
const createConfigs: Record<string, {title:string; fields:{key:string;label:string;required?:boolean;placeholder?:string;type?:string}[]}> = {
  whitelist:{title:'新增白名单',fields:[{key:'studentNo',label:'学号',required:true},{key:'className',label:'班级'},{key:'verifyCode',label:'校验码',placeholder:'仅字母或数字'}]},
  relations:{title:'建立师生关系',fields:[{key:'advisorId',label:'辅导员 ID',required:true},{key:'studentIds',label:'学生 ID',required:true,placeholder:'多个 ID 用逗号分隔'}]},
  'admin-directions':{title:'新增方向',fields:[{key:'id',label:'方向编码',required:true},{key:'name',label:'方向名称',required:true},{key:'path',label:'所属路径',placeholder:'例如 employment'},{key:'intro',label:'简介',type:'textarea'}]},
  abilities:{title:'新增能力标签',fields:[{key:'id',label:'标签编码',required:true},{key:'name',label:'标签名称',required:true},{key:'category',label:'分类'},{key:'status',label:'状态',placeholder:'ACTIVE'}]},
  templates:{title:'新增任务模板',fields:[{key:'id',label:'模板编码',required:true},{key:'directionId',label:'方向编码',required:true},{key:'name',label:'模板名称',required:true},{key:'goalSummary',label:'目标摘要',type:'textarea'},{key:'status',label:'状态',placeholder:'DRAFT'}]},
  exports:{title:'创建导出任务',fields:[{key:'type',label:'导出类型',required:true,placeholder:'STUDENT_DATA / WHITELIST / OPERATION_LOG / AI_LOG / DIRECTION_LIB'},{key:'scope',label:'导出范围说明',type:'textarea'}]},
}
const createConfig = computed(() => createConfigs[active.value])
const visibleRemoteRecords = computed(() => {
  const query = remoteSearch.value.trim().toLowerCase()
  const list = remoteRecords.value[active.value] || []
  return query ? list.filter(item => `${item.id} ${item.title} ${item.desc} ${item.status || ''}`.toLowerCase().includes(query)) : list
})
function statusClass(status?:string) {
  const value = (status || '').toUpperCase()
  if (/(ACTIVE|PUBLISHED|SUCCESS|COMPLETED|DONE|ENABLED|TRUE|正常|成功|已发布)/.test(value)) return 'success'
  if (/(FAILED|DISABLED|REJECTED|ERROR|停用|失败)/.test(value)) return 'danger'
  return 'neutral'
}
const advisorStatusLabels:Record<string,string> = { good:'状态良好', todo:'待完善', late:'长期未复盘', review:'申请指导' }
const advisorPathLabels:Record<string,string> = { graduate:'国内升学', employment:'就业发展', overseas:'出国留学', undecided:'待确定' }
const adviceTypeLabels:Record<Guidance['adviceType'],string> = { COMMENT:'指导意见', SUGGEST_TASK:'建议任务', SUGGEST_RETEST:'建议重新测评' }
function advisorStatusLabel(status?:string){ return advisorStatusLabels[status || ''] || status || '待评估' }
function advisorPathLabel(path?:string){ return advisorPathLabels[path || ''] || path || '待确定' }
function guidanceTypeLabel(type?:string){ return adviceTypeLabels[type as Guidance['adviceType']] || type || '指导意见' }
function formatDate(value?:string){
  if (!value) return '暂无记录'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('zh-CN',{year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hour12:false}).format(date).replace(/\//g,'-')
}
function objectOf(value:unknown):Record<string,unknown>{ return value && typeof value === 'object' ? value as Record<string,unknown> : {} }
function listOf<T = Record<string,unknown>>(value:unknown):T[]{ return Array.isArray(value) ? value as T[] : [] }
const advisorDetail = computed(() => objectOf(advisorStudent.value?.detail))
const advisorProfile = computed(() => objectOf(advisorDetail.value.profile))
const advisorPlan = computed(() => objectOf(advisorDetail.value.plan))
const advisorGoal = computed(() => objectOf(advisorDetail.value.goal))
const advisorPortrait = computed(() => objectOf(advisorDetail.value.portrait))
const advisorTasks = computed(() => listOf<Record<string,unknown>>(advisorDetail.value.tasks))
const advisorReviews = computed(() => listOf<Record<string,unknown>>(advisorDetail.value.reviews))
const advisorRecommendations = computed(() => listOf<Record<string,unknown>>(objectOf(advisorDetail.value.recommendation).results))
function valueOf(item:Record<string, unknown>, keys:string[]) { return keys.map(key => item[key]).find(value => value !== undefined && value !== null && value !== '') }
const fieldLabels: Record<string, string> = {
  studentNo:'学号', className:'班级', grade:'年级', majorCategory:'专业大类', role:'角色', status:'状态',
  lastLoginAt:'最近登录', createdAt:'创建时间', updatedAt:'更新时间', usedAt:'使用时间', used:'使用状态',
  advisorName:'辅导员', studentName:'学生', advisorId:'辅导员编号', studentId:'学生编号', directionId:'方向编号',
  directionName:'方向', category:'类别', path:'路径', version:'版本', publishedAt:'发布时间', operatorName:'操作人',
  action:'操作', filename:'文件名', fileName:'文件名', total:'总数', successCount:'成功数', failedCount:'失败数',
  startedAt:'开始时间', finishedAt:'完成时间', errorMessage:'错误信息', description:'说明', remark:'备注',
 }
function formatValue(key:string, value:unknown) {
  if (value === true) return '是'
  if (value === false) return '否'
  if (value === null || value === undefined || value === '') return ''
  if (typeof value === 'string' && /(At|Time|Date)$/.test(key)) {
    const date = new Date(value)
    if (!Number.isNaN(date.getTime())) return new Intl.DateTimeFormat('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit', hour12:false }).format(date).replace(/\//g, '-')
  }
  if (Array.isArray(value)) return value.join('、')
  if (typeof value === 'object') return '已配置'
  return String(value)
}
function toItem(item:unknown, index:number):Item {
  const row = (item && typeof item === 'object' ? item : {}) as Record<string, unknown>
  const id = String(valueOf(row, ['id','jobId','userId','relationId','tagId','directionId','templateId','version','name']) ?? index + 1)
  const title = String(valueOf(row, ['name','title','username','studentNo','directionName','abilityName','templateName','fileName','type']) ?? `记录 ${id}`)
  const status = valueOf(row, ['status','state','role','enabled','published'])
  const desc = Object.entries(row)
    .filter(([key, value]) => !['id','jobId','userId','relationId','tagId','directionId','templateId','name','title','username','status','state','role'].includes(key) && formatValue(key, value))
    .slice(0, 3)
    .map(([key,value]) => `${fieldLabels[key] || key}: ${formatValue(key, value)}`)
    .join(' · ')
  return { id, title, desc:desc || '后端返回的管理记录', status:status === undefined ? undefined : String(status) }
}
async function loadRemoteModule(){
  const loader = adminLoaders[active.value]
  if (!loader || !loggedIn.value) return
  remoteLoading.value = true; remoteError.value = ''
  try {
    const result = await loader()
    const data = result as {list?:unknown[]} | unknown[]
    const list = Array.isArray(data) ? data : Array.isArray(data?.list) ? data.list : data && typeof data === 'object' ? [data] : []
    remoteRecords.value[active.value] = list.map(toItem)
  } catch (e) { remoteError.value = getErrorMessage(e) } finally { remoteLoading.value = false }
}
function advisorQuery(page = advisorPage.value){
  const params = new URLSearchParams({ page:String(page), size:'20', sort:advisorFilters.value.sort || '-createdAt' })
  const filters = advisorFilters.value
  if (filters.keyword.trim()) params.set('keyword', filters.keyword.trim())
  if (filters.path) params.set('path', filters.path)
  if (filters.goalStatus) params.set('goalStatus', filters.goalStatus)
  if (filters.reviewStatus) params.set('reviewStatus', filters.reviewStatus)
  if (filters.guidanceRequested) params.set('guidanceRequested', filters.guidanceRequested)
  return params.toString()
}
async function loadAdvisorStudents(page = advisorPage.value){
  advisorLoading.value = true; advisorError.value = ''
  try {
    const data = await api.advisor.students(advisorQuery(page)) as {list?:AdvisorStudent[]; total?:number; page?:number; totalPages?:number}
    advisorStudents.value = data.list || []
    advisorTotal.value = data.total || 0
    advisorPage.value = data.page || page
    advisorTotalPages.value = Math.max(data.totalPages || 1, 1)
  } catch (e) { advisorError.value = getErrorMessage(e) } finally { advisorLoading.value = false }
}
async function loadAdvisorAttention(){
  advisorLoading.value = true; advisorError.value = ''
  try { advisorAttention.value = await api.advisor.attention() as AdvisorAttention[] } catch (e) { advisorError.value = getErrorMessage(e) } finally { advisorLoading.value = false }
}
async function loadAdvisorStatistics(){
  advisorLoading.value = true; advisorError.value = ''
  try { advisorStatistics.value = await api.advisor.statistics() as AdvisorStatistics } catch (e) { advisorError.value = getErrorMessage(e) } finally { advisorLoading.value = false }
}
async function loadAdvisorModule(){
  if (!loggedIn.value || role.value !== 'ADVISOR') return
  if (active.value === 'students' || active.value === 'guidance') await loadAdvisorStudents(active.value === 'guidance' ? 1 : advisorPage.value)
  else if (active.value === 'attention') await loadAdvisorAttention()
  else if (active.value === 'advisor-overview' || active.value === 'statistics') await loadAdvisorStatistics()
}
function applyAdvisorFilters(){ advisorPage.value = 1; loadAdvisorStudents(1) }
function resetAdvisorFilters(){ advisorFilters.value = { keyword:'', path:'', goalStatus:'', reviewStatus:'', guidanceRequested:'', sort:'-createdAt' }; applyAdvisorFilters() }
function changeAdvisorPage(next:number){ if (next >= 1 && next <= advisorTotalPages.value) loadAdvisorStudents(next) }
function openCreateForm(){ createValues.value = {}; showForm.value = true }
async function createRemote(){
  const config = createConfig.value
  if (!config) return
  const data:Record<string, unknown> = {...createValues.value}
  if (active.value === 'relations') data.studentIds = String(data.studentIds || '').split(',').map(value => value.trim()).filter(Boolean)
  const actions:Record<string,(payload:unknown)=>Promise<unknown>> = { whitelist:api.admin.createWhitelist, relations:api.admin.createRelations, 'admin-directions':api.admin.createDirection, abilities:api.admin.createAbility, templates:api.admin.createTemplate, exports:api.admin.createExport }
  creating.value = true
  try { await actions[active.value](data); showForm.value=false; act(`${config.title}成功`); await loadRemoteModule() } catch (e) { act(getErrorMessage(e)) } finally { creating.value=false }
}
async function deleteRemote(id:string){
  if (!confirm('确定删除这条记录吗？此操作不可撤销。')) return
  try { const actions:Record<string,(target:string)=>Promise<unknown>>={whitelist:api.admin.deleteWhitelist,relations:api.admin.deleteRelation}; await actions[active.value](id); act('记录已删除'); await loadRemoteModule() } catch(e){act(getErrorMessage(e))}
}
function menuLabel(key:string){ for(const g of menus[role.value]) for(const [id,name] of g.links) if(id===key)return name; return key }
async function loadStudentData(){
  if (role.value !== 'STUDENT') return
  const [profileResult, completenessResult, experienceResult, consent] = await Promise.all([api.student.me(), api.student.completeness(), api.student.experiences(), api.auth.consentStatus()])
  profile.value = profileResult; completeness.value = completenessResult; experiences.value = experienceResult
  consentStatus.value = consent; consentAgreed.value = consent.agreed
}
async function login(){
  error.value = ''; loading.value = true
  try {
    if (authMode.value === 'reset') throw new Error('当前后端仅提供管理员重置密码接口，暂不支持自助重置。')
    const result = authMode.value === 'login'
      ? await api.auth.login({ account:account.value, password:password.value })
      : await api.auth.register({ studentNo:studentNo.value, name:name.value, className:className.value || undefined, verifyCode:verifyCode.value })
    setAuthTokens(result); currentUser.value = result.user; role.value = result.user.role
    active.value = role.value === 'STUDENT' ? 'overview' : role.value === 'ADVISOR' ? 'advisor-overview' : 'admin-overview'
    loggedIn.value = true
    await loadStudentData(); await loadRemoteModule(); await loadAdvisorModule()
  } catch (e) { error.value = getErrorMessage(e) } finally { loading.value = false }
}
function act(label='操作已保存'){ toast.value=label; showForm.value=false; setTimeout(()=>toast.value='',2200) }
async function createItem(){
  if (active.value === 'experiences') {
    creating.value = true
    try {
      const payload = { type:experienceType.value, title:experienceTitle.value, startDate:experienceStart.value, endDate:experienceEnd.value || undefined, description:experienceDescription.value || undefined, attachment:experienceAttachment.value || undefined }
      const item = editingExperienceId.value ? await api.student.updateExperience(editingExperienceId.value, payload) : await api.student.addExperience(payload)
      const index = experiences.value.findIndex(value => value.id === item.id)
      index >= 0 ? experiences.value.splice(index, 1, item) : experiences.value.unshift(item)
      editingExperienceId.value=null; experienceTitle.value=''; experienceStart.value=''; experienceEnd.value=''; experienceDescription.value=''; experienceAttachment.value=''; act('经历已保存')
    } catch (e) { act(getErrorMessage(e)) } finally { creating.value = false }
    return
  }
  const list=records[active.value] || tasks.value; list.unshift({id:'NEW-'+Date.now(),title:'新建'+menuLabel(active.value),desc:'已创建为草稿，可继续补充信息。',status:'草稿'}); act('已创建草稿')
}
function openExperienceEditor(item:Experience){ editingExperienceId.value=item.id; experienceType.value=item.type; experienceTitle.value=item.title; experienceStart.value=item.startDate; experienceEnd.value=item.endDate || ''; experienceDescription.value=item.description || ''; experienceAttachment.value=item.attachmentUrl || ''; showForm.value=true }
function openProfileEditor(){
  const source=profile.value
  profileForm.value={ gender:source?.basic?.gender || '',hometown:source?.basic?.hometown || '',birthday:source?.basic?.birthday || '',phone:source?.basic?.phone || '',math:String(source?.academic?.math || ''),english:String(source?.academic?.english || ''),programming:String(source?.academic?.programming || ''),academicNote:source?.academic?.note || '',abilityProgramming:String(source?.abilitySelf?.programming || ''),abilityMath:String(source?.abilitySelf?.math || ''),abilityEnglish:String(source?.abilitySelf?.english || ''),communication:String(source?.abilitySelf?.communication || ''),organization:String(source?.abilitySelf?.organization || ''),interests:(source?.interestPrefs || []).join('、'),values:(source?.values || []).join('、'),developmentIntention:source?.developmentIntention || 'undecided',constraints:(source?.constraints || []).join('、') }
  profileEditor.value=true
}
const toNumber=(value:string)=>value.trim()?Number(value):undefined
const splitTags=(value:string)=>value.split(/[、,，]/).map(item=>item.trim()).filter(Boolean)
async function saveProfile(){
  profileSaving.value=true
  try { const form=profileForm.value; profile.value=await api.student.update({basic:{gender:form.gender||undefined,hometown:form.hometown||undefined,birthday:form.birthday||undefined,phone:form.phone||undefined},academic:{math:toNumber(form.math),english:toNumber(form.english),programming:toNumber(form.programming),note:form.academicNote||undefined},abilitySelf:{programming:toNumber(form.abilityProgramming),math:toNumber(form.abilityMath),english:toNumber(form.abilityEnglish),communication:toNumber(form.communication),organization:toNumber(form.organization)},interestPrefs:splitTags(form.interests),values:splitTags(form.values),developmentIntention:form.developmentIntention,constraints:splitTags(form.constraints)}); completeness.value=await api.student.completeness(); profileEditor.value=false; act('个人资料已保存') } catch(e){ act(getErrorMessage(e)) } finally {profileSaving.value=false}
}
async function submitConsent(){
  if (!consentStatus.value?.currentVersion) { act('暂未获取到当前隐私授权文本，请刷新后重试'); return }
  consentSubmitting.value = true
  try {
    consentStatus.value = await api.auth.consent({version:consentStatus.value.currentVersion})
    consentAgreed.value = consentStatus.value.agreed
    if (currentUser.value) currentUser.value.consentAgreed = true
    act('隐私授权已保存')
  } catch(e) { act(getErrorMessage(e)) } finally { consentSubmitting.value = false }
}
async function submitPrivacyAction(){ privacySubmitting.value=true; try { if(privacyAction.value==='password') await api.auth.changePassword({oldPassword:oldPassword.value,newPassword:newPassword.value}); else if(privacyAction.value==='delete') await api.student.requestDeletion(deletionReason.value); privacyAction.value=null; oldPassword.value='';newPassword.value='';deletionReason.value='';act('申请已提交') } catch(e){act(getErrorMessage(e))} finally {privacySubmitting.value=false} }
async function openAdvisorStudent(id:string){
  guidanceContent.value=''; guidanceType.value='COMMENT'; guidanceSuggestedTask.value=''; guidanceRetestReason.value=''
  try {
    const [detail,guidance] = await Promise.all([api.advisor.detail(id),api.advisor.guidance(id)])
    advisorStudent.value = { id, detail, guidance:guidance as Guidance[] }
  } catch(e) { act(getErrorMessage(e)) }
}
async function submitGuidance(){
  if (!advisorStudent.value) return
  if (guidanceType.value === 'SUGGEST_TASK' && !guidanceSuggestedTask.value.trim()) { act('请填写建议任务'); return }
  if (guidanceType.value === 'SUGGEST_RETEST' && !guidanceRetestReason.value.trim()) { act('请填写建议重新测评的原因'); return }
  guidanceSubmitting.value = true
  try {
    const data = { content:guidanceContent.value.trim(), adviceType:guidanceType.value, suggestedTask:guidanceSuggestedTask.value.trim() || undefined, retestReason:guidanceRetestReason.value.trim() || undefined }
    const result = guidanceType.value === 'COMMENT'
      ? await api.advisor.writeGuidance(advisorStudent.value.id, data)
      : await api.advisor.writeAdvice(advisorStudent.value.id, data)
    advisorStudent.value.guidance.push(result as Guidance)
    guidanceContent.value=''; guidanceSuggestedTask.value=''; guidanceRetestReason.value=''
    act('指导已发送并保存')
  } catch(e) { act(getErrorMessage(e)) } finally { guidanceSubmitting.value=false }
}
function checkin(t:Item){ t.status=t.status==='已完成'?'进行中':'已完成'; act('任务状态已更新，并已生成打卡记录') }
function switchRole(next:Role){ role.value=next; active.value=next==='STUDENT'?'overview':next==='ADVISOR'?'advisor-overview':'admin-overview' }
async function logout(){
  try { await api.auth.logout() } catch { /* local token must still be cleared */ }
  clearAuthSession(); loggedIn.value=false; currentUser.value=null; profile.value=null; completeness.value=null; experiences.value=[]; consentStatus.value=null; consentAgreed.value=null
  authMode.value='login'; keyword.value=''; showForm.value=false; toast.value=''
}
async function deleteExperience(id:string){
  try { await api.student.deleteExperience(id); experiences.value = experiences.value.filter(item => item.id !== id); act('经历已删除') } catch (e) { act(getErrorMessage(e)) }
}
onMounted(async () => {
  if (!hasAccessToken() && !hasRefreshToken()) return
  loading.value = true
  try {
    const user = await api.auth.me()
    currentUser.value = user; role.value = user.role
    active.value = user.role === 'STUDENT' ? 'overview' : user.role === 'ADVISOR' ? 'advisor-overview' : 'admin-overview'
    loggedIn.value = true
    await loadStudentData(); await loadRemoteModule(); await loadAdvisorModule()
  } catch {
    clearAuthSession()
  } finally {
    loading.value = false
  }
})
watch(active, () => { remoteSearch.value = ''; loadRemoteModule(); loadAdvisorModule() })
const radar = computed(()=>scores.map((d,i)=>{const a=-Math.PI/2+i*Math.PI/3,r=95*d.value/100;return `${150+Math.cos(a)*r},${150+Math.sin(a)*r}`}).join(' '))
</script>

<template>
  <main v-if="!loggedIn" class="auth-shell"><header><span>CAREER ARCHIVE / 01</span><span>生涯规划系统</span></header><section class="auth-grid"><div class="auth-intro"><p class="eyebrow">CAREER PLANNING SYSTEM</p><h1><span>把每一次探索，</span><em>留下证据。</em></h1><p>账号所属角色由系统自动识别并进入对应工作台。</p><div class="protocol"><span>PROFILE</span><span>ASSESSMENT</span><span>PLAN</span><span>REVIEW</span></div></div><form class="auth-card" @submit.prevent="login"><p class="eyebrow">{{ authMode.toUpperCase() }} / ACCESS</p><h2>{{ authMode==='login'?'进入工作台':authMode==='register'?'白名单注册':'重置密码' }}</h2><label v-if="authMode==='login'">账号<input v-model.trim="account" required placeholder="账号或学号" /></label><label v-if="authMode==='register'">学号<input v-model.trim="studentNo" required placeholder="例如：2026011301" /></label><label v-if="authMode==='register'">姓名<input v-model.trim="name" required placeholder="请输入姓名" /></label><label v-if="authMode==='register'">班级（可选）<input v-model.trim="className" placeholder="例如：软件 2601" /></label><label v-if="authMode==='register'">白名单校验码<input v-model.trim="verifyCode" required placeholder="输入校验码" /></label><label v-if="authMode==='login'">密码<input v-model="password" required type="password" placeholder="输入密码" /></label><p v-if="error" class="form-error">{{ error }}</p><button class="primary-btn" type="submit" :disabled="loading">{{ loading?'正在验证…':authMode==='login'?'登录系统 →':authMode==='register'?'提交注册 →':'查看说明' }}</button><div class="auth-actions"><button type="button" @click="authMode='login';error=''">登录</button><button type="button" @click="authMode='register';error=''">注册</button><button type="button" @click="authMode='reset';error=''">忘记密码</button></div></form></section></main>

  <div v-else class="app-shell"><aside class="side-nav"><div class="brand"><span class="brand-mark">C</span><span>CAREER / ARCHIVE</span></div><div class="user-card"><div class="avatar">{{ (currentUser?.name || roleNames[role]).slice(0,1) }}</div><div><b>{{ currentUser?.name || currentUser?.username }}</b><small>{{ roleNames[role] }} · 在线</small></div></div><nav v-for="group in menus[role]" :key="group.group"><p class="nav-group">{{ group.group }}</p><button v-for="[id,label] in group.links" :key="id" :class="{active:active===id}" @click="active=id"><i>◼</i>{{ label }}</button></nav><div class="side-foot">当前登录身份由后端 JWT 决定<br/><br/>智能生成，仅供探索参考</div></aside>
    <main class="main-content"><header class="topbar"><span>{{ roleNames[role] }} / 2026-2027 · FIRST TERM</span><div><button class="logout-btn" @click="logout">↙ 退出登录</button><button class="outline-btn" @click="showForm=true">+ 新建</button><button class="notice" @click="act('通知已标记为已读')">●</button></div></header>
      <section class="page fade-in"><div class="page-title"><div><p class="eyebrow">{{ currentTitle[0] }}</p><h1>{{ currentTitle[1] }}</h1><p>{{ currentTitle[2] }}</p></div><button v-if="active==='experiences'" class="primary-btn" @click="showForm=true">新增经历 <span>→</span></button></div>
        <template v-if="active==='overview'"><div class="journey-panel"><div class="journey-head"><div><span class="soft-pill">PROFILE / LIVE</span><h2>{{ currentUser?.name || '我的生涯档案' }}</h2></div><b>{{ completeness?.score ?? profile?.completeness ?? '—' }}<small>%</small></b></div><div class="progress-line"><span :style="{width:(completeness?.score ?? profile?.completeness ?? 0)+'%'}"></span></div><div class="journey-steps"><div v-for="x in (completeness?.dimensions ?? [])" :key="x.key" class="journey-step" :class="{done:x.filled}"><span class="step-dot">{{ x.filled?'✓':'·' }}</span><div><b>{{ x.name }}</b><small>{{ x.filled?'已完善':'待补充' }}</small></div></div></div></div><div class="dashboard-grid"><section class="card"><p class="eyebrow">PROFILE / NEXT</p><h2>{{ completeness?.missing?.length ? '下一步：补充档案' : '档案信息已完善' }}</h2><div class="focus-task"><p>{{ completeness?.missing?.length ? `仍缺少：${completeness.missing.map(x=>x.name).join('、')}` : '可继续维护个人经历，后续测评、推荐和计划服务上线后会使用这些信息。' }}</p><button class="primary-btn" @click="active='profile'">维护个人资料 →</button></div></section><section class="card data-card"><p class="eyebrow">PROFILE / COMPLETENESS</p><b class="big-number">{{ completeness?.filled ?? 0 }}</b><span>/ {{ completeness?.total ?? 0 }} 已填写字段</span></section></div></template>
<template v-else-if="active==='profile'"><section class="card"><div class="section-head"><div><p class="eyebrow">PROFILE / LIVE DATA</p><h2>个人资料</h2></div><button class="primary-btn compact-btn" @click="openProfileEditor">编辑资料 <span>→</span></button></div><div class="record-list"><article v-for="x in [{t:'姓名',v:profile?.name},{t:'学号',v:currentUser?.studentNo},{t:'班级',v:profile?.className},{t:'年级',v:profile?.grade},{t:'专业大类',v:profile?.majorCategory},{t:'兴趣偏好',v:profile?.interestPrefs?.join('、')},{t:'发展意向',v:profile?.developmentIntention}]" :key="x.t"><small>PROFILE</small><div><b>{{ x.t }}</b><span>{{ x.v || '尚未填写' }}</span></div></article></div></section><div v-if="profileEditor" class="modal-mask"><form class="modal-card profile-editor" @submit.prevent="saveProfile"><p class="eyebrow">PROFILE / EDIT</p><h2>编辑个人资料</h2><div class="form-grid"><label>性别<input v-model.trim="profileForm.gender"/></label><label>籍贯<input v-model.trim="profileForm.hometown"/></label><label>出生日期<input v-model="profileForm.birthday" placeholder="YYYY-MM-DD"/></label><label>手机号<input v-model.trim="profileForm.phone"/></label><label>数学基础（1-5）<input v-model="profileForm.math" type="number" min="1" max="5"/></label><label>英语基础（1-5）<input v-model="profileForm.english" type="number" min="1" max="5"/></label><label>编程基础（1-5）<input v-model="profileForm.programming" type="number" min="1" max="5"/></label><label>编程能力自评（1-5）<input v-model="profileForm.abilityProgramming" type="number" min="1" max="5"/></label><label>数学能力自评（1-5）<input v-model="profileForm.abilityMath" type="number" min="1" max="5"/></label><label>英语能力自评（1-5）<input v-model="profileForm.abilityEnglish" type="number" min="1" max="5"/></label><label>沟通能力（1-5）<input v-model="profileForm.communication" type="number" min="1" max="5"/></label><label>组织能力（1-5）<input v-model="profileForm.organization" type="number" min="1" max="5"/></label><label>发展意向<select v-model="profileForm.developmentIntention"><option value="undecided">待确定</option><option value="employment">就业</option><option value="graduate">升学</option><option value="overseas">出国</option></select></label><label>兴趣偏好<textarea v-model.trim="profileForm.interests" placeholder="用顿号或逗号分隔"/></label><label>价值取向<textarea v-model.trim="profileForm.values" placeholder="用顿号或逗号分隔"/></label><label>学业备注<textarea v-model.trim="profileForm.academicNote"/></label><label>现实约束<textarea v-model.trim="profileForm.constraints" placeholder="用顿号或逗号分隔"/></label></div><div><button type="button" class="outline-btn" @click="profileEditor=false">取消</button><button class="primary-btn" :disabled="profileSaving">{{ profileSaving?'正在保存…':'保存资料 →' }}</button></div></form></div></template>
        <template v-else-if="active==='experiences'"><section class="card"><div class="section-head"><div><p class="eyebrow">EXPERIENCE / LIVE DATA</p><h2>经历管理</h2></div><button class="primary-btn" @click="editingExperienceId=null;showForm=true">新增经历 →</button></div><div class="record-list"><article v-for="x in experiences" :key="x.id"><small>{{ x.type }}</small><div><b>{{ x.title }}</b><span>{{ x.startDate }}{{ x.endDate ? ` 至 ${x.endDate}` : '' }} · {{ x.description || '未填写说明' }}</span></div><div class="row-actions"><button class="outline-btn" @click="openExperienceEditor(x)">编辑</button><button class="outline-btn" @click="deleteExperience(x.id)">删除</button></div></article><p v-if="!experiences.length" class="empty">暂未记录经历。</p></div></section></template>
        <template v-else-if="active==='advisor-overview' || active==='statistics'"><section class="advisor-statistics"><div class="section-head"><div><p class="eyebrow">ADVISOR / LIVE DATA</p><h2>{{ active==='statistics' ? '群体统计' : '工作总览' }}</h2></div><button class="outline-btn" @click="loadAdvisorStatistics">刷新</button></div><p v-if="advisorLoading" class="empty">正在读取统计数据…</p><p v-else-if="advisorError" class="empty error-state">{{ advisorError }}</p><template v-else><div class="metric-grid advisor-metrics"><article><b>{{ advisorStatistics?.totalStudents ?? 0 }}</b><span>所带学生</span></article><article><b>{{ advisorStatistics?.assessedCount ?? 0 }}</b><span>已完成测评</span></article><article><b>{{ advisorStatistics?.planMadeCount ?? 0 }}</b><span>已制定计划</span></article><article><b>{{ advisorStatistics?.reviewedCount ?? 0 }}</b><span>本月已复盘</span></article></div><div class="advisor-overview-grid"><section class="advisor-subcard"><p class="eyebrow">TASK / COMPLETION</p><b class="advisor-rate">{{ advisorStatistics?.taskCompletionRate ?? '—' }}<small v-if="advisorStatistics?.taskCompletionRate !== undefined">%</small></b><p>所带学生的平均任务完成率，仅统计存在任务的已确认计划。</p><button class="outline-btn" @click="active='students'">查看学生列表</button></section><section class="advisor-subcard"><p class="eyebrow">PATH / DISTRIBUTION</p><div class="path-bars"><div v-for="item in (advisorStatistics?.pathDistribution || [])" :key="item.path"><div><span>{{ advisorPathLabel(item.path) }}</span><b>{{ item.count }}</b></div><i><em :style="{width: `${(advisorStatistics?.totalStudents ? item.count / advisorStatistics.totalStudents * 100 : 0)}%`}"></em></i></div><p v-if="!(advisorStatistics?.pathDistribution || []).length" class="empty">暂无路径分布数据。</p></div></section></div></template></section></template>
        <template v-else-if="active==='students'"><section class="card data-list-card advisor-list-card"><div class="section-head"><div><p class="eyebrow">ADVISOR / STUDENTS</p><h2>学生列表</h2></div><button class="outline-btn" @click="loadAdvisorStudents()">刷新</button></div><form class="advisor-filters" @submit.prevent="applyAdvisorFilters"><label>关键词<input v-model.trim="advisorFilters.keyword" placeholder="姓名、班级或目标"/></label><label>发展路径<select v-model="advisorFilters.path"><option value="">全部路径</option><option value="graduate">国内升学</option><option value="employment">就业发展</option><option value="overseas">出国留学</option></select></label><label>目标状态<select v-model="advisorFilters.goalStatus"><option value="">全部目标</option><option value="HAS_GOAL">已设主目标</option><option value="NO_GOAL">未设主目标</option></select></label><label>复盘状态<select v-model="advisorFilters.reviewStatus"><option value="">全部复盘</option><option value="REVIEWED_THIS_MONTH">本月已复盘</option><option value="LONG_NO_REVIEW">长期未复盘</option></select></label><label>指导申请<select v-model="advisorFilters.guidanceRequested"><option value="">全部</option><option value="true">已申请指导</option><option value="false">未申请指导</option></select></label><label>排序<select v-model="advisorFilters.sort"><option value="-createdAt">最近更新</option><option value="name">姓名</option><option value="-completeness">档案完整度</option><option value="-planRate">计划完成率</option><option value="-lastReview">最近复盘</option></select></label><div class="advisor-filter-actions"><button class="primary-btn compact-btn" type="submit">应用筛选</button><button class="outline-btn" type="button" @click="resetAdvisorFilters">重置</button></div></form><div class="list-summary"><span>共 <b>{{ advisorTotal }}</b> 名学生</span><span>第 {{ advisorPage }} / {{ advisorTotalPages }} 页</span></div><p v-if="advisorLoading" class="empty">正在读取学生数据…</p><p v-else-if="advisorError" class="empty error-state">{{ advisorError }}</p><div v-else class="advisor-student-table"><div class="advisor-student-columns"><span>学生</span><span>发展状态</span><span>进度</span><span>最近复盘</span><span>操作</span></div><article v-for="student in advisorStudents" :key="student.id"><div><b>{{ student.name }}</b><small>{{ student.className || '未填写班级' }} · {{ student.id }}</small></div><div><span>{{ advisorPathLabel(student.path) }}</span><small>{{ student.primaryGoal || '尚未设定主目标' }}{{ student.direction ? ` · ${student.direction}` : '' }}</small></div><div><span>档案 {{ student.completeness ?? 0 }}% · 计划 {{ student.planRate ?? 0 }}%</span><small>{{ student.assessed ? '已完成测评' : '未完成测评' }}</small></div><div><em :class="statusClass(student.status)">{{ advisorStatusLabel(student.status) }}</em><small>{{ student.lastReview ? formatDate(student.lastReview) : '暂无复盘' }}</small></div><button class="outline-btn" @click="openAdvisorStudent(student.id)">详情与指导</button></article><p v-if="!advisorStudents.length" class="empty">当前筛选条件下没有学生记录。</p></div><div v-if="advisorTotalPages>1" class="advisor-pagination"><button class="outline-btn" :disabled="advisorPage<=1" @click="changeAdvisorPage(advisorPage-1)">上一页</button><span>{{ advisorPage }} / {{ advisorTotalPages }}</span><button class="outline-btn" :disabled="advisorPage>=advisorTotalPages" @click="changeAdvisorPage(advisorPage+1)">下一页</button></div></section></template>
        <template v-else-if="active==='attention'"><section class="card data-list-card"><div class="section-head"><div><p class="eyebrow">ADVISOR / ATTENTION</p><h2>重点关注</h2></div><button class="outline-btn" @click="loadAdvisorAttention">刷新</button></div><p v-if="advisorLoading" class="empty">正在读取关注学生…</p><p v-else-if="advisorError" class="empty error-state">{{ advisorError }}</p><div v-else class="attention-list"><article v-for="item in advisorAttention" :key="item.student.id"><div><p class="eyebrow">{{ item.student.id }}</p><h3>{{ item.student.name }}</h3><p>{{ item.student.className || '未填写班级' }} · {{ advisorPathLabel(item.student.path) }}</p></div><div class="attention-reasons"><span v-for="reason in item.reasons" :key="reason">{{ reason }}</span></div><button class="outline-btn" @click="openAdvisorStudent(item.student.id)">查看并指导</button></article><p v-if="!advisorAttention.length" class="empty">当前没有需要重点关注的学生。</p></div></section></template>
        <template v-else-if="active==='guidance'"><section class="card data-list-card"><div class="section-head"><div><p class="eyebrow">ADVISOR / GUIDANCE</p><h2>指导记录</h2></div><button class="outline-btn" @click="loadAdvisorStudents(1)">刷新学生列表</button></div><div class="guidance-hint"><b>按学生查看记录</b><span>当前后端的指导记录接口需要指定学生；选择学生后可查看完整历史，并发送指导意见、建议任务或建议重新测评。</span></div><p v-if="advisorLoading" class="empty">正在读取学生数据…</p><p v-else-if="advisorError" class="empty error-state">{{ advisorError }}</p><div v-else class="record-list enhanced-list"><div class="list-columns"><span>编号</span><span>学生</span><span>状态</span></div><article v-for="student in advisorStudents" :key="student.id"><small>{{ student.id }}</small><div><b>{{ student.name }}</b><span>{{ student.className || '未填写班级' }} · {{ student.primaryGoal || '尚未设定主目标' }}</span></div><em :class="statusClass(student.status)">{{ advisorStatusLabel(student.status) }}</em><button class="outline-btn" @click="openAdvisorStudent(student.id)">查看记录</button></article><p v-if="!advisorStudents.length" class="empty">暂无所带学生。</p></div></section></template>
        <template v-else-if="hasRemoteModule"><section class="card data-list-card"><div class="section-head"><div><p class="eyebrow">{{ role==='ADMIN'?'ADMIN':'ADVISOR' }} / LIVE DATA</p><h2>{{ menuLabel(active) }}</h2></div><div class="list-actions"><button v-if="createConfig" class="primary-btn compact-btn" @click="openCreateForm">新建 <span>→</span></button><button class="outline-btn" @click="loadRemoteModule">刷新</button></div></div><div class="list-summary"><span>共 <b>{{ remoteRecords[active]?.length || 0 }}</b> 条记录</span><label><span class="sr-only">搜索</span><input v-model="remoteSearch" placeholder="搜索名称、编号或状态"/></label></div><p v-if="remoteLoading" class="empty">正在读取后端数据…</p><p v-else-if="remoteError" class="empty error-state">{{ remoteError }}</p><div v-else class="record-list enhanced-list"><div class="list-columns"><span>编号</span><span>内容</span><span>状态</span></div><article v-for="x in visibleRemoteRecords" :key="x.id"><small>{{ x.id }}</small><div><b>{{ x.title }}</b><span>{{ x.desc }}</span></div><em v-if="x.status" :class="statusClass(x.status)">{{ x.status }}</em><em v-else class="neutral">—</em></article><p v-if="!visibleRemoteRecords.length" class="empty">{{ remoteSearch ? '没有符合搜索条件的记录。' : '暂无记录。' }}</p></div></section></template>
        <template v-else-if="unsupportedPages.includes(active)"><section class="card"><p class="eyebrow">SERVICE / PENDING</p><h2>此功能尚未接入当前后端</h2><p>已核对全部远程分支：认证、学生档案、经历和辅导员/管理模块分别存在于后端分支；画像、推荐、计划与 AI 服务尚未与带 JWT 的业务后端合并。前端不会绕过服务端直接调用需要 <code>X-Internal-Token</code> 的 AI 接口。</p></section></template>
        <template v-else-if="active==='portrait'"><div class="profile-layout"><section class="card radar-card"><div class="section-head"><div><p class="eyebrow">SIX DIMENSIONS</p><h2>个人能力画像</h2></div><span class="data-note">MOCK DATA</span></div><div class="radar-wrap"><svg viewBox="0 0 300 300"><g v-for="l in [25,50,75,100]" :key="l"><polygon :points="Array.from({length:6},(_,i)=>{const a=-Math.PI/2+i*Math.PI/3,r=95*l/100;return `${150+Math.cos(a)*r},${150+Math.sin(a)*r}`}).join(' ')" fill="none" stroke="#c9c9bf"/></g><line v-for="i in 6" :key="i" x1="150" y1="150" :x2="150+Math.cos(-Math.PI/2+i*Math.PI/3)*95" :y2="150+Math.sin(-Math.PI/2+i*Math.PI/3)*95" stroke="#c9c9bf"/><polygon :points="radar" fill="rgba(199,255,56,.55)" stroke="#171914" stroke-width="2"/><text v-for="(d,i) in scores" :key="d.label" :x="150+Math.cos(-Math.PI/2+i*Math.PI/3)*122" :y="154+Math.sin(-Math.PI/2+i*Math.PI/3)*122" text-anchor="middle">{{ d.label }}</text></svg><div class="dimension-list"><div v-for="d in scores" :key="d.label"><span>{{d.label}}</span><i><em :style="{width:d.value+'%'}"></em></i><b>{{d.value}}</b></div></div></div></section><aside class="insight-card"><p class="eyebrow">PROFILE / INSIGHT</p><h2>一份正在成形的优势</h2><p>你对需要深度思考的问题有耐心，也愿意主动上手尝试。下一步应把兴趣沉淀为作品。</p><hr/><button class="outline-btn" @click="act('已提交画像反馈')">提交反馈</button></aside></div></template>
        <template v-else-if="active==='tasks'"><section class="task-board"><div v-for="state in ['未开始','进行中','已完成']" :key="state"><p>{{ state.toUpperCase() }}</p><article v-for="t in tasks.filter(x=>x.status===state)" :key="t.id"><small>{{t.id}}</small><b>{{t.title}}</b><span>{{t.desc}}</span><footer><em>{{t.meta}}</em><button @click="checkin(t)">{{state==='已完成'?'撤销':'打卡'}}</button></footer></article></div></section></template>
        <template v-else-if="active==='chat'"><section class="chat-panel"><div class="chat-log"><p class="system-line">SYSTEM / AI 建议不构成结论性判断</p><div class="bubble"><b>生涯助手</b> 你想先讨论方向、计划，还是最近遇到的困难？</div><div class="bubble user">我想比较算法与数据分析方向。</div><div class="bubble"><b>生涯助手</b> 可以。你在两项方向上的兴趣与能力差距较小，建议完成一个数据分析项目并访谈一位相关高年级同学，再回来看证据。</div></div><div class="chat-input"><input placeholder="输入你的问题…"/><button @click="act('已发送，AI 回复已生成')">发送</button></div></section></template>
        <template v-else-if="active==='privacy'"><section class="settings-list"><article><div><b>隐私授权</b><span>{{ consentAgreed ? `已同意 ${consentStatus?.version || ''} 生涯规划数据处理授权` : `请阅读并同意当前 ${consentStatus?.currentVersion || '—'} 版本授权文本` }}</span><small v-if="consentStatus?.content">{{ consentStatus.content }}</small></div><button class="outline-btn" :disabled="consentAgreed===true || consentSubmitting || !consentStatus?.currentVersion" @click="submitConsent">{{ consentAgreed ? '已授权' : consentSubmitting ? '正在保存…' : '同意当前授权' }}</button></article><article><div><b>修改密码</b><span>定期更换密码，保护账户安全。</span></div><button class="outline-btn" @click="privacyAction='password'">修改密码</button></article><article><div><b>删除申请</b><span>提交后将进入人工审核流程。</span></div><button class="outline-btn" @click="privacyAction='delete'">提交申请</button></article></section><div v-if="privacyAction" class="modal-mask"><form class="modal-card" @submit.prevent="submitPrivacyAction"><p class="eyebrow">ACCOUNT / SECURITY</p><h2>{{ privacyAction==='password'?'修改密码':'提交删除申请' }}</h2><template v-if="privacyAction==='password'"><label>当前密码<input v-model="oldPassword" required type="password"/></label><label>新密码<input v-model="newPassword" required type="password" minlength="6" maxlength="128"/></label></template><label v-else>申请原因（可选）<textarea v-model.trim="deletionReason" maxlength="255" placeholder="说明删除原因"></textarea></label><div><button type="button" class="outline-btn" @click="privacyAction=null">取消</button><button class="primary-btn" :disabled="privacySubmitting">{{privacySubmitting?'正在提交…':'确认提交 →'}}</button></div></form></div></template>
        <template v-else-if="active==='advisor-overview' || active==='admin-overview'"><section class="card"><div class="section-head"><div><p class="eyebrow">{{ role==='ADVISOR'?'ADVISOR / LIVE':'ADMIN / LIVE' }}</p><h2>{{ role==='ADVISOR'?'工作总览':'运行总览' }}</h2></div><button class="outline-btn" @click="loadRemoteModule">刷新</button></div><p v-if="remoteLoading" class="empty">正在读取后端数据…</p><p v-else-if="remoteError" class="empty">{{ remoteError }}</p><div v-else class="record-list"><article v-for="x in (remoteRecords[active] || [])" :key="x.id"><small>{{ x.id }}</small><div><b>{{ x.title }}</b><span>{{ x.desc }}</span></div><em v-if="x.status">{{ x.status }}</em></article><p v-if="!(remoteRecords[active] || []).length" class="empty">暂无后端记录。</p></div></section></template>
        <template v-else><section class="card"><div class="table-tools"><input v-model="keyword" placeholder="搜索名称、编号或状态"/><button class="outline-btn" @click="act('筛选已应用')">筛选</button></div><div class="record-list"><article v-for="x in (records[active]||[]).filter(x=>!keyword || (x.title+x.desc+x.id).includes(keyword))" :key="x.id"><small>{{x.id}}</small><div><b>{{x.title}}</b><span>{{x.desc}}</span></div><em>{{x.status}}</em><button class="outline-btn" @click="act('已保存 '+x.title+' 的变更')">查看 / 编辑</button></article><p v-if="!(records[active]||[]).length" class="empty">该模块的页面骨架已建立，等待接口数据接入。</p></div></section></template>
      </section></main><div v-if="toast" class="toast">✓ {{toast}}</div><div v-if="showForm" class="modal-mask"><form class="modal-card" @submit.prevent="active==='experiences' ? createItem() : createRemote()"><p class="eyebrow">CREATE / DRAFT</p><h2>{{ active==='experiences' ? (editingExperienceId ? '编辑经历' : '新建经历') : createConfig?.title }}</h2><template v-if="active==='experiences'"><label>经历类别<select v-model="experienceType"><option value="project">项目</option><option value="internship">实习</option><option value="competition">竞赛</option><option value="club">社团</option></select></label><label>经历名称<input v-model.trim="experienceTitle" required maxlength="100" placeholder="例如：校园数据分析训练营"/></label><label>开始时间<input v-model="experienceStart" required pattern="\d{4}-\d{2}" placeholder="YYYY-MM"/></label><label>结束时间（可选）<input v-model="experienceEnd" pattern="\d{4}-\d{2}" placeholder="YYYY-MM"/></label><label>说明<textarea v-model.trim="experienceDescription" maxlength="2000" placeholder="你负责的内容、成果或收获"></textarea></label><label>附件地址（可选）<input v-model.trim="experienceAttachment" maxlength="255" placeholder="临时附件 ID 或可访问地址"/></label></template><template v-else-if="createConfig"><label v-for="field in createConfig.fields" :key="field.key">{{ field.label }}<textarea v-if="field.type==='textarea'" v-model.trim="createValues[field.key]" :required="field.required" :placeholder="field.placeholder"/><input v-else v-model.trim="createValues[field.key]" :required="field.required" :placeholder="field.placeholder"/></label></template><div><button type="button" class="outline-btn" @click="showForm=false">取消</button><button class="primary-btn" :disabled="creating">{{ creating?'正在保存…':'保存 →' }}</button></div></form></div></div>
<div v-if="advisorStudent" class="modal-mask"><section class="modal-card advisor-detail-modal"><header><div><p class="eyebrow">ADVISOR / STUDENT DETAIL</p><h2>{{ String(advisorProfile.name || '学生详情') }}</h2><p>{{ String(advisorProfile.className || '未填写班级') }} · {{ advisorStudent.id }}</p></div><button class="outline-btn" @click="advisorStudent=null">关闭</button></header><div class="advisor-detail-scroll"><section class="advisor-detail-section"><h3>档案概览</h3><div class="detail-kv"><div><small>档案完整度</small><b>{{ advisorProfile.completeness ?? '—' }}<i v-if="advisorProfile.completeness !== undefined">%</i></b></div><div><small>发展意向</small><b>{{ advisorPathLabel(String(advisorProfile.developmentIntention || '')) }}</b></div><div><small>专业大类</small><b>{{ String(advisorProfile.majorCategory || '未填写') }}</b></div><div><small>兴趣偏好</small><b>{{ listOf<string>(advisorProfile.interestPrefs).join('、') || '未填写' }}</b></div></div></section><section v-if="advisorPortrait.summary || advisorPortrait.completeness !== undefined" class="advisor-detail-section"><h3>能力画像</h3><p>{{ String(advisorPortrait.summary || '暂无画像摘要') }}</p><div class="detail-tags"><span v-for="strength in listOf<string>(advisorPortrait.strengths)" :key="strength">优势：{{ strength }}</span><span v-for="item in listOf<string>(advisorPortrait.explore)" :key="item">待探索：{{ item }}</span></div></section><section class="advisor-detail-section detail-split"><div><h3>目标与计划</h3><p><b>主目标：</b>{{ String(objectOf(advisorGoal.primary).name || '尚未设定') }}</p><p><b>备选目标：</b>{{ String(objectOf(advisorGoal.backup).name || '尚未设定') }}</p><p><b>计划：</b>{{ String(advisorPlan.goalSummary || '尚未制定') }}</p><p><b>状态：</b>{{ String(advisorPlan.status || '—') }}</p></div><div><h3>推荐方向</h3><p v-for="item in advisorRecommendations.slice(0,3)" :key="String(item.directionId)"><b>#{{ item.rank ?? '—' }}</b> {{ String(item.directionId || '方向') }} · {{ item.score ?? '—' }} 分</p><p v-if="!advisorRecommendations.length">暂无推荐结果。</p></div></section><section class="advisor-detail-section"><h3>计划任务</h3><div class="detail-timeline"><article v-for="task in advisorTasks" :key="String(task.id)"><div><b>{{ String(task.title || '未命名任务') }}</b><small>{{ String(task.month || '') }} · {{ String(task.type || '') }} · {{ String(task.status || '') }}</small></div><span>{{ task.deadline ? formatDate(String(task.deadline)) : '无截止日期' }}</span></article><p v-if="!advisorTasks.length">暂无计划任务。</p></div></section><section class="advisor-detail-section"><h3>阶段复盘</h3><div class="detail-timeline"><article v-for="review in advisorReviews" :key="String(review.id)"><div><b>{{ String(review.cycle || '阶段复盘') }}</b><small>{{ String(review.status || '') }} · {{ review.submittedAt ? formatDate(String(review.submittedAt)) : '未提交' }}</small></div><span>{{ review.advisorRequested ? '已请求指导' : '未请求指导' }}</span></article><p v-if="!advisorReviews.length">暂无阶段复盘。</p></div></section><section class="advisor-detail-section"><div class="section-head"><div><h3>历史指导记录</h3><p>按时间顺序展示，发送后会即时追加。</p></div></div><div class="guidance-history"><article v-for="item in advisorStudent.guidance" :key="item.id"><div><em>{{ guidanceTypeLabel(item.adviceType) }}</em><time>{{ formatDate(item.createdAt) }}</time></div><b>{{ item.content }}</b><p v-if="item.suggestedTask">建议任务：{{ item.suggestedTask }}</p><p v-if="item.retestReason">复测原因：{{ item.retestReason }}</p></article><p v-if="!advisorStudent.guidance.length">暂无历史指导记录。</p></div></section><form class="advisor-guidance-form" @submit.prevent="submitGuidance"><p class="eyebrow">WRITE / GUIDANCE</p><h3>发送新的指导</h3><label>类型<select v-model="guidanceType"><option value="COMMENT">指导意见</option><option value="SUGGEST_TASK">建议任务</option><option value="SUGGEST_RETEST">建议重新测评</option></select></label><label>指导内容<textarea v-model.trim="guidanceContent" required maxlength="2000" placeholder="清晰说明观察、建议与下一步行动"></textarea></label><label v-if="guidanceType==='SUGGEST_TASK'">建议任务<input v-model.trim="guidanceSuggestedTask" required maxlength="500" placeholder="例如：完成一次岗位访谈并提交记录"/></label><label v-if="guidanceType==='SUGGEST_RETEST'">重新测评原因<input v-model.trim="guidanceRetestReason" required maxlength="500" placeholder="例如：兴趣偏好变化较大，建议重新完成测评"/></label><div><button type="button" class="outline-btn" @click="advisorStudent=null">取消</button><button class="primary-btn" :disabled="guidanceSubmitting || !guidanceContent">{{ guidanceSubmitting ? '正在发送…' : '发送指导 →' }}</button></div></form></div></section></div>
</template>
