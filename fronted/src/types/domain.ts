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
export type AdvisorDetail = { id:string; detail:unknown; guidance:Guidance[] }
export type ProfileForm = { gender:string; hometown:string; birthday:string; phone:string; math:string; english:string; programming:string; academicNote:string; abilityProgramming:string; abilityMath:string; abilityEnglish:string; communication:string; organization:string; interests:string; values:string; developmentIntention:string; constraints:string }
export type ExperienceDraft = { id?:string; type:string; title:string; startDate:string; endDate:string; description:string; attachment:string }
