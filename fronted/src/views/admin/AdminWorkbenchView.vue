<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { api, downloadFile, getErrorMessage, request } from '../../api/request'
import BaseModal from '../../components/BaseModal.vue'

type Row = Record<string, unknown>
type Field = { key:string; label:string; type?:'text'|'select'|'textarea'|'number'; options?:string[]; required?:boolean; placeholder?:string }
const props = defineProps<{ module:string }>()
const emit = defineEmits<{ notice:[message:string] }>()

const names:Record<string,string> = {
  'admin-overview':'运行总览', users:'用户管理', whitelist:'白名单', relations:'师生关系',
  'admin-directions':'方向库', abilities:'能力标签', templates:'任务模板', curricula:'培养方案',
  weights:'推荐权重', exports:'导出任务', logs:'审计日志',
}
const endpoints:Record<string,string> = {
  users:'/admin/users', whitelist:'/admin/whitelist', relations:'/admin/relations', 'admin-directions':'/admin/directions',
  abilities:'/admin/abilities', templates:'/admin/templates', exports:'/admin/exports',
}
const filterSpecs:Record<string,Field[]> = {
  users:[{key:'role',label:'角色',type:'select',options:['','STUDENT','ADVISOR','ADMIN']},{key:'status',label:'状态',type:'select',options:['','ACTIVE','DISABLED']},{key:'keyword',label:'关键词'}],
  whitelist:[{key:'used',label:'使用状态',type:'select',options:['','false','true']},{key:'keyword',label:'关键词'}],
  relations:[{key:'advisorId',label:'辅导员编号'}],
  'admin-directions':[{key:'path',label:'路径',type:'select',options:['','graduate','employment','overseas']},{key:'status',label:'状态',type:'select',options:['','DRAFT','PUBLISHED','DISABLED']},{key:'keyword',label:'关键词'}],
  abilities:[{key:'category',label:'分类'},{key:'keyword',label:'关键词'}],
  templates:[{key:'directionId',label:'方向编号'}],
  logs:[{key:'scene',label:'场景'},{key:'status',label:'状态'},{key:'action',label:'操作类型'},{key:'operator',label:'操作人'},{key:'from',label:'开始时间',type:'text',placeholder:'2026-09-01T00:00:00'},{key:'to',label:'结束时间',type:'text',placeholder:'2026-09-30T23:59:59'}],
  curricula:[{key:'status',label:'审核状态',type:'select',options:['','PENDING','APPROVED','REJECTED','MERGED']}],
}
const editorSpecs:Record<string,Field[]> = {
  users:[{key:'status',label:'账户状态',type:'select',options:['ACTIVE','DISABLED']},{key:'className',label:'班级'},{key:'studentNo',label:'学生学号',placeholder:'仅重置学生密码时填写'},{key:'newPassword',label:'重置后的初始密码',placeholder:'留空则不重置密码'},{key:'reason',label:'重置原因',type:'textarea'}],
  whitelist:[{key:'studentNo',label:'学号',required:true},{key:'className',label:'班级'},{key:'initialPassword',label:'初始密码',placeholder:'留空则由系统自动生成'}],
  relations:[{key:'advisorId',label:'辅导员编号',required:true},{key:'studentIds',label:'学生编号',type:'textarea',required:true,placeholder:'每行一个学生编号，最多 100 个'}],
  'admin-directions':[
    {key:'id',label:'方向编码',required:true},{key:'name',label:'方向名称',required:true},{key:'path',label:'路径',type:'select',options:['graduate','employment','overseas']},{key:'icon',label:'图标'},{key:'intro',label:'简介',type:'textarea'},
    {key:'targetInterest',label:'目标：兴趣',type:'number'},{key:'targetValues',label:'目标：价值观',type:'number'},{key:'targetAbility',label:'目标：能力',type:'number'},
    {key:'targetAcademic',label:'目标：学业',type:'number'},{key:'targetTendency',label:'目标：倾向',type:'number'},{key:'targetPractice',label:'目标：实践',type:'number'},
    {key:'minAbility',label:'最低能力分',type:'number'},{key:'minAcademic',label:'最低学业分',type:'number'},
    {key:'learning',label:'学习建议',type:'textarea',placeholder:'每行一条建议'},{key:'abilities',label:'关联能力',type:'textarea',placeholder:'每行一个能力标签'},{key:'courses',label:'关联课程',type:'textarea',placeholder:'每行一门课程'},
    {key:'activities',label:'关联活动',type:'textarea',placeholder:'每行一项活动'},{key:'pathDesc',label:'路径说明',type:'textarea',placeholder:'每行一条说明'},{key:'misconceptions',label:'常见误区',type:'textarea',placeholder:'每行一条误区'},
    {key:'sortOrder',label:'排序序号',type:'number'},{key:'applicableMajors',label:'适用专业',type:'textarea',placeholder:'每行一个专业'},
  ],
  abilities:[{key:'id',label:'标签编码',required:true},{key:'name',label:'标签名称',required:true},{key:'category',label:'分类'},{key:'status',label:'状态',type:'select',options:['ACTIVE','DISABLED']}],
  templates:[{key:'id',label:'模板编码',required:true},{key:'directionId',label:'方向编码',required:true},{key:'name',label:'模板名称',required:true},{key:'goalSummary',label:'目标摘要',type:'textarea'},{key:'semesterGoals',label:'学期目标',type:'textarea',placeholder:'每行一条：目标名称｜能力标签'},{key:'monthlyTasks',label:'月度任务',type:'textarea',placeholder:'每行一条：月份｜任务名称｜类型｜预计小时'},{key:'status',label:'状态',type:'select',options:['DRAFT','PUBLISHED','DISABLED']}],
  weights:[{key:'version',label:'版本号',required:true},{key:'interest',label:'兴趣权重',type:'number',required:true},{key:'values',label:'价值观权重',type:'number',required:true},{key:'ability',label:'能力权重',type:'number',required:true},{key:'academic',label:'学业权重',type:'number',required:true},{key:'tendency',label:'倾向权重',type:'number',required:true},{key:'practice',label:'实践权重',type:'number',required:true},{key:'minConfidence',label:'最低置信度',type:'number'},{key:'topN',label:'推荐条数',type:'number'}],
  exports:[{key:'type',label:'导出类型',type:'select',required:true,options:['STUDENT_DATA','WHITELIST','OPERATION_LOG','AI_LOG','DIRECTION_LIB']},{key:'scope',label:'导出范围说明',type:'textarea'},{key:'filters',label:'筛选条件',type:'textarea',placeholder:'每行一条：字段名｜字段值'}],
  curriculumItem:[{key:'courseCode',label:'课程代码'},{key:'courseName',label:'课程名称'},{key:'semester',label:'开课学期'},{key:'credits',label:'学分',type:'number'},{key:'hours',label:'学时',type:'number'},{key:'category',label:'课程类别'},{key:'module',label:'课程模块'},{key:'prerequisites',label:'先修课程',type:'textarea',placeholder:'每行一门先修课程'},{key:'abilityTags',label:'能力标签',type:'textarea',placeholder:'每行一个能力标签'},{key:'status',label:'审核结果',type:'select',options:['APPROVED','REJECTED']}],
  curriculumPublish:[{key:'jobId',label:'解析任务编号',required:true},{key:'name',label:'方案名称',required:true},{key:'major',label:'适用专业',required:true}],
}

const rows = ref<Row[]>([]), total = ref(0), page = ref(1), totalPages = ref(1), loading = ref(false), error = ref('')
const filters = ref<Record<string,string>>({}), modal = ref(false), submitting = ref(false), form = ref<Record<string,string>>({}), editing = ref<Row|null>(null), editKind = ref('')
const logTab = ref<'operations'|'ai'>('operations'), curriculumTab = ref<'jobs'|'items'|'versions'>('jobs'), selectedJob = ref<Row|null>(null), jobDetail = ref<Row|null>(null), selectedItems = ref<string[]>([]), batchAction = ref('APPROVE')
const generatedInitialPasswords = ref<{studentNo:string;initialPassword:string}[]>([])
const deleteTarget = ref<Row|null>(null)
let curriculumPollTimer:number|undefined
const title = computed(() => names[props.module] || '管理工作台')
const isPage = computed(() => props.module !== 'weights')
const currentFilters = computed(() => filterSpecs[props.module] || [])
const currentFields = computed(() => editorSpecs[editKind.value || props.module] || [])

function query(values:Record<string,string> = {}) {
  const params = new URLSearchParams({ page:String(page.value), size:'20', sort:'-createdAt' })
  Object.entries(values).forEach(([key,value]) => { if (value !== '') params.set(key, value) })
  return params.toString()
}
function normalizePage(data:unknown) {
  const value = (data || {}) as Row
  rows.value = Array.isArray(value.list) ? value.list as Row[] : Array.isArray(data) ? data as Row[] : value && Object.keys(value).length ? [value] : []
  total.value = Number(value.total || rows.value.length)
  totalPages.value = Math.max(1, Number(value.totalPages || Math.ceil(total.value / 20) || 1))
}
let loadSeq = 0
async function load() {
  if (props.module === 'admin-overview') { rows.value=[]; total.value=0; return }
  const seq = ++loadSeq
  loading.value=true; error.value=''
  try {
    const apply = (data:unknown) => { if (seq === loadSeq) normalizePage(data) }
    if (props.module === 'weights') { apply(await api.admin.weights()); return }
    if (props.module === 'curricula') {
      if (curriculumTab.value === 'items') {
        if (!selectedJob.value?.id) { rows.value=[]; total.value=0; return }
        apply(await request(`/admin/curricula/items?${query({...filters.value,jobId:String(selectedJob.value.id)})}`))
      } else if (curriculumTab.value === 'versions') apply(await request(`/admin/curricula/versions?${query()}`))
      else apply(await request(`/admin/curricula/jobs?${query()}`))
      return
    }
    if (props.module === 'logs') {
      const values = {...filters.value}
      const endpoint = logTab.value === 'ai' ? '/admin/logs/ai' : '/admin/logs/operations'
      if (logTab.value === 'ai') { delete values.action; delete values.operator } else { delete values.scene; delete values.status }
      apply(await request(`${endpoint}?${query(values)}`)); return
    }
    apply(await request(`${endpoints[props.module]}?${query(filters.value)}`))
  } catch (e) { if (seq === loadSeq) error.value=getErrorMessage(e) } finally { if (seq === loadSeq) loading.value=false }
}
function resetAndLoad(){ page.value=1; load() }
function nextPage(delta:number){ const target=page.value+delta; if(target>=1 && target<=totalPages.value){page.value=target;load()} }
function label(key:string){ return ({id:'编号',userId:'用户编号',studentNo:'学号',name:'名称',username:'账号',className:'班级',role:'角色',status:'状态',createdAt:'创建时间',updatedAt:'更新时间',used:'已使用',advisorId:'辅导员编号',advisorName:'辅导员',studentId:'学生编号',studentName:'学生',directionId:'方向编号',version:'版本',type:'类型',filename:'文件名',fileName:'文件名',courseName:'课程名称',courseCode:'课程代码',semester:'学期',credits:'学分',hours:'学时',publishedAt:'发布时间',lastLoginAt:'最近登录',goalSummary:'目标摘要',sortOrder:'排序',applicableMajors:'适用专业',totalItems:'识别课程',parsedItems:'已解析',confidence:'置信度',operator:'操作人',action:'操作',scene:'场景',durationMs:'耗时'} as Record<string,string>)[key] || key }
function optionText(input:unknown){ return ({'':'全部',STUDENT:'学生',ADVISOR:'辅导员',ADMIN:'系统管理员',ACTIVE:'已启用',DISABLED:'已停用',DRAFT:'草稿',PUBLISHED:'已发布',PENDING:'待处理',APPROVED:'已通过',REJECTED:'已驳回',MERGED:'已合并',APPROVE:'批量通过',REJECT:'批量驳回',graduate:'国内升学',employment:'就业发展',overseas:'出国留学',true:'已使用',false:'未使用',STUDENT_DATA:'学生数据',WHITELIST:'白名单',OPERATION_LOG:'操作日志',AI_LOG:'模型调用日志',DIRECTION_LIB:'方向库'} as Record<string,string>)[String(input)] || String(input) }
function value(value:unknown){ if(value===null||value===undefined||value==='') return '—'; if(typeof value==='boolean') return value?'是':'否'; if(Array.isArray(value)) return value.map(item=>typeof item==='object'?'':optionText(item)).filter(Boolean).join('、')||'—'; if(typeof value==='object') return '—'; return optionText(value) }
const weightBoundsKeys=['interest','values','ability','academic','tendency','practice','minConfidence']
function numberBounds(field:Field){ if(field.type!=='number') return undefined; if(field.key.startsWith('target')) return {min:0,max:100}; if(weightBoundsKeys.includes(field.key)) return {min:0,max:1}; return undefined }
function dateTime(input:unknown){ const text=String(input||''); if(!text) return '—'; const date=new Date(text); return Number.isNaN(date.getTime()) ? text.replace('T',' ').replace(/\+08:00$/,'') : date.toLocaleString('zh-CN',{hour12:false}).replace(/\//g,'-') }
function shortText(input:unknown, length=42){ const text=String(input||'').trim(); return text.length>length ? `${text.slice(0,length)}…` : text }
function list(input:unknown){ return Array.isArray(input) ? input.map(item=>String(item)).filter(Boolean) : [] }
function pathLabel(input:unknown){ return ({graduate:'国内升学',employment:'就业发展',overseas:'出国留学'} as Record<string,string>)[String(input)] || String(input||'未设置') }
function roleLabel(input:unknown){ return ({STUDENT:'学生',ADVISOR:'辅导员',ADMIN:'系统管理员'} as Record<string,string>)[String(input)] || String(input||'—') }
function exportTypeLabel(input:unknown){ return ({STUDENT_DATA:'学生数据',WHITELIST:'白名单',OPERATION_LOG:'操作日志',AI_LOG:'模型调用日志',DIRECTION_LIB:'方向库'} as Record<string,string>)[String(input)] || String(input||'导出任务') }
const weightLabels:Record<string,string>={interest:'兴趣',values:'价值观',ability:'能力',academic:'学业',tendency:'发展倾向',practice:'实践'}
function percent(input:unknown){ const number=Number(input); return Number.isFinite(number) ? `${(number*100).toFixed(number*100%1===0?0:1)}%` : '—' }
function confidence(input:unknown){ const number=Number(input); return !Number.isFinite(number)||number===0 ? '不设下限' : percent(number) }
function statusText(input:unknown){ return ({DRAFT:'草稿',PUBLISHED:'已发布',DISABLED:'已停用',COMPLETED:'已完成',PENDING:'待处理',PROCESSING:'处理中',FAILED:'失败',APPROVED:'已通过',REJECTED:'已驳回',MERGED:'已合并',SUCCESS:'成功',ERROR:'失败',ACTIVE:'已启用',UPLOADED:'已上传',PARSING:'解析中'} as Record<string,string>)[String(input)] || value(input) }
function weightValue(row:Row, key:string){ const weights=row.weights; return weights && typeof weights==='object' ? Number((weights as Row)[key] || 0) : 0 }
function barWidth(row:Row, key:string){ return `${Math.max(0,Math.min(100,weightValue(row,key)*100))}%` }
function summary(row:Row){ return Object.entries(row).filter(([key,v])=>!['id','userId','relationId','tagId','directionId','templateId','status','name','username','studentNo','verifyCode','initialPassword','generatedInitialPassword','password','passwordHash'].includes(key)&&v!==null&&v!==undefined&&v!=='').slice(0,4) }
function detailLines(row:Row){
  if(props.module==='users') return [roleLabel(row.role), row.className?`班级：${row.className}`:'未设置班级', row.lastLoginAt?`最近登录：${dateTime(row.lastLoginAt)}`:'从未登录', row.createdAt?`创建于 ${dateTime(row.createdAt)}`:'']
  if(props.module==='whitelist') return [row.className?`班级：${row.className}`:'未填写班级', row.createdAt?`创建于 ${dateTime(row.createdAt)}`:'']
  if(props.module==='relations') return [row.advisorName?`辅导员：${row.advisorName}`:`辅导员编号：${value(row.advisorId)}`, row.studentName?`学生：${row.studentName}`:`学生编号：${value(row.studentId)}`, row.createdAt?`建立于 ${dateTime(row.createdAt)}`:'']
  if(props.module==='admin-directions') return [pathLabel(row.path), list(row.applicableMajors).length?`适用：${list(row.applicableMajors).join('、')}`:'暂未限定专业', row.sortOrder!==undefined?`排序 ${row.sortOrder}`:'']
  if(props.module==='abilities') return [row.category?`分类：${row.category}`:'未分类']
  if(props.module==='templates') return [row.directionId?`方向：${row.directionId}`:'未绑定方向', shortText(row.goalSummary)||'未填写目标摘要', Array.isArray(row.semesterGoals)?`${row.semesterGoals.length} 项学期目标`:'', Array.isArray(row.monthlyTasks)?`${row.monthlyTasks.length} 项月度任务`:'']
  if(props.module==='exports') return [row.scope?shortText(row.scope):'全量导出', row.operator?`创建人：${row.operator}`:'', row.createdAt?`创建于 ${dateTime(row.createdAt)}`:'']
  if(props.module==='logs') return logTab.value==='ai' ? [row.userRef?`调用人：${row.userRef}`:'', row.modelName?`模型：${row.modelName}`:'', row.durationMs!==undefined?`耗时：${row.durationMs} ms`:'', row.time?dateTime(row.time):''] : [row.operator?`操作人：${row.operator}`:'', row.target?`对象：${shortText(row.target)}`:'', row.detail?shortText(row.detail):'', row.time?dateTime(row.time):'']
  if(props.module==='curricula') {
    if(curriculumTab.value==='jobs') return [`识别 ${row.totalItems??0} 门课程 · 已解析 ${row.parsedItems??0} 门`, row.confidence!==undefined?`置信度 ${percent(row.confidence)}`:'', row.createdAt?`提交于 ${dateTime(row.createdAt)}`:'']
    if(curriculumTab.value==='items') return [row.courseCode?`课程代码：${row.courseCode}`:'', row.semester?`开课：${row.semester}`:'', row.credits!==undefined?`${row.credits} 学分`:'', row.abilityTags?`能力：${list(row.abilityTags).join('、')||'未标注'}`:'']
    return [row.major?`专业：${row.major}`:'', row.courseCount!==undefined?`${row.courseCount} 门课程`:'', row.publishedBy?`发布人：${row.publishedBy}`:'', row.publishedAt?`发布于 ${dateTime(row.publishedAt)}`:'']
  }
  return summary(row).map(([key,item])=>`${label(key)}：${value(item)}`)
}
function rowTitle(row:Row){
  if(props.module==='relations') return `${row.advisorName || row.advisorId || '辅导员'} — ${row.studentName || row.studentId || '学生'}`
  if(props.module==='exports') return exportTypeLabel(row.type)
  if(props.module==='logs') return String(logTab.value==='ai' ? row.scene || '模型调用' : row.action || '操作记录')
  if(props.module==='curricula'&&curriculumTab.value==='versions') return String(row.name || '未命名方案')
  return value(row.name || row.username || row.studentNo || row.courseName || row.fileName || row.filename || row.version || row.id)
}
function rowStatus(row:Row){ if(props.module==='whitelist') return row.used?'已使用':'未使用'; if(props.module==='relations') return '已关联'; if(props.module==='logs'&&logTab.value==='operations') return value(row.level)||'记录'; return statusText(row.status) }
function rowStatusClass(row:Row){
  const status=String(props.module==='whitelist' ? (row.used?'USED':'ACTIVE') : props.module==='relations' ? 'ACTIVE' : props.module==='logs'&&logTab.value==='operations' ? row.level||'' : row.status||'').toUpperCase()
  if(/ACTIVE|PUBLISHED|SUCCESS|COMPLETED|APPROVED|USED|INFO/.test(status)) return 'success'
  if(/FAILED|DISABLED|REJECTED|ERROR|WARN/.test(status)) return 'danger'
  return 'neutral'
}
function columnLabels(){ if(props.module==='users') return ['用户编号','账户信息','状态与操作']; if(props.module==='whitelist') return ['白名单编号','学生信息','状态与操作']; if(props.module==='relations') return ['关系编号','师生关系','状态与操作']; if(props.module==='admin-directions') return ['方向编码','方向信息','状态与操作']; if(props.module==='abilities') return ['标签编码','能力标签','状态与操作']; if(props.module==='templates') return ['模板编码','任务模板','状态与操作']; if(props.module==='exports') return ['任务编号','导出内容','状态与操作']; if(props.module==='logs') return ['日志编号','记录内容','处理状态']; if(props.module==='curricula') return curriculumTab.value==='items'?['选择与条目编号','课程信息','审核与操作']:curriculumTab.value==='versions'?['版本编号','方案信息','发布状态']:['任务编号','导入信息','处理状态']; return ['编号','内容','状态与操作'] }
function rowId(row:Row){ return String(row.id || row.userId || row.relationId || row.tagId || row.directionId || row.templateId || '') }
const directionTargetKeys = ['interest','values','ability','academic','tendency','practice']
const directionListKeys = ['learning','abilities','courses','activities','pathDesc','misconceptions','applicableMajors']
const weightKeys = ['interest','values','ability','academic','tendency','practice']
const arrayKeys = ['studentIds','prerequisites','abilityTags',...directionListKeys]
const numericKeys = ['minAbility','minAcademic','sortOrder','minConfidence','topN','credits','hours',...directionTargetKeys,...weightKeys]
function lines(raw:unknown){ return Array.isArray(raw) ? raw.map(item=>String(item)).join('\n') : raw===undefined||raw===null ? '' : String(raw) }
function columns(raw:unknown){ return lines(raw).split('\n').map(line=>line.split(/[｜|]/).map(part=>part.trim())).filter(parts=>parts.some(Boolean)) }
function serialize(row:Row, fields:Field[]){ const next:Record<string,string>={}; fields.forEach(field=>{
  let raw:unknown=row[field.key]
  if(field.key.startsWith('target')) raw=(row.target as Row|undefined)?.[field.key.replace('target','').replace(/^./,letter=>letter.toLowerCase())]
  else if(weightKeys.includes(field.key) && row.weights) raw=(row.weights as Row)[field.key]
  else if(field.key==='semesterGoals') raw=Array.isArray(row.semesterGoals) ? (row.semesterGoals as Row[]).map(goal=>`${goal.title||''}｜${goal.abilityTag||''}`).join('\n') : raw
  else if(field.key==='monthlyTasks') raw=Array.isArray(row.monthlyTasks) ? (row.monthlyTasks as Row[]).map(task=>`${task.month||''}｜${task.title||''}｜${task.taskType||''}｜${task.estimatedHours??''}`).join('\n') : raw
  else if(field.key==='filters' && raw && typeof raw==='object') raw=Object.entries(raw as Row).map(([key,item])=>`${key}｜${String(item)}`).join('\n')
  next[field.key]=arrayKeys.includes(field.key) ? lines(raw) : raw===undefined||raw===null?'':String(raw)
}); return next }
function openCreate(kind=props.module){ editing.value=null; editKind.value=kind; form.value={}; if(kind==='curriculumPublish'&&selectedJob.value?.id) form.value.jobId=String(selectedJob.value.id); modal.value=true }
function openEdit(row:Row, kind=props.module){ editing.value=row; editKind.value=kind; form.value=serialize(row,editorSpecs[kind]||[]); modal.value=true }
function payload(){ const output:Record<string,unknown>={}, target:Record<string,number>={}, weights:Record<string,number>={}
  for(const field of currentFields.value){ const raw=(form.value[field.key]||'').trim(); if(!raw) continue
    if(field.key.startsWith('target')) target[field.key.replace('target','').replace(/^./,letter=>letter.toLowerCase())]=Number(raw)
    else if(weightKeys.includes(field.key) && editKind.value==='weights') weights[field.key]=Number(raw)
    else if(arrayKeys.includes(field.key)) output[field.key]=raw.split(/[\n,，]/).map(item=>item.trim()).filter(Boolean)
    else if(field.key==='semesterGoals') output.semesterGoals=columns(raw).map(([title,abilityTag])=>({title,abilityTag})).filter(item=>item.title)
    else if(field.key==='monthlyTasks') output.monthlyTasks=columns(raw).map(([month,title,taskType,estimatedHours])=>({month,title,taskType,estimatedHours:Number(estimatedHours)})).filter(item=>item.month&&item.title&&item.taskType)
    else if(field.key==='filters') output.filters=Object.fromEntries(columns(raw).filter(([key])=>key).map(([key,item])=>[key,item||'']))
    else if(numericKeys.includes(field.key)) output[field.key]=Number(raw)
    else output[field.key]=raw
  }
  if(Object.keys(target).length) output.target=target
  if(Object.keys(weights).length) {
    const totalWeight=Object.values(weights).reduce((sum,item)=>sum+item,0)
    if(Math.abs(totalWeight-1)>0.001) throw new Error('六项权重之和必须为 1')
    output.weights=weights
  }
  if(Array.isArray(output.studentIds) && output.studentIds.length>100) throw new Error('一次最多建立 100 名学生的师生关系')
  return output
}
async function save(){ submitting.value=true; try { const data=payload(), id=editing.value?rowId(editing.value):''
  if(editKind.value==='curriculumItem') await api.admin.reviewCurriculumItem(id,data)
  else if(editKind.value==='curriculumPublish') await api.admin.publishCurriculum(data)
  else if(props.module==='users') {
    const userData=data as Record<string,unknown>, newPassword=String(userData.newPassword || '')
    const {newPassword: _newPassword, reason, studentNo, ...update}=userData
    await api.admin.updateUser(id,update)
    if(newPassword) { const resetStudentNo=String(studentNo || editing.value?.studentNo || ''); if(!resetStudentNo) throw new Error('重置学生密码时必须填写学生学号'); await api.auth.resetPassword({studentNo:resetStudentNo,newPassword,reason}) }
  }
  else if(props.module==='whitelist') { const created=await api.admin.createWhitelist(data) as Row; const password=String(created.generatedInitialPassword||''); if(password) generatedInitialPasswords.value=[{studentNo:String(created.studentNo||data.studentNo||''),initialPassword:password}] }
  else if(props.module==='relations') await api.admin.createRelations(data)
  else if(props.module==='admin-directions') editing.value ? await api.admin.updateDirection(id,data) : await api.admin.createDirection(data)
  else if(props.module==='abilities') editing.value ? await api.admin.updateAbility(id,data) : await api.admin.createAbility(data)
  else if(props.module==='templates') editing.value ? await api.admin.updateTemplate(id,data) : await api.admin.createTemplate(data)
  else if(props.module==='weights') await api.admin.createWeights(data)
  else if(props.module==='exports') await api.admin.createExport(data)
  modal.value=false; emit('notice','保存成功'); await load()
  } catch(e){ emit('notice',getErrorMessage(e)) } finally { submitting.value=false }
}
function askDelete(row:Row){ deleteTarget.value = row }
async function confirmDelete(){ const row = deleteTarget.value; if(!row) return; deleteTarget.value = null; try { const id = rowId(row); if(props.module==='whitelist') await api.admin.deleteWhitelist(id); else await api.admin.deleteRelation(id); emit('notice','已删除'); await load() } catch(e){emit('notice',getErrorMessage(e))} }
function clearCurriculumPolling(){ if(curriculumPollTimer!==undefined){window.clearTimeout(curriculumPollTimer);curriculumPollTimer=undefined} }
async function pollCurriculumJob(jobId:string, attempt=0){
  try { const job=await api.admin.curriculumJob(jobId) as Row; jobDetail.value=job; selectedJob.value=job; await load(); const status=String(job.status||'').toUpperCase()
    if(['UPLOADED','PARSING','PROCESSING'].includes(status) && attempt<30) curriculumPollTimer=window.setTimeout(()=>pollCurriculumJob(jobId,attempt+1),3000)
    else if(['UPLOADED','PARSING','PROCESSING'].includes(status)) emit('notice','文件已上传，解析服务尚未返回结果；可稍后刷新查看')
    else emit('notice',status==='FAILED'?'解析失败，请查看任务详情':'解析任务状态已更新')
  } catch(e){emit('notice',getErrorMessage(e))}
}
async function importFile(event:Event){ const file=(event.target as HTMLInputElement).files?.[0]; if(!file) return; try { if(props.module==='whitelist') { const result=await api.admin.importWhitelist(file) as Row; const passwords=Array.isArray(result.generatedInitialPasswords)?result.generatedInitialPasswords as Row[]:[]; generatedInitialPasswords.value=passwords.map(item=>({studentNo:String(item.studentNo||''),initialPassword:String(item.initialPassword||'')})).filter(item=>item.studentNo&&item.initialPassword); emit('notice',generatedInitialPasswords.value.length?'白名单导入完成，请记录系统生成的初始密码':'白名单导入完成'); await load() } else { const job=await api.admin.importCurriculum(file) as Row; const jobId=String(job.id||''); emit('notice','文件已上传，正在跟踪解析进度'); if(jobId){clearCurriculumPolling();await pollCurriculumJob(jobId)}else await load() } }catch(e){emit('notice',getErrorMessage(e))}finally{(event.target as HTMLInputElement).value=''} }
async function changeDirectionStatus(row:Row){ const status=String(row.status)==='PUBLISHED'?'DISABLED':'PUBLISHED'; try{await api.admin.setDirectionStatus(rowId(row),{status});emit('notice',`方向已${status==='PUBLISHED'?'发布':'停用'}`);await load()}catch(e){emit('notice',getErrorMessage(e))} }
async function openJob(row:Row, items=false){ try{ jobDetail.value=await api.admin.curriculumJob(rowId(row)) as Row; selectedJob.value=row; if(items){curriculumTab.value='items';page.value=1;await load()} }catch(e){emit('notice',getErrorMessage(e))} }
async function batchReview(){ if(!selectedItems.value.length) return emit('notice','请先选择课程条目'); try{await api.admin.batchReviewCurriculum({actions:selectedItems.value.map(itemId=>({itemId,action:batchAction.value}))});selectedItems.value=[];emit('notice','批量审核已提交');await load()}catch(e){emit('notice',getErrorMessage(e))} }
async function download(row:Row){ try{const file=await downloadFile(`/admin/exports/${rowId(row)}/download`);const url=URL.createObjectURL(file.blob);const a=document.createElement('a');a.href=url;a.download=file.filename;document.body.append(a);a.click();a.remove();URL.revokeObjectURL(url)}catch(e){emit('notice',getErrorMessage(e))} }
function switchLog(tab:'operations'|'ai'){logTab.value=tab;page.value=1;load()}
function switchCurriculum(tab:'jobs'|'items'|'versions'){curriculumTab.value=tab;page.value=1;load()}
watch(()=>props.module,()=>{clearCurriculumPolling();filters.value={};page.value=1;selectedItems.value=[];selectedJob.value=null;jobDetail.value=null;curriculumTab.value='jobs';load()})
onMounted(load)
onBeforeUnmount(clearCurriculumPolling)
</script>

<template>
  <section v-if="props.module==='admin-overview'" class="card admin-overview-card">
    <p class="eyebrow">平台概览</p><h2>平台运行概览</h2>
    <p>通过左侧菜单维护账户、方向库、任务模板、培养方案、导出任务与审计记录。所有数据均由当前后端接口返回。</p>
  </section>
  <section v-else class="card data-list-card admin-workbench">
    <div class="section-head"><div><p class="eyebrow">管理数据</p><h2>{{ title }}</h2></div><div class="list-actions"><button class="outline-btn" @click="load">刷新</button><button v-if="['whitelist','relations','admin-directions','abilities','templates','weights','exports'].includes(props.module)" class="primary-btn compact-btn" @click="openCreate()">新建</button><label v-if="props.module==='whitelist'||props.module==='curricula'" class="outline-btn file-btn">导入文件<input type="file" :accept="props.module==='whitelist'?'.csv,text/csv':'.pdf,.doc,.docx'" @change="importFile"/></label></div></div>
    <div v-if="props.module==='logs'" class="tab-bar"><button :class="{active:logTab==='operations'}" @click="switchLog('operations')">操作日志</button><button :class="{active:logTab==='ai'}" @click="switchLog('ai')">模型调用日志</button></div>
    <div v-if="props.module==='curricula'" class="tab-bar"><button :class="{active:curriculumTab==='jobs'}" @click="switchCurriculum('jobs')">导入任务</button><button :class="{active:curriculumTab==='items'}" :disabled="!selectedJob" @click="switchCurriculum('items')">课程校核</button><button :class="{active:curriculumTab==='versions'}" @click="switchCurriculum('versions')">已发布版本</button><button class="outline-btn compact-btn" @click="openCreate('curriculumPublish')">发布方案</button></div>
    <form v-if="currentFilters.length" class="admin-filters" @submit.prevent="resetAndLoad"><label v-for="field in currentFilters" :key="field.key">{{ field.label }}<select v-if="field.type==='select'" v-model="filters[field.key]"><option v-for="option in field.options" :key="option" :value="option">{{ optionText(option) }}</option></select><input v-else v-model.trim="filters[field.key]" :placeholder="field.placeholder || `输入${field.label}`"/></label><button class="outline-btn" type="submit">应用筛选</button></form>
    <div v-if="props.module==='curricula' && jobDetail" class="job-detail"><b>当前任务：{{ rowTitle(jobDetail) }}</b><span v-for="item in detailLines(jobDetail).filter(Boolean)" :key="item">{{ item }}</span></div>
    <div v-if="props.module==='curricula' && curriculumTab==='items'" class="batch-tools"><span>已选 {{ selectedItems.length }} 条</span><select v-model="batchAction"><option value="APPROVE">批量通过</option><option value="REJECT">批量驳回</option></select><button class="outline-btn" @click="batchReview">执行批量审核</button></div>
    <div v-if="props.module==='weights' && rows.length" class="weight-records"><article v-for="row in rows" :key="rowId(row) || String(row.version)" class="weight-card"><div class="weight-card-head"><div><p class="eyebrow">推荐权重</p><h3>{{ row.version || '未命名版本' }}</h3></div><em :class="String(row.status || '').toLowerCase()">{{ statusText(row.status) }}</em></div><div class="weight-facts"><div><small>最低匹配置信度</small><b>{{ confidence(row.minConfidence) }}</b></div><div><small>每次推荐数量</small><b>{{ row.topN || '—' }} 个方向</b></div></div><div class="weight-bars"><div v-for="key in weightKeys" :key="key"><span>{{ weightLabels[key] }}</span><i><b :style="{ width: barWidth(row,key) }"/></i><strong>{{ percent(weightValue(row,key)) }}</strong></div></div></article></div>
    <div v-if="loading" class="skeleton-group"><div class="skeleton" style="height:46px;margin-bottom:14px"></div><div v-for="i in 6" :key="i" class="skeleton" style="height:56px;margin-bottom:10px"></div></div><p v-else-if="error" class="empty error-state">{{ error }}</p>
    <template v-else-if="props.module!=='weights'"><div class="list-summary"><span>共 <b>{{ total }}</b> 条记录</span><span v-if="isPage">第 {{ page }} / {{ totalPages }} 页</span></div><div class="enhanced-list"><div class="list-columns"><span v-for="heading in columnLabels()" :key="heading">{{ heading }}</span></div><article v-for="(row,index) in rows" :key="rowId(row)" :style="{ '--i': index }"><small v-if="props.module==='curricula'&&curriculumTab==='items'"><input v-model="selectedItems" type="checkbox" :value="rowId(row)"/></small><small v-else>{{ rowId(row) || '—' }}</small><div><b>{{ rowTitle(row) }}</b><span>{{ detailLines(row).filter(Boolean).join(' · ') || '—' }}</span></div><div class="row-actions"><em v-if="rowStatus(row)!=='—'" :class="rowStatusClass(row)">{{ rowStatus(row) }}</em><button v-if="props.module==='users'||props.module==='abilities'||props.module==='templates'||(props.module==='admin-directions')" class="outline-btn" @click="openEdit(row)">编辑</button><button v-if="props.module==='whitelist'||props.module==='relations'" class="outline-btn" @click="askDelete(row)">删除</button><button v-if="props.module==='admin-directions'" class="outline-btn" @click="changeDirectionStatus(row)">{{ String(row.status)==='PUBLISHED'?'停用':'发布' }}</button><button v-if="props.module==='exports'" class="outline-btn" :disabled="String(row.status)!=='COMPLETED'" @click="download(row)">下载</button><button v-if="props.module==='curricula'&&curriculumTab==='jobs'" class="outline-btn" @click="openJob(row,true)">查看并校核</button><button v-if="props.module==='curricula'&&curriculumTab==='items'" class="outline-btn" @click="openEdit(row,'curriculumItem')">审核编辑</button></div></article><p v-if="!rows.length" class="empty">暂无记录。</p></div><div v-if="isPage&&totalPages>1" class="admin-pagination"><button class="outline-btn" :disabled="page<=1" @click="nextPage(-1)">上一页</button><span>{{ page }} / {{ totalPages }}</span><button class="outline-btn" :disabled="page>=totalPages" @click="nextPage(1)">下一页</button></div></template>
  </section>
  <Transition name="modal"><BaseModal v-if="modal" @close="modal=false"><form class="modal-card admin-editor" @submit.prevent="save"><p class="eyebrow">{{ editing ? '编辑' : '新建' }}</p><h2>{{ editKind==='curriculumItem'?'审核课程条目':editKind==='curriculumPublish'?'发布培养方案':title }}</h2><label v-for="field in currentFields" :key="field.key">{{ field.label }}<select v-if="field.type==='select'" v-model="form[field.key]" :required="field.required"><option value="" disabled>请选择</option><option v-for="option in field.options" :key="option" :value="option">{{optionText(option)}}</option></select><textarea v-else-if="field.type==='textarea'" v-model="form[field.key]" :required="field.required" :placeholder="field.placeholder"/><input v-else v-model.trim="form[field.key]" :type="field.type==='number'?'number':'text'" :min="numberBounds(field)?.min" :max="numberBounds(field)?.max" step="any" :required="field.required" :placeholder="field.placeholder"/></label><div><button type="button" class="outline-btn" @click="modal=false">取消</button><button class="primary-btn" :disabled="submitting">{{submitting?'正在保存…':'保存'}}</button></div></form></BaseModal></Transition>
  <Transition name="modal"><BaseModal v-if="generatedInitialPasswords.length" :closeable="false"><section class="modal-card generated-password-dialog" role="dialog" aria-modal="true" aria-labelledby="generated-password-title"><p class="eyebrow">账户创建完成</p><h2 id="generated-password-title">请记录初始密码</h2><p>以下密码由系统自动生成，仅在本次提示中显示。学生首次登录后必须修改密码。</p><div class="generated-password-list"><div v-for="item in generatedInitialPasswords" :key="item.studentNo"><span>{{item.studentNo}}</span><code>{{item.initialPassword}}</code></div></div><div><button class="primary-btn" @click="generatedInitialPasswords=[]">我已记录</button></div></section></BaseModal></Transition>
  <Transition name="modal"><BaseModal v-if="deleteTarget" @close="deleteTarget=null"><section class="modal-card confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="delete-confirm-title"><p class="eyebrow">删除确认</p><h2 id="delete-confirm-title">确定删除这条记录吗？</h2><p>将删除「{{ rowTitle(deleteTarget) }}」，删除后不可恢复。</p><div><button type="button" class="outline-btn" @click="deleteTarget=null">取消</button><button class="primary-btn" @click="confirmDelete">确定删除</button></div></section></BaseModal></Transition>
</template>
