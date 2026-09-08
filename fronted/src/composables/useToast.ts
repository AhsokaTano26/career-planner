import { ref } from 'vue'

// Module-level singleton state so any composable / component can show the toast.
const message = ref('')
let timer: number | undefined

export function useToast() {
  function show(next: string) {
    window.clearTimeout(timer)
    message.value = next
    timer = window.setTimeout(() => { message.value = '' }, 2200)
  }
  return { toast: message, show }
}

/** 会话清理：登出/401 时清掉可能跨号闪现的提示。 */
export function resetToast() {
  window.clearTimeout(timer)
  timer = undefined
  message.value = ''
}
