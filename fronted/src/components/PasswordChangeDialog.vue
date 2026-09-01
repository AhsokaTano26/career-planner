<script setup lang="ts">
import { ref } from 'vue'
import BaseModal from './BaseModal.vue'

defineProps<{saving:boolean;mandatory?:boolean}>()
const emit=defineEmits<{submit:[oldPassword:string,newPassword:string];close:[]}>()
const oldPassword=ref(''), newPassword=ref('')
function submit(){emit('submit',oldPassword.value,newPassword.value)}
</script>

<template>
  <BaseModal :closeable="!mandatory" @close="emit('close')">
    <form class="modal-card" aria-modal="true" role="dialog" aria-labelledby="password-dialog-title" @submit.prevent="submit">
      <p class="eyebrow">账户安全</p><h2 id="password-dialog-title">{{mandatory?'请先修改初始密码':'修改密码'}}</h2>
      <p v-if="mandatory" class="dialog-hint">为保护账户安全，首次登录必须设置新的个人密码后才能进入工作台。</p>
      <label>当前密码<input v-model="oldPassword" required type="password" autocomplete="current-password"></label>
      <label>新密码<input v-model="newPassword" required type="password" minlength="6" maxlength="128" autocomplete="new-password"></label>
      <p class="dialog-hint">新密码至少 6 位，且同时包含字母和数字。</p>
      <div><button v-if="!mandatory" type="button" class="outline-btn" @click="emit('close')">取消</button><button class="primary-btn" :disabled="saving">{{saving?'正在保存…':'确认修改'}}</button></div>
    </form>
  </BaseModal>
</template>
