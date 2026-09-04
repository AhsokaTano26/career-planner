<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BaseModal from '../../components/BaseModal.vue'
import BaseSelect from '../../components/BaseSelect.vue'
import { api, getErrorMessage } from '../../api/request'
import { useToast } from '../../composables/useToast'

type Row = Record<string, any>
const { show } = useToast()
const tab = ref('model'), busy = ref(''), configs = ref<Row[]>([]), prompts = ref<Row[]>([]), scenes = ref<string[]>([]), scene = ref(''), questionnaires = ref<Row[]>([]), keyword = ref(''), page = ref(1), totalPages = ref(1)
const promptForm = ref({scene:'',version:'',content:''}), questionnaireForm = ref({type:'CAREER',name:'',typeName:'',minutes:'10',tip:'',changeNote:'',questionsText:''}), modal = ref(false), editing = ref<Row|null>(null), versions = ref<Row[]>([])
const sceneOptions = computed(()=>[{value:'',label:'全部场景'},...scenes.value.map(item=>({value:item,label:item}))])
const statusOptions = [{value:'DRAFT',label:'草稿'},{value:'PUBLISHED',label:'已发布'},{value:'DISABLED',label:'已停用'}]
const tabs = [{value:'model',label:'模型配置'},{value:'prompt',label:'提示词版本'},{value:'questionnaire',label:'问卷管理'}]
function rows(value:any){ return Array.isArray(value)?value:(value?.list||[]) }
async function run(key:string, action:()=>Promise<any>){ busy.value=key; try { await action(); show('操作已完成'); await load() } catch(e){show(getErrorMessage(e))} finally{busy.value=''} }
async function load(){ try { if(tab.value==='model'){configs.value=rows(await api.admin.modelConfigs());return} if(tab.value==='prompt'){ prompts.value=rows(await api.admin.prompts(scene.value||undefined)); scenes.value=rows(await api.admin.promptScenes()).map(String); return } const data:any=await api.admin.questionnaires({keyword:keyword.value||undefined,page:page.value,size:20}); questionnaires.value=rows(data); totalPages.value=Number(data?.totalPages||1) }catch(e){show(getErrorMessage(e))} }
function openQuestionnaire(row?:Row){ editing.value=row||null; questionnaireForm.value={type:row?.type||'CAREER',name:row?.name||'',typeName:row?.typeName||'',minutes:String(row?.minutes||10),tip:row?.tip||'',changeNote:'',questionsText:''}; versions.value=[]; modal.value=true; if(row) api.admin.questionnaireVersions(row.id).then(data=>versions.value=rows(data)).catch(e=>show(getErrorMessage(e))) }
function parseQuestions(raw:string){ return raw.split('\n').map(line=>line.trim()).filter(Boolean).map(line=>{const [text,type='CHOICE',dim='interest',options='']=line.split(/[｜|]/).map(item=>item.trim());return {text,type,dim,options:options.split(/[；;]/).map(text=>text.trim()).filter(Boolean).map(text=>({text,scores:{[dim]:1}}))}}).filter(item=>item.text) }
async function saveQuestionnaire(){ const data={type:questionnaireForm.value.type,name:questionnaireForm.value.name,typeName:questionnaireForm.value.typeName,minutes:Number(questionnaireForm.value.minutes||0),tip:questionnaireForm.value.tip,changeNote:questionnaireForm.value.changeNote,questions:parseQuestions(questionnaireForm.value.questionsText)}; await run('questionnaire-save',()=>editing.value?api.admin.updateQuestionnaire(editing.value.id,data):api.admin.createQuestionnaire(data)); modal.value=false }
async function createVersion(){ if(!editing.value) return; const data={changeNote:questionnaireForm.value.changeNote,questions:parseQuestions(questionnaireForm.value.questionsText)}; await run('questionnaire-version',()=>api.admin.createQuestionnaireVersion(editing.value!.id,data)); versions.value=rows(await api.admin.questionnaireVersions(editing.value.id)) }
onMounted(load)
</script>

<template>
  <section class="card data-list-card admin-workbench"><div class="section-head"><div><p class="eyebrow">人工智能治理</p><h2>模型、提示词与测评问卷</h2></div><button class="outline-btn" @click="load">刷新</button></div><nav class="tab-bar"><button v-for="item in tabs" :key="item.value" :class="{active:tab===item.value}" @click="tab=item.value;load()">{{item.label}}</button></nav>
    <template v-if="tab==='model'"><p class="muted">敏感配置在列表中会脱敏；修改后由后端安全保存。</p><div class="simple-list"><div v-for="item in configs" :key="item.configKey"><b>{{item.configKey}}</b><span>最近更新：{{item.updatedAt||'—'}}</span><div class="inline-form"><input v-model="item.configValue" :placeholder="item.masked?'输入新值以更新':''"><button class="primary-btn" @click="run(`config-${item.configKey}`,()=>api.admin.updateModelConfig(item.configKey,{configValue:item.configValue}))">保存</button></div></div><p v-if="!configs.length" class="empty">暂无模型配置</p></div></template>
    <template v-else-if="tab==='prompt'"><div class="inline-form"><BaseSelect v-model="scene" :options="sceneOptions"/><button class="outline-btn" @click="load">筛选</button></div><form class="stack-form" @submit.prevent="run('prompt',()=>api.admin.createPrompt(promptForm));promptForm={scene:'',version:'',content:''}"><input v-model.trim="promptForm.scene" required placeholder="场景，例如 plan_generate"><input v-model.trim="promptForm.version" required placeholder="版本，例如 v1.1"><textarea v-model.trim="promptForm.content" required placeholder="提示词正文"></textarea><button class="primary-btn" :disabled="busy==='prompt'">新增提示词版本</button></form><div class="simple-list"><div v-for="item in prompts" :key="item.id"><b>{{item.scene}} · {{item.version}}</b><span>{{item.status}} · {{item.content}}</span><button v-if="item.status!=='PUBLISHED'" class="outline-btn" @click="run(`publish-${item.id}`,()=>api.admin.publishPrompt(item.id))">发布版本</button></div><p v-if="!prompts.length" class="empty">暂无提示词版本</p></div></template>
    <template v-else><div class="inline-form"><input v-model.trim="keyword" placeholder="搜索问卷"><button class="outline-btn" @click="page=1;load()">搜索</button><button class="primary-btn" @click="openQuestionnaire()">新建问卷</button></div><div class="simple-list"><div v-for="item in questionnaires" :key="item.id"><b>{{item.name}} <small>第 {{item.version||1}} 版</small></b><span>{{item.typeName||item.type}} · {{item.questionCount||0}} 题 · {{item.minutes||0}} 分钟 · {{item.status}}</span><div class="form-actions"><button class="outline-btn" @click="openQuestionnaire(item)">编辑与版本</button><button class="outline-btn" @click="run(`status-${item.id}`,()=>api.admin.updateQuestionnaireStatus(item.id,{status:item.status==='PUBLISHED'?'DISABLED':'PUBLISHED'}))">{{item.status==='PUBLISHED'?'停用':'发布'}}</button></div></div><p v-if="!questionnaires.length" class="empty">暂无问卷</p></div><div v-if="totalPages>1" class="admin-pagination"><button class="outline-btn" :disabled="page<=1" @click="page--;load()">上一页</button><span>{{page}} / {{totalPages}}</span><button class="outline-btn" :disabled="page>=totalPages" @click="page++;load()">下一页</button></div></template>
  </section>
  <BaseModal v-if="modal" @close="modal=false">
    <form class="modal-card admin-editor" @submit.prevent="saveQuestionnaire">
      <p class="eyebrow">{{editing?'编辑问卷':'新建问卷'}}</p><h2>问卷与题目</h2>
      <label>问卷类别<input v-model.trim="questionnaireForm.type" required placeholder="例如 CAREER"></label>
      <label>问卷名称<input v-model.trim="questionnaireForm.name" required></label>
      <label>类别名称<input v-model.trim="questionnaireForm.typeName" placeholder="例如 生涯发展测评"></label>
      <label>预计分钟<input v-model="questionnaireForm.minutes" type="number" min="1"></label>
      <label>填写提示<textarea v-model.trim="questionnaireForm.tip"/></label>
      <label>本次变更说明<input v-model.trim="questionnaireForm.changeNote"/></label>
      <label>题目（每行一题）<textarea v-model.trim="questionnaireForm.questionsText" placeholder="题干｜CHOICE｜interest｜选项一；选项二&#10;题干｜RATING｜ability"></textarea><small>使用“｜”分列：题干、题型（CHOICE 或 RATING）、维度、选项（用分号隔开）。这是扁平输入，系统会转换为题目结构。</small></label>
      <div class="form-actions"><button type="button" class="outline-btn" @click="modal=false">取消</button><button class="primary-btn" :disabled="busy==='questionnaire-save'">保存问卷</button></div>
      <section v-if="editing" class="modal-subsection"><h3>版本记录</h3><div v-for="item in versions" :key="item.id" class="simple-list"><div><b>第 {{item.version}} 版 · {{item.status}}</b><span>{{item.questionCount||0}} 题 · {{item.changeNote||'无说明'}}</span><button v-if="item.status!=='PUBLISHED'" type="button" class="outline-btn" @click="run(`publish-version-${item.id}`,()=>api.admin.publishQuestionnaireVersion(editing?.id || '',item.id))">发布此版本</button></div></div><button type="button" class="outline-btn" @click="createVersion">按上方题目创建新版本</button></section>
    </form>
  </BaseModal>
</template>
