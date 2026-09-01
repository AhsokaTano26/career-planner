<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { api, clearAuthSession, getErrorMessage, hasAccessToken, hasRefreshToken, setAuthTokens } from './api/request'
import type { AdvisorAttention, AdvisorDetail, AdvisorStatistics, AdvisorStudent, Completeness, ConsentStatus, Experience, ExperienceDraft, Guidance, Profile, ProfileForm, Role, User } from './types/domain'
import AuthView from './views/AuthView.vue'
import StudentOverviewPage from './views/student/StudentOverviewPage.vue'
import StudentProfilePage from './views/student/StudentProfilePage.vue'
import StudentExperiencesPage from './views/student/StudentExperiencesPage.vue'
import StudentPrivacyPage from './views/student/StudentPrivacyPage.vue'
import AdvisorDashboardPage from './views/advisor/AdvisorDashboardPage.vue'
import AdvisorStudentsPage from './views/advisor/AdvisorStudentsPage.vue'
import AdvisorAttentionPage from './views/advisor/AdvisorAttentionPage.vue'
import AdvisorGuidancePage from './views/advisor/AdvisorGuidancePage.vue'
import AdvisorAccountPage from './views/advisor/AdvisorAccountPage.vue'
import AdvisorStatisticsPage from './views/advisor/AdvisorStatisticsPage.vue'
import AdvisorStudentDialog from './components/AdvisorStudentDialog.vue'
import DefaultLayout from './layouts/DefaultLayout.vue'
import AdminLayout from './layouts/AdminLayout.vue'
import { useToast } from './composables/useToast'
import { defaultRoute, routesFor } from './router'

const role=ref<Role>('STUDENT'), active=ref('overview'), loggedIn=ref(false), loading=ref(false), error=ref('')
const { toast, show:notice }=useToast()
const currentUser=ref<User|null>(null), profile=ref<Profile|null>(null), completeness=ref<Completeness|null>(null), experiences=ref<Experience[]>([]), consent=ref<ConsentStatus|null>(null), consentAgreed=ref<boolean|null>(null), studentSaving=ref(false)
const advisorStudents=ref<AdvisorStudent[]>([]), advisorAttention=ref<AdvisorAttention[]>([]), advisorStatistics=ref<AdvisorStatistics|null>(null), advisorLoading=ref(false), advisorError=ref(''), advisorTotal=ref(0), advisorPage=ref(1), advisorTotalPages=ref(1), advisorDetail=ref<AdvisorDetail|null>(null), guidanceSaving=ref(false)
const advisorAnalysisStudents=ref<AdvisorStudent[]>([]), advisorAnalysisLoading=ref(false), advisorAnalysisError=ref('')
const advisorFilters=ref({keyword:'',path:'',goalStatus:'',reviewStatus:'',guidanceRequested:'',sort:'-createdAt'})
const menus:Record<Exclude<Role,'ADMIN'>,{group:string;links:[string,string][]}[]>={STUDENT:routesFor('STUDENT'),ADVISOR:routesFor('ADVISOR')}
function navigate(page:string){active.value=page}
async function loadStudent(){if(role.value!=='STUDENT')return;const [p,c,e,consentStatus]=await Promise.all([api.student.me(),api.student.completeness(),api.student.experiences(),api.auth.consentStatus()]);profile.value=p;completeness.value=c;experiences.value=e;consent.value=consentStatus;consentAgreed.value=consentStatus.agreed}
function advisorQuery(page=advisorPage.value){const params=new URLSearchParams({page:String(page),size:'20',sort:advisorFilters.value.sort||'-createdAt'});Object.entries(advisorFilters.value).forEach(([key,value])=>{if(key!=='sort'&&value)params.set(key,value)});return params.toString()}
async function loadAdvisorStudents(page=advisorPage.value){advisorLoading.value=true;advisorError.value='';try{const data=await api.advisor.students(advisorQuery(page)) as {list?:AdvisorStudent[];total?:number;page?:number;totalPages?:number};advisorStudents.value=data.list||[];advisorTotal.value=data.total||0;advisorPage.value=data.page||page;advisorTotalPages.value=Math.max(data.totalPages||1,1)}catch(e){advisorError.value=getErrorMessage(e)}finally{advisorLoading.value=false}}
async function loadAttention(){advisorLoading.value=true;advisorError.value='';try{advisorAttention.value=await api.advisor.attention() as AdvisorAttention[]}catch(e){advisorError.value=getErrorMessage(e)}finally{advisorLoading.value=false}}
async function loadStatistics(){advisorLoading.value=true;advisorError.value='';try{advisorStatistics.value=await api.advisor.statistics() as AdvisorStatistics}catch(e){advisorError.value=getErrorMessage(e)}finally{advisorLoading.value=false}}
async function loadAdvisorAnalysis(){advisorAnalysisLoading.value=true;advisorAnalysisError.value='';try{const all:AdvisorStudent[]=[];let current=1,totalPages=1;do{const data=await api.advisor.students(`page=${current}&size=100&sort=name`) as {list?:AdvisorStudent[];totalPages?:number};all.push(...(data.list||[]));totalPages=Math.max(data.totalPages||1,1);current++}while(current<=totalPages);advisorAnalysisStudents.value=all}catch(e){advisorAnalysisError.value=getErrorMessage(e)}finally{advisorAnalysisLoading.value=false}}
async function loadAdvisorPage(){if(role.value!=='ADVISOR')return;if(active.value==='students'||active.value==='guidance')await loadAdvisorStudents(active.value==='guidance'?1:advisorPage.value);else if(active.value==='attention')await loadAttention();else if(active.value==='advisor-overview')await loadStatistics();else if(active.value==='statistics')await loadAdvisorAnalysis()}
async function authenticate(payload:{mode:'login'|'register'|'reset';account:string;password:string;studentNo:string;name:string;className:string;verifyCode:string}){error.value='';loading.value=true;try{if(payload.mode==='reset')throw new Error('当前后端仅提供管理员重置密码接口，请联系系统管理员。');const result=payload.mode==='login'?await api.auth.login({account:payload.account,password:payload.password}):await api.auth.register({studentNo:payload.studentNo,name:payload.name,className:payload.className||undefined,verifyCode:payload.verifyCode});setAuthTokens(result);currentUser.value=result.user;role.value=result.user.role;active.value=defaultRoute(role.value);loggedIn.value=true;await Promise.all([loadStudent(),loadAdvisorPage()])}catch(e){error.value=getErrorMessage(e)}finally{loading.value=false}}
async function saveProfile(form:ProfileForm){studentSaving.value=true;try{const number=(v:string)=>v.trim()?Number(v):undefined;const tags=(v:string)=>v.split(/[、,，]/).map(item=>item.trim()).filter(Boolean);profile.value=await api.student.update({basic:{gender:form.gender||undefined,hometown:form.hometown||undefined,birthday:form.birthday||undefined,phone:form.phone||undefined},academic:{math:number(form.math),english:number(form.english),programming:number(form.programming),note:form.academicNote||undefined},abilitySelf:{programming:number(form.abilityProgramming),math:number(form.abilityMath),english:number(form.abilityEnglish),communication:number(form.communication),organization:number(form.organization)},interestPrefs:tags(form.interests),values:tags(form.values),developmentIntention:form.developmentIntention,constraints:tags(form.constraints)});completeness.value=await api.student.completeness();notice('个人资料已保存')}catch(e){notice(getErrorMessage(e))}finally{studentSaving.value=false}}
async function saveExperience(draft:ExperienceDraft){studentSaving.value=true;try{const data={type:draft.type,title:draft.title,startDate:draft.startDate,endDate:draft.endDate||undefined,description:draft.description||undefined,attachment:draft.attachment||undefined};const item=draft.id?await api.student.updateExperience(draft.id,data):await api.student.addExperience(data);const index=experiences.value.findIndex(current=>current.id===item.id);index<0?experiences.value.unshift(item):experiences.value.splice(index,1,item);notice('经历已保存')}catch(e){notice(getErrorMessage(e))}finally{studentSaving.value=false}}
async function removeExperience(id:string){if(!confirm('确定删除这条经历吗？'))return;try{await api.student.deleteExperience(id);experiences.value=experiences.value.filter(item=>item.id!==id);notice('经历已删除')}catch(e){notice(getErrorMessage(e))}}
async function saveConsent(){if(!consent.value?.currentVersion)return;studentSaving.value=true;try{consent.value=await api.auth.consent({version:consent.value.currentVersion});consentAgreed.value=consent.value.agreed;notice('隐私授权已保存')}catch(e){notice(getErrorMessage(e))}finally{studentSaving.value=false}}
async function changePassword(oldPassword:string,newPassword:string){studentSaving.value=true;try{await api.auth.changePassword({oldPassword,newPassword});notice('密码已修改')}catch(e){notice(getErrorMessage(e))}finally{studentSaving.value=false}}
async function saveAdvisorAccount(name:string){studentSaving.value=true;try{currentUser.value=await api.auth.updateMe({name});notice('账户信息已保存')}catch(e){notice(getErrorMessage(e))}finally{studentSaving.value=false}}
async function requestDeletion(reason:string){studentSaving.value=true;try{await api.student.requestDeletion(reason);notice('删除申请已提交')}catch(e){notice(getErrorMessage(e))}finally{studentSaving.value=false}}
async function openAdvisorDetail(id:string){try{const [detail,guidance]=await Promise.all([api.advisor.detail(id),api.advisor.guidance(id)]);advisorDetail.value={id,detail,guidance:guidance as Guidance[]}}catch(e){notice(getErrorMessage(e))}}
async function sendGuidance(payload:{content:string;adviceType:Guidance['adviceType'];suggestedTask?:string;retestReason?:string}){if(!advisorDetail.value)return;guidanceSaving.value=true;try{const action=payload.adviceType==='COMMENT'?api.advisor.writeGuidance:api.advisor.writeAdvice;const item=await action(advisorDetail.value.id,payload) as Guidance;advisorDetail.value.guidance.push(item);notice('指导已发送并保存')}catch(e){notice(getErrorMessage(e))}finally{guidanceSaving.value=false}}
async function logout(){try{await api.auth.logout()}catch{}clearAuthSession();loggedIn.value=false;currentUser.value=null;active.value='overview'}
watch(active,()=>loadAdvisorPage())
onMounted(async()=>{if(!hasAccessToken()&&!hasRefreshToken())return;loading.value=true;try{const user=await api.auth.me();currentUser.value=user;role.value=user.role;active.value=defaultRoute(user.role);loggedIn.value=true;await Promise.all([loadStudent(),loadAdvisorPage()])}catch{clearAuthSession()}finally{loading.value=false}})
</script>

<template>
  <AdminLayout v-if="loggedIn&&role==='ADMIN'" :user="currentUser" @notice="notice" @logout="logout"/>
  <AuthView v-else-if="!loggedIn" :loading="loading" :error="error" @submit="authenticate"/>
  <DefaultLayout v-else :role="role" :user="currentUser" :active="active" :menus="menus[role as Exclude<Role,'ADMIN'>]" @navigate="navigate" @logout="logout">
    <StudentOverviewPage v-if="active==='overview'" :user="currentUser" :profile="profile" :completeness="completeness" @navigate="navigate"/>
    <StudentProfilePage v-else-if="active==='profile'" :profile="profile" :user="currentUser" :saving="studentSaving" @save="saveProfile"/>
    <StudentExperiencesPage v-else-if="active==='experiences'" :experiences="experiences" :saving="studentSaving" @save="saveExperience" @remove="removeExperience"/>
    <StudentPrivacyPage v-else-if="active==='privacy'" :consent="consent" :agreed="consentAgreed" :saving="studentSaving" @consent="saveConsent" @password="changePassword" @deletion="requestDeletion"/>
    <AdvisorDashboardPage v-else-if="active==='advisor-overview'" :statistics="advisorStatistics" :loading="advisorLoading" :error="advisorError" :statistics-mode="false" @refresh="loadStatistics" @navigate="navigate"/>
    <AdvisorStatisticsPage v-else-if="active==='statistics'" :students="advisorAnalysisStudents" :loading="advisorAnalysisLoading" :error="advisorAnalysisError" @refresh="loadAdvisorAnalysis"/>
    <AdvisorStudentsPage v-else-if="active==='students'" :students="advisorStudents" :filters="advisorFilters" :total="advisorTotal" :page="advisorPage" :total-pages="advisorTotalPages" :loading="advisorLoading" :error="advisorError" @update-filters="advisorFilters=$event" @apply="advisorPage=1;loadAdvisorStudents(1)" @reset="advisorFilters={keyword:'',path:'',goalStatus:'',reviewStatus:'',guidanceRequested:'',sort:'-createdAt'};advisorPage=1;loadAdvisorStudents(1)" @page="loadAdvisorStudents" @detail="openAdvisorDetail"/>
    <AdvisorAttentionPage v-else-if="active==='attention'" :items="advisorAttention" :loading="advisorLoading" :error="advisorError" @refresh="loadAttention" @detail="openAdvisorDetail"/>
    <AdvisorGuidancePage v-else-if="active==='guidance'" :students="advisorStudents" :loading="advisorLoading" :error="advisorError" @refresh="loadAdvisorStudents(1)" @detail="openAdvisorDetail"/>
    <AdvisorAccountPage v-else-if="active==='advisor-account'" :user="currentUser" :saving="studentSaving" @save="saveAdvisorAccount" @password="changePassword"/>
  </DefaultLayout>
  <AdvisorStudentDialog v-if="advisorDetail" :student="advisorDetail" :saving="guidanceSaving" @close="advisorDetail=null" @submit="sendGuidance"/>
  <div v-if="toast" class="toast">✓ {{toast}}</div>
</template>
