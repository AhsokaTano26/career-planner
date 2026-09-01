<script setup lang="ts">
import PageHeader from '../../components/BasePageHeader.vue'
import { useAdvisorAttention } from '../../composables/useAdvisorAttention'
import { useAdvisorDetail } from '../../composables/useAdvisorDetail'
const { items, loading, error, load } = useAdvisorAttention()
const { open: openDetail } = useAdvisorDetail()
const path=(value?:string)=>({graduate:'国内升学',employment:'就业发展',overseas:'出国留学'} as Record<string,string>)[value||'']||'待确定'
</script>
<template><PageHeader eyebrow="辅导工作" title="重点关注" description="查看需要优先跟进的学生及原因。"><template #actions><button class="outline-btn" @click="load">刷新</button></template></PageHeader><section class="card data-list-card"><p v-if="loading" class="empty">正在读取关注学生…</p><p v-else-if="error" class="empty error-state">{{error}}</p><div v-else class="attention-list"><article v-for="(item,index) in items" :key="item.student.id" :style="{ '--i': index }"><div><p class="eyebrow">{{item.student.id}}</p><h3>{{item.student.name}}</h3><p>{{item.student.className||'未填写班级'}} · {{path(item.student.path)}}</p></div><div class="attention-reasons"><span v-for="(reason,index) in item.reasons" :key="`${item.student.id}-${index}`">{{reason}}</span></div><button class="outline-btn" @click="openDetail(item.student.id)">查看并指导</button></article><p v-if="!items.length" class="empty">当前没有需要重点关注的学生。</p></div></section></template>
