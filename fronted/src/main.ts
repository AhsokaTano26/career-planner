import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/styles/styles.css'
import './assets/styles/functional.css'
import './assets/styles/lists.css'
import './assets/styles/logout.css'
import './assets/styles/cqu-theme.css'
import './assets/styles/motion.css'
import { installCustomSelects } from './plugins/customSelect'
import { encodeBase64 } from './utils/base64'

function describeError(error: unknown, context?: string): string {
  let message: string
  if (error instanceof Error) {
    message = error.stack || error.message
  } else if (typeof error === 'string') {
    message = error
  } else if (error !== null && error !== undefined) {
    try {
      message = JSON.stringify(error)
    } catch {
      message = String(error)
    }
  } else {
    message = '未知错误'
  }
  return [context, message].filter(Boolean).join('\n')
}

/** 将无法就地展示的错误路由到独立错误页,并以 base64 错误码携带详情。 */
function routeToError(error: unknown, context?: string) {
  try {
    if (router.currentRoute.value.name === 'error') return
    const detail = describeError(error, context)
    router.replace({ name: 'error', query: { msg: encodeBase64(detail) } })
  } catch {
    // 错误页本身异常时不再跳转,避免死循环。
  }
}

const app = createApp(App)
app.config.errorHandler = (error, _instance, info) => routeToError(error, info)
window.addEventListener('unhandledrejection', (event) => {
  routeToError(event.reason, 'unhandledrejection')
})
router.onError((error, to, from) => routeToError(error, `路由导航失败 ${from.fullPath} → ${to.fullPath}`))
app.use(router)
app.mount('#app')
installCustomSelects()
