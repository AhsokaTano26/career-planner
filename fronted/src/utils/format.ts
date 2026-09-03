export function formatDateTime(input?:string){
  if(!input) return '暂无记录'
  const date=new Date(input)
  return Number.isNaN(date.getTime()) ? input : new Intl.DateTimeFormat('zh-CN',{year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hour12:false}).format(date).replace(/\//g,'-')
}

export const pathLabel=(value?:string)=>({graduate:'国内升学',employment:'就业发展',overseas:'出国留学',undecided:'待确定'} as Record<string,string>)[value||'']||'待确定'
export const statusLabel=(value?:string)=>({DRAFT:'草稿',CONFIRMED:'已确认',SUBMITTED:'已提交',COMPLETED:'已完成',IN_PROGRESS:'进行中',NOT_STARTED:'未开始',PUBLISHED:'已发布',DISABLED:'已停用'} as Record<string,string>)[value||'']||value||'—'
