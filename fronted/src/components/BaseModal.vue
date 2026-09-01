<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(defineProps<{
  /** When false, ESC 与点击遮罩都无法关闭弹窗(如强制改密、必须记录的初始密码)。 */
  closeable?: boolean
}>(), { closeable: true })

const emit = defineEmits<{ close: [] }>()
const mask = ref<HTMLElement | null>(null)
let previousFocus: Element | null = null

function dismiss() {
  if (props.closeable) emit('close')
}
function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    dismiss()
    return
  }
  // Simple focus trap: keep Tab within the dialog.
  if (event.key !== 'Tab') return
  const focusable = mask.value?.querySelectorAll<HTMLElement>(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
  )
  if (!focusable?.length) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}
onMounted(() => {
  previousFocus = document.activeElement
  window.addEventListener('keydown', onKeydown)
  mask.value?.querySelector<HTMLElement>('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')?.focus()
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  if (previousFocus instanceof HTMLElement) previousFocus.focus()
})
</script>

<template>
  <div ref="mask" class="modal-mask" role="dialog" aria-modal="true" @mousedown.self="dismiss">
    <slot />
  </div>
</template>
