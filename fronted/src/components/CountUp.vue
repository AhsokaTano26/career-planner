<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { animate } from 'motion-v'

const props = withDefaults(defineProps<{ to: number; duration?: number }>(), { duration: 0.8 })
const EASE: [number, number, number, number] = [0.16, 1, 0.3, 1]
const display = ref(0)
let controls: { stop: () => void } | undefined

function run() {
  controls?.stop()
  const target = Number(props.to)
  if (!Number.isFinite(target)) { display.value = 0; return }
  controls = animate(display.value, target, {
    duration: props.duration,
    ease: EASE,
    onUpdate: (value: number) => { display.value = Math.round(value) },
  })
}
watch(() => props.to, run, { immediate: true })
onBeforeUnmount(() => controls?.stop())
</script>

<template>
  <span>{{ display }}</span>
</template>
