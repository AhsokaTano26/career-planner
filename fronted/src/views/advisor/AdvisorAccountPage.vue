<script setup lang="ts">
import { ref, watch } from 'vue'
import type { User } from '../../types/domain'
import PageHeader from '../../components/BasePageHeader.vue'

const props=defineProps<{user:User|null;saving:boolean}>()
const emit=defineEmits<{save:[name:string],password:[oldPassword:string,newPassword:string]}>()
const action=ref<'profile'|'password'|null>(null)
const name=ref(''), oldPassword=ref(''), newPassword=ref('')
watch(()=>props.user?.name,value=>{name.value=value||''},{immediate:true})
function openProfile(){name.value=props.user?.name||'';action.value='profile'}
function close(){action.value=null;oldPassword.value='';newPassword.value=''}
function submit(){if(action.value==='profile')emit('save',name.value);else if(action.value==='password')emit('password',oldPassword.value,newPassword.value);close()}
</script>

<template>
  <PageHeader eyebrow="账户设置" title="账户与安全" description="维护您的展示信息和登录密码；登录账号、角色与权限由系统统一管理。"/>
  <section class="card account-summary"><p class="eyebrow">当前账户</p><div class="account-facts"><div><small>姓名</small><b>{{user?.name||'—'}}</b></div><div><small>登录账号</small><b>{{user?.username||'—'}}</b></div><div><small>身份</small><b>辅导员</b></div></div></section>
  <section class="settings-list mt-24"><article><div><b>展示姓名</b><span>用于工作台、指导记录等系统内展示。</span></div><button class="outline-btn" @click="openProfile">修改姓名</button></article><article><div><b>登录密码</b><span>新密码须至少 6 位，且同时包含字母和数字。</span></div><button class="outline-btn" @click="action='password'">修改密码</button></article></section>
  <div v-if="action" class="modal-mask"><form class="modal-card" @submit.prevent="submit"><p class="eyebrow">账户设置</p><h2>{{action==='profile'?'修改展示姓名':'修改密码'}}</h2><label v-if="action==='profile'">展示姓名<input v-model.trim="name" required maxlength="50" autocomplete="name"></label><template v-else><label>当前密码<input v-model="oldPassword" required type="password" autocomplete="current-password"></label><label>新密码<input v-model="newPassword" required type="password" minlength="6" maxlength="128" autocomplete="new-password"></label></template><div><button type="button" class="outline-btn" @click="close">取消</button><button class="primary-btn" :disabled="saving">{{saving?'正在保存…':'保存修改'}}</button></div></form></div>
</template>
