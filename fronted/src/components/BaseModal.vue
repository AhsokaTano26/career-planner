<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'

const props = withDefaults(defineProps<{
  /** When false, ESC 与点击遮罩都无法关闭弹窗(如强制改密、必须记录的初始密码)。 */
  closeable?: boolean
}>(), { closeable: true })

const emit = defineEmits<{ close: [] }>()

function dismiss() {
  if (props.closeable) emit('close')
}
function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') dismiss()
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="modal-mask" role="presentation" @mousedown.self="dismiss">
    <slot />
  </div>
</template>
