import type { Completeness, ConsentStatus, Experience, Profile, User } from '../api/request'

export type Role = 'STUDENT' | 'ADVISOR' | 'ADMIN'
export type { Completeness, ConsentStatus, Experience, Profile, User }

export type AdvisorStudent = {
  id:string; name:string; className?:string; completeness?:number; assessed?:boolean; path?:string
  direction?:string; primaryGoal?:string; planRate?:number; lastReview?:string; askGuidance?:boolean; status?:string
}
export type AdvisorAttention = { student:AdvisorStudent; reasons:string[] }
export type AdvisorStatistics = {
  totalStudents?:number; assessedCount?:number; planMadeCount?:number; reviewedCount?:number
  taskCompletionRate?:number; pathDistribution?:{path:string;count:number}[]
}
export type Guidance = { id:string; studentId:string; content:string; adviceType:'COMMENT'|'SUGGEST_TASK'|'SUGGEST_RETEST'; suggestedTask?:string; retestReason?:string; createdAt?:string }
export type AdvisorFilters = { keyword:string; path:string; directionId:string; goalStatus:string; reviewStatus:string; guidanceRequested:boolean|undefined; sort:string }
/** 学生详情 8 段（与后端 StudentDetailViewVO 对齐；未知字段保留字符串索引以兼容后端增补） */
export type StudentDetailView = {
  profile?: { name?:string; className?:string; completeness?:number; developmentIntention?:string; majorCategory?:string; interestPrefs?:string[] } & Record<string, unknown>
  portrait?: { summary?:string; completeness?:number; strengths?:string[]; explore?:string[] } & Record<string, unknown>
  recommendation?: { results?: { directionId?:string; rank?:number; score?:number }[] } & Record<string, unknown>
  goal?: { primary?: { name?:string }; backup?: { name?:string } } & Record<string, unknown>
  plan?: { goalSummary?:string; status?:string } & Record<string, unknown>
  tasks?: { id?:string; title?:string; month?:string; type?:string; taskType?:string; status?:string; deadline?:string }[]
  reviews?: { id?:string; cycle?:string; status?:string; submittedAt?:string; advisorRequested?:boolean }[]
  guidance?: Guidance[]
} & Record<string, unknown>
export type AdvisorDetail = { id:string; detail:StudentDetailView; guidance:Guidance[] }
export type ProfileForm = { gender:string; hometown:string; birthday:string; phone:string; math:string; english:string; programming:string; academicNote:string; abilityProgramming:string; abilityMath:string; abilityEnglish:string; communication:string; organization:string; interests:string; values:string; developmentIntention:string; constraints:string }
export type ExperienceDraft = { id?:string; type:string; title:string; startDate:string; endDate:string; description:string; attachment:string }
