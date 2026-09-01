<script setup lang="ts">
import { computed } from 'vue'
import type { AdvisorStudent } from '../../types/domain'
import PageHeader from '../../components/BasePageHeader.vue'
import { useAdvisorAnalysis } from '../../composables/useAdvisorAnalysis'

const { students, loading, error, load } = useAdvisorAnalysis()
type Distribution={label:string;count:number}

const pathName=(value?:string)=>({graduate:'国内升学',employment:'就业发展',overseas:'出国留学'} as Record<string,string>)[value||'']||'待确定'
const average=(values:(number|undefined)[])=>{const valid=values.filter((value):value is number=>typeof value==='number');return valid.length?Math.round(valid.reduce((sum,value)=>sum+value,0)/valid.length):0}
const distribute=(items:AdvisorStudent[], key:(student:AdvisorStudent)=>string)=>{const counts=new Map<string,number>();items.forEach(student=>{const label=key(student);counts.set(label,(counts.get(label)||0)+1)});return [...counts.entries()].map(([label,count])=>({label,count})).sort((a,b)=>b.count-a.count||a.label.localeCompare(b.label,'zh-CN'))}

const averageCompleteness=computed(()=>average(students.value.map(student=>student.completeness)))
const averagePlanRate=computed(()=>average(students.value.map(student=>student.planRate)))
const assessedCount=computed(()=>students.value.filter(student=>student.assessed).length)
const priorityCount=computed(()=>students.value.filter(student=>student.status==='review'||student.status==='late').length)
const classDistribution=computed(()=>distribute(students.value,student=>student.className||'未填写班级'))
const pathDistribution=computed(()=>distribute(students.value,student=>pathName(student.path)))
const completenessDistribution=computed(()=>{
  const ranges:[string,(value:number)=>boolean][]=[['未完善档案',value=>value===0],['1—59 分',value=>value>0&&value<60],['60—79 分',value=>value>=60&&value<80],['80 分及以上',value=>value>=80]]
  return ranges.map(([label,match])=>({label,count:students.value.filter(student=>match(student.completeness??0)).length}))
})
const reviewDistribution=computed(()=>{
  const today=Date.now(), day=24*60*60*1000
  const ranges:[string,(student:AdvisorStudent)=>boolean][]=[
    ['7 天内已复盘',student=>Boolean(student.lastReview)&&today-new Date(student.lastReview!).getTime()<=7*day],
    ['8—30 天未复盘',student=>Boolean(student.lastReview)&&today-new Date(student.lastReview!).getTime()>7*day&&today-new Date(student.lastReview!).getTime()<=30*day],
    ['超过 30 天未复盘',student=>Boolean(student.lastReview)&&today-new Date(student.lastReview!).getTime()>30*day],
    ['尚未提交复盘',student=>!student.lastReview],
  ]
  return ranges.map(([label,match])=>({label,count:students.value.filter(match).length}))
})
const maxCount=(data:Distribution[])=>Math.max(1,...data.map(item=>item.count))
</script>

<template>
  <PageHeader eyebrow="辅导工作" title="群体统计" description="按班级、发展路径、档案完整度与复盘时效，了解所带学生的整体情况。">
    <template #actions><button class="outline-btn" @click="load">刷新统计</button></template>
  </PageHeader>
  <p v-if="loading" class="empty">正在汇总学生数据…</p>
  <p v-else-if="error" class="empty error-state">{{error}}</p>
  <template v-else>
    <div class="metric-grid advisor-metrics analysis-metrics">
      <article><b>{{students.length}}</b><span>纳入统计学生</span></article>
      <article><b>{{averageCompleteness}}<small>%</small></b><span>平均档案完整度</span></article>
      <article><b>{{averagePlanRate}}<small>%</small></b><span>平均计划完成率</span></article>
      <article><b>{{priorityCount}}</b><span>需优先跟进学生</span></article>
    </div>
    <section v-if="students.length" class="analysis-grid">
      <article class="analysis-card"><div class="analysis-card-head"><div><p class="eyebrow">班级对比</p><h2>各班学生数量</h2></div><span>{{classDistribution.length}} 个班级</span></div><div class="analysis-bars"><div v-for="item in classDistribution" :key="item.label"><div><b>{{item.label}}</b><span>{{item.count}} 人</span></div><i><em :style="{width:`${item.count/maxCount(classDistribution)*100}%`}"/></i></div></div></article>
      <article class="analysis-card"><div class="analysis-card-head"><div><p class="eyebrow">方向对比</p><h2>发展路径分布</h2></div><span>已设置方向</span></div><div class="analysis-bars"><div v-for="item in pathDistribution" :key="item.label"><div><b>{{item.label}}</b><span>{{item.count}} 人</span></div><i><em :style="{width:`${item.count/maxCount(pathDistribution)*100}%`}"/></i></div></div></article>
      <article class="analysis-card"><div class="analysis-card-head"><div><p class="eyebrow">档案完整度</p><h2>档案分布</h2></div><span>平均 {{averageCompleteness}}%</span></div><div class="analysis-bars"><div v-for="item in completenessDistribution" :key="item.label"><div><b>{{item.label}}</b><span>{{item.count}} 人</span></div><i><em :style="{width:`${item.count/maxCount(completenessDistribution)*100}%`}"/></i></div></div></article>
      <article class="analysis-card"><div class="analysis-card-head"><div><p class="eyebrow">复盘时效</p><h2>近期复盘动态</h2></div><span>{{assessedCount}} 人已完成测评</span></div><div class="analysis-bars"><div v-for="item in reviewDistribution" :key="item.label"><div><b>{{item.label}}</b><span>{{item.count}} 人</span></div><i><em :style="{width:`${item.count/maxCount(reviewDistribution)*100}%`}"/></i></div></div><p class="analysis-note">按每名学生最近一次提交的复盘时间统计。</p></article>
    </section>
    <p v-else class="empty">当前没有可供统计的学生数据。</p>
  </template>
</template>
