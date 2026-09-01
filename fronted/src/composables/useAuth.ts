import { ref } from 'vue'
import { api, clearAuthSession, getErrorMessage, hasAccessToken, hasRefreshToken, setAuthTokens } from '../api/request'
import type { Role, User } from '../types/domain'
import { useToast } from './useToast'

export type AuthPayload = {
  mode: 'login' | 'register' | 'reset'
  account: string
  password: string
  studentNo: string
  name: string
  className: string
  initialPassword: string
}

// Module-level singleton state: the router guard, App.vue and every view share one session.
const currentUser = ref<User | null>(null)
const role = ref<Role>('STUDENT')
const loggedIn = ref(false)
const loading = ref(false)
const error = ref('')
const forcePasswordChange = ref(false)
const forcedPasswordSaving = ref(false)
/** Generic "an action is in flight" flag shared by student/advisor account actions. */
const saving = ref(false)

let restoring: Promise<void> | null = null

export function useAuth() {
  const { show: notice } = useToast()

  /**
   * Restore the session exactly once per login cycle. Idempotent: concurrent
   * callers (router guard + any early mounted component) share one `auth.me`.
   */
  function restore(): Promise<void> {
    if (restoring) return restoring
    restoring = (async () => {
      if (!hasAccessToken() && !hasRefreshToken()) return
      loading.value = true
      try {
        const user = await api.auth.me()
        currentUser.value = user
        role.value = user.role
        loggedIn.value = true
        forcePasswordChange.value = Boolean(user.passwordChangeRequired)
      } catch {
        clearAuthSession()
      } finally {
        loading.value = false
      }
    })()
    return restoring
  }

  /** Returns true when the user is signed in (redirect/landing is up to the caller). */
  async function authenticate(payload: AuthPayload): Promise<boolean> {
    error.value = ''
    loading.value = true
    try {
      if (payload.mode === 'reset') throw new Error('当前后端仅提供管理员重置密码接口，请联系系统管理员。')
      const result = payload.mode === 'login'
        ? await api.auth.login({ account: payload.account, password: payload.password })
        : await api.auth.register({
            studentNo: payload.studentNo,
            name: payload.name,
            className: payload.className || undefined,
            initialPassword: payload.initialPassword,
          })
      setAuthTokens(result)
      currentUser.value = result.user
      role.value = result.user.role
      loggedIn.value = true
      forcePasswordChange.value = Boolean(result.user.passwordChangeRequired)
      return true
    } catch (e) {
      error.value = getErrorMessage(e)
      return false
    } finally {
      loading.value = false
    }
  }

  /** Drop the local session without relying on the server (used by 401 invalidation too). */
  function resetSession() {
    currentUser.value = null
    role.value = 'STUDENT'
    loggedIn.value = false
    forcePasswordChange.value = false
    restoring = null
  }

  async function logout() {
    try { await api.auth.logout() } catch { /* server may already have dropped the session */ }
    clearAuthSession()
    resetSession()
  }

  async function completeForcedPasswordChange(oldPassword: string, newPassword: string) {
    forcedPasswordSaving.value = true
    try {
      await api.auth.changePassword({ oldPassword, newPassword })
      currentUser.value = await api.auth.me()
      forcePasswordChange.value = Boolean(currentUser.value?.passwordChangeRequired)
      if (forcePasswordChange.value) throw new Error('密码修改状态未更新，请重试')
    } finally {
      forcedPasswordSaving.value = false
    }
  }

  async function saveAccount(name: string) {
    saving.value = true
    try {
      currentUser.value = await api.auth.updateMe({ name })
      notice('账户信息已保存')
    } catch (e) {
      notice(getErrorMessage(e))
    } finally {
      saving.value = false
    }
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    saving.value = true
    try {
      await api.auth.changePassword({ oldPassword, newPassword })
      notice('密码已修改')
    } catch (e) {
      notice(getErrorMessage(e))
    } finally {
      saving.value = false
    }
  }

  return {
    currentUser, role, loggedIn, loading, error,
    forcePasswordChange, forcedPasswordSaving, saving,
    restore, authenticate, logout, resetSession,
    completeForcedPasswordChange, saveAccount, changePassword,
  }
}
