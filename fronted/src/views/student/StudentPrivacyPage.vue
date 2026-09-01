<script setup lang="ts">
import { ref } from 'vue'
import PageHeader from '../../components/BasePageHeader.vue'
import BaseModal from '../../components/BaseModal.vue'
import { useAuth } from '../../composables/useAuth'
import { useStudent } from '../../composables/useStudent'
const { consent, consentAgreed: agreed, saving, saveConsent, requestDeletion } = useStudent()
const { changePassword } = useAuth()
const action=ref<'password'|'delete'|null>(null), oldPassword=ref(''), newPassword=ref(''), reason=ref('')
async function submit(){const current=action.value;const ok=current==='password'?await changePassword(oldPassword.value,newPassword.value):current==='delete'?await requestDeletion(reason.value):true;if(ok){action.value=null;oldPassword.value='';newPassword.value='';reason.value=''}}
</script>
<template><PageHeader eyebrow="账户设置" title="隐私与账户" description="管理数据授权、密码和数据删除申请。"/><section class="settings-list"><article><div><b>隐私授权</b><span>{{agreed?`已同意 ${consent?.version||''} 生涯规划数据处理授权`:`请阅读并同意当前 ${consent?.currentVersion||'—'} 版本授权文本`}}</span><small v-if="consent?.content">{{consent.content}}</small></div><button class="outline-btn" :disabled="agreed===true||saving||!consent?.currentVersion" @click="saveConsent">{{agreed?'已授权':saving?'正在保存…':'同意当前授权'}}</button></article><article><div><b>修改密码</b><span>定期更换密码，保护账户安全。</span></div><button class="outline-btn" @click="action='password'">修改密码</button></article><article><div><b>删除申请</b><span>提交后将进入人工审核流程。</span></div><button class="outline-btn" @click="action='delete'">提交申请</button></article></section><BaseModal v-if="action" @close="action=null"><form class="modal-card" @submit.prevent="submit"><p class="eyebrow">账户设置</p><h2>{{action==='password'?'修改密码':'提交删除申请'}}</h2><template v-if="action==='password'"><label>当前密码<input v-model="oldPassword" required type="password"></label><label>新密码<input v-model="newPassword" required type="password" minlength="6" maxlength="128" pattern="(?=.*[a-zA-Z])(?=.*\d).*" title="至少 6 位，且同时包含字母和数字"></label></template><label v-else>申请原因（可选）<textarea v-model.trim="reason" maxlength="255"></textarea></label><div><button type="button" class="outline-btn" @click="action=null">取消</button><button class="primary-btn" :disabled="saving">{{saving?'正在提交…':'确认提交 →'}}</button></div></form></BaseModal></template>
