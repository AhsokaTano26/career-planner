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
