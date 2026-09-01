import type { Role } from '../types/domain'

export type AppRoute = { name:string; label:string; group:string; roles:Role[] }
export const appRoutes:AppRoute[]=[
  {name:'overview',label:'档案概览',group:'个人档案',roles:['STUDENT']},{name:'profile',label:'个人资料',group:'个人档案',roles:['STUDENT']},{name:'experiences',label:'经历管理',group:'个人档案',roles:['STUDENT']},{name:'privacy',label:'隐私与账户',group:'账户设置',roles:['STUDENT']},
  {name:'advisor-overview',label:'工作总览',group:'辅导工作',roles:['ADVISOR']},{name:'students',label:'学生列表',group:'辅导工作',roles:['ADVISOR']},{name:'attention',label:'重点关注',group:'辅导工作',roles:['ADVISOR']},{name:'guidance',label:'指导记录',group:'辅导工作',roles:['ADVISOR']},{name:'statistics',label:'群体统计',group:'辅导工作',roles:['ADVISOR']},{name:'advisor-account',label:'账户与安全',group:'账户设置',roles:['ADVISOR']},
]
export function defaultRoute(role:Role){return role==='STUDENT'?'overview':role==='ADVISOR'?'advisor-overview':'admin-overview'}
export function routesFor(role:Exclude<Role,'ADMIN'>){const groups=new Map<string,[string,string][]>();appRoutes.filter(route=>route.roles.includes(role)).forEach(route=>groups.set(route.group,[...(groups.get(route.group)||[]),[route.name,route.label]]));return [...groups].map(([group,links])=>({group,links}))}
