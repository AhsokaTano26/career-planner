<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '../../components/BasePageHeader.vue'
import { api, getErrorMessage } from '../../api/request'
import { useToast } from '../../composables/useToast'

const { show } = useToast()
const questionnaires = ref<any[]>([]); const sessions = ref<any[]>([]); const portrait = ref<any>(); const goals = ref<any>(); const plan = ref<any>(); const tasks = ref<any[]>([])
const selectedQuestionnaire = ref(''); const goalSummary = ref(''); const taskTitle = ref(''); const taskMonth = ref(new Date().toISOString().slice(0,7))
const busy = ref('')
async function load(){ try { [questionnaires.value,sessions.value,portrait.value,goals.value,plan.value] = await Promise.all([api.student.questionnaires(),api.student.assessmentSessions(),api.student.latestPortrait(),api.student.goals(),api.student.latestPlan()]) as any; const r:any=await api.student.tasks(); tasks.value=r.list??r.items??[]; goalSummary.value=goals.value?.goalSummary??'' } catch(e){ show(getErrorMessage(e)) } }
async function run(name:string, action:()=>Promise<unknown>){ busy.value=name; try{ await action(); show('操作已完成'); await load() }catch(e){ show(getErrorMessage(e)) }finally{busy.value=''} }
onMounted(load)
</script>
<template>
  <PageHeader eyebrow="生涯发展" title="测评、画像与行动计划" description="完成测评后更新个人画像，结合推荐方向设定目标并持续记录任务。"/>
  <div class="dashboard-grid development-grid">
    <section class="card"><p class="eyebrow">生涯测评</p><h2>问卷与测评记录</h2><select v-model="selectedQuestionnaire"><option value="">请选择问卷</option><option v-for="q in questionnaires" :key="q.id" :value="q.id">{{q.name}}</option></select><button class="primary-btn" :disabled="!selectedQuestionnaire||busy==='assessment'" @click="run('assessment',()=>api.student.createAssessment({questionnaireId:selectedQuestionnaire}))">开始测评</button><p class="muted">已创建 {{ sessions.length }} 次测评，可在完成答题后提交评分。</p></section>
    <section class="card"><p class="eyebrow">个人画像</p><h2>{{portrait?.title ?? '生涯画像'}}</h2><p>{{portrait?.summary ?? '画像会综合测评、档案和经历信息生成。'}}</p><button class="secondary-btn" :disabled="busy==='portrait'" @click="run('portrait',api.student.refreshPortrait)">更新画像</button></section>
    <section class="card"><p class="eyebrow">目标与计划</p><h2>本学期目标</h2><input v-model="goalSummary" placeholder="例如：完成一个数据分析项目并明确发展方向"><button class="primary-btn" :disabled="!goalSummary||busy==='goal'" @click="run('goal',()=>api.student.saveGoals({goalSummary}))">保存目标</button><button class="secondary-btn" :disabled="busy==='plan'" @click="run('plan',()=>api.student.draftPlan({semester:'2026-2027-1'}))">生成计划草案</button><p class="muted">{{ plan?.goalSummary ?? '尚未生成计划' }}</p></section>
    <section class="card"><p class="eyebrow">行动任务</p><h2>我的任务</h2><div class="form-row"><input v-model="taskTitle" placeholder="任务名称"><input v-model="taskMonth" type="month"></div><button class="primary-btn" :disabled="!taskTitle||busy==='task'" @click="run('task',()=>api.student.createTask({title:taskTitle,month:taskMonth}))">新增任务</button><ul class="plain-list"><li v-for="task in tasks" :key="task.id">{{task.title}} <small>{{task.status}}</small></li><li v-if="!tasks.length" class="empty-state">暂未创建任务</li></ul></section>
  </div>
</template>
