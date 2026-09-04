import { createRouter, createWebHistory, type RouteLocationRaw, type RouteRecordRaw } from 'vue-router'
import { clearAuthSession, hasAccessToken, hasRefreshToken } from '../api/request'
import type { Role } from '../types/domain'
import { useAuth } from '../composables/useAuth'
import AuthView from '../views/AuthView.vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import AdminModuleView from '../views/admin/AdminModuleView.vue'
import AiPlaygroundPage from '../views/admin/AiPlaygroundPage.vue'
import AdminAiManagementPage from '../views/admin/AdminAiManagementPage.vue'
import ErrorView from '../views/ErrorView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import StudentOverviewPage from '../views/student/StudentOverviewPage.vue'
import StudentProfilePage from '../views/student/StudentProfilePage.vue'
import StudentExperiencesPage from '../views/student/StudentExperiencesPage.vue'
import StudentPrivacyPage from '../views/student/StudentPrivacyPage.vue'
import StudentDevelopmentPage from '../views/student/StudentDevelopmentPage.vue'
import AdvisorDashboardPage from '../views/advisor/AdvisorDashboardPage.vue'
import AdvisorStudentsPage from '../views/advisor/AdvisorStudentsPage.vue'
import AdvisorAttentionPage from '../views/advisor/AdvisorAttentionPage.vue'
import AdvisorGuidancePage from '../views/advisor/AdvisorGuidancePage.vue'
import AdvisorStatisticsPage from '../views/advisor/AdvisorStatisticsPage.vue'
import AdvisorAccountPage from '../views/advisor/AdvisorAccountPage.vue'

export type MenuLink = [path: string, label: string]
export type MenuGroup = { group: string; links: MenuLink[] }
export type MenuGroups = Record<Exclude<Role, 'ADMIN'>, MenuGroup[]>

export const menuGroups: MenuGroups = {
  STUDENT: [
    { group: '个人档案', links: [['/student/overview', '档案概览'], ['/student/profile', '个人资料'], ['/student/experiences', '经历管理'], ['/student/development', '测评与发展']] },
    { group: '账户设置', links: [['/student/privacy', '隐私与账户']] },
  ],
  ADVISOR: [
    { group: '辅导工作', links: [['/advisor/overview', '工作总览'], ['/advisor/students', '学生列表'], ['/advisor/attention', '重点关注'], ['/advisor/guidance', '指导记录'], ['/advisor/statistics', '群体统计']] },
    { group: '账户设置', links: [['/advisor/account', '账户与安全']] },
  ],
}

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'login',
    component: AuthView,
    meta: { guestOnly: true },
  },
  {
    path: '/student',
    component: DefaultLayout,
    meta: { requiresAuth: true, role: 'STUDENT' },
    children: [
      { path: '', redirect: '/student/overview' },
      { path: 'overview', name: 'student-overview', component: StudentOverviewPage },
      { path: 'profile', name: 'student-profile', component: StudentProfilePage },
      { path: 'experiences', name: 'student-experiences', component: StudentExperiencesPage },
      { path: 'privacy', name: 'student-privacy', component: StudentPrivacyPage },
      { path: 'development', name: 'student-development', component: StudentDevelopmentPage },
    ],
  },
  {
    path: '/advisor',
    component: DefaultLayout,
    meta: { requiresAuth: true, role: 'ADVISOR' },
    children: [
      { path: '', redirect: '/advisor/overview' },
      { path: 'overview', name: 'advisor-overview', component: AdvisorDashboardPage },
      { path: 'students', name: 'advisor-students', component: AdvisorStudentsPage },
      { path: 'attention', name: 'advisor-attention', component: AdvisorAttentionPage },
      { path: 'guidance', name: 'advisor-guidance', component: AdvisorGuidancePage },
      { path: 'statistics', name: 'advisor-statistics', component: AdvisorStatisticsPage },
      { path: 'account', name: 'advisor-account', component: AdvisorAccountPage },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/overview' },
      { path: 'overview', name: 'admin-overview', component: AdminModuleView, props: { module: 'admin-overview' } },
      { path: 'users', name: 'admin-users', component: AdminModuleView, props: { module: 'users' } },
      { path: 'whitelist', name: 'admin-whitelist', component: AdminModuleView, props: { module: 'whitelist' } },
      { path: 'relations', name: 'admin-relations', component: AdminModuleView, props: { module: 'relations' } },
      { path: 'directions', name: 'admin-directions', component: AdminModuleView, props: { module: 'admin-directions' } },
      { path: 'abilities', name: 'admin-abilities', component: AdminModuleView, props: { module: 'abilities' } },
      { path: 'templates', name: 'admin-templates', component: AdminModuleView, props: { module: 'templates' } },
      { path: 'curricula', name: 'admin-curricula', component: AdminModuleView, props: { module: 'curricula' } },
      { path: 'weights', name: 'admin-weights', component: AdminModuleView, props: { module: 'weights' } },
      { path: 'exports', name: 'admin-exports', component: AdminModuleView, props: { module: 'exports' } },
      { path: 'logs', name: 'admin-logs', component: AdminModuleView, props: { module: 'logs' } },
      { path: 'ai-playground', name: 'admin-ai-playground', component: AiPlaygroundPage },
      { path: 'ai-management', name: 'admin-ai-management', component: AdminAiManagementPage },
    ],
  },
  {
    path: '/error',
    name: 'error',
    component: ErrorView,
    // 无需登录:全局错误兜底会在任何会话状态下跳转到此页。
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView },
]

const router = createRouter({ history: createWebHistory(), routes })

const DEFAULT_ROUTES: Record<Role, string> = {
  STUDENT: 'student-overview',
  ADVISOR: 'advisor-overview',
  ADMIN: 'admin-overview',
}

export function defaultRouteName(role: Role): string {
  return DEFAULT_ROUTES[role]
}

/** Landing target after login: honour a safe `redirect` query, otherwise the role default. */
export function postLoginTarget(redirect: string | null | undefined, role: Role): RouteLocationRaw {
  if (redirect && redirect.startsWith('/')) {
    const resolved = router.resolve(redirect)
    if (resolved.name && resolved.meta.requiresAuth) {
      const requiredRole = resolved.meta.role as Role | undefined
      if (!requiredRole || requiredRole === role) return resolved.fullPath
    }
  }
  return { name: defaultRouteName(role) }
}

router.beforeEach(async (to) => {
  const auth = useAuth()

  // Tokens may have been invalidated mid-session (e.g. a 401 response cleared them).
  // Drop the stale local session without issuing another `auth.me` request.
  if (auth.loggedIn.value && !hasAccessToken() && !hasRefreshToken()) {
    auth.resetSession()
  }

  await auth.restore()

  if (to.meta.requiresAuth) {
    if (!auth.loggedIn.value) {
      clearAuthSession()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    const requiredRole = to.meta.role as Role | undefined
    if (requiredRole && auth.role.value !== requiredRole) {
      return { name: defaultRouteName(auth.role.value) }
    }
  } else if (to.meta.guestOnly) {
    if (auth.loggedIn.value) {
      return { name: defaultRouteName(auth.role.value) }
    }
  }

  return true
})

export default router
