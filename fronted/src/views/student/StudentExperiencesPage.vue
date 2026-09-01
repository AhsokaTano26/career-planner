<script setup lang="ts">
import { ref } from 'vue'
import type { Experience, ExperienceDraft } from '../../types/domain'
import PageHeader from '../../components/BasePageHeader.vue'
import BaseSelect from '../../components/BaseSelect.vue'
import BaseModal from '../../components/BaseModal.vue'
import { useStudent } from '../../composables/useStudent'
const { experiences, saving, saveExperience, removeExperience } = useStudent()
const editing=ref(false), draft=ref<ExperienceDraft>({type:'project',title:'',startDate:'',endDate:'',description:'',attachment:''})
const labels:Record<string,string>={project:'项目',internship:'实习',competition:'竞赛',club:'社团',research:'科研',volunteer:'志愿服务'}
function create(){draft.value={type:'project',title:'',startDate:'',endDate:'',description:'',attachment:''};editing.value=true}
function edit(item:Experience){draft.value={id:item.id,type:item.type,title:item.title,startDate:item.startDate,endDate:item.endDate||'',description:item.description||'',attachment:item.attachmentUrl||''};editing.value=true}
function save(){editing.value=false;saveExperience(draft.value)}
</script>
<template><PageHeader eyebrow="个人档案" title="经历管理" description="维护项目、实习、竞赛和社团等成长经历。"><template #actions><button class="primary-btn" @click="create">新增经历 →</button></template></PageHeader><section class="card"><div class="record-list"><article v-for="item in experiences" :key="item.id"><small>{{ labels[item.type] || item.type || '经历' }}</small><div><b>{{ item.title }}</b><span>{{ item.startDate }}{{ item.endDate ? ` 至 ${item.endDate}` : ' 至今' }}{{ item.description ? ` · ${item.description}` : '' }}</span></div><div class="row-actions"><button class="outline-btn" @click="edit(item)">编辑</button><button class="outline-btn" @click="removeExperience(item.id)">删除</button></div></article><p v-if="!experiences.length" class="empty">暂未记录经历。</p></div></section><BaseModal v-if="editing" @close="editing=false"><form class="modal-card" @submit.prevent="save"><p class="eyebrow">{{draft.id?'编辑经历':'新建经历'}}</p><h2>{{ draft.id?'编辑经历':'新建经历' }}</h2><label>经历类别<BaseSelect v-model="draft.type" :options="Object.entries(labels).map(([value,label])=>({value,label}))" required/></label><label>经历名称<input v-model.trim="draft.title" required maxlength="100"></label><label>开始时间<input v-model="draft.startDate" required pattern="\d{4}-\d{2}" placeholder="例如：2026-09"></label><label>结束时间（可选）<input v-model="draft.endDate" pattern="\d{4}-\d{2}" placeholder="例如：2026-12"></label><label>说明<textarea v-model.trim="draft.description" maxlength="2000" placeholder="你负责的内容、成果或收获"></textarea></label><label>附件地址（可选）<input v-model.trim="draft.attachment" maxlength="255"></label><div><button type="button" class="outline-btn" @click="editing=false">取消</button><button class="primary-btn" :disabled="saving">{{saving?'正在保存…':'保存'}}</button></div></form></BaseModal></template>
