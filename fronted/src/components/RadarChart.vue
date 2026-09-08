<template>
  <div class="radar-box" :class="{ mini: mini }">
    <div
      v-if="!loadFailed"
      ref="host"
      class="radar-host"
      :style="{ height: height + 'px' }"
      tabindex="0"
      role="img"
      :aria-label="ariaLabel"
    />
    <!-- echarts 加载失败降级：数字表兜底（与下方 metric-grid 同口径，不播报虚假图形） -->
    <div v-else class="radar-fallback" role="img" aria-label="图表加载失败，数值如下">
      <div v-for="(d, i) in dimensions" :key="i" class="radar-fallback-row">
        <span class="radar-fallback-name">{{ d.label }}</span>
        <span class="radar-fallback-value">{{ d.value }} / {{ max }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { ECharts } from 'echarts/core'
import { buildRadarOption, radarAriaLabel, type RadarDimension } from '../utils/radarOption'
import { useReducedMotion } from '../composables/useReducedMotion'

// echarts 按需加载：CanvasRenderer + RadarChart + TooltipComponent 拆到独立 chunk，
// 主包不引入整包与默认主题；类型为纯 type-import，运行时零开销。

const props = withDefaults(
  defineProps<{
    dimensions: RadarDimension[]
    max: number
    mini?: boolean
    height?: number
  }>(),
  { mini: false, height: 320 },
)

const host = ref<HTMLElement | null>(null)
const reducedMotion = useReducedMotion()
// echarts 动态加载失败标记（v-if 切换宿主，数字表兜底）
const loadFailed = ref(false)
let chart: ECharts | null = null
let observer: ResizeObserver | null = null

const ariaLabel = computed(() => radarAriaLabel(props.dimensions, props.max))

function render() {
  if (!chart) return
  chart.setOption(
    buildRadarOption({
      dimensions: props.dimensions,
      max: props.max,
      mini: props.mini,
      animate: !reducedMotion.value,
    }),
    { notMerge: true },
  )
}

onMounted(() => {
  // 复审 Batch4：动态 import 失败降级为数字表（metric-grid 承担数字兜底），不再永久空白
  // （此前无 catch，弱网/拆包失败即空白雷达盒，读屏却播报有数据）。
  Promise.all([import('echarts/core'), import('echarts/renderers'), import('echarts/charts'), import('echarts/components')]).then(
    ([{ init, use }, { CanvasRenderer }, { RadarChart: EChartsRadar }, { TooltipComponent }]) => {
      use([CanvasRenderer, EChartsRadar, TooltipComponent])
      if (!host.value) return
      chart = init(host.value)
      render()
      observer = new ResizeObserver(() => chart?.resize())
      if (host.value) observer.observe(host.value)
    },
  ).catch(() => {
    loadFailed.value = true
  })
})

watch(
  [() => props.dimensions, () => props.max, () => props.mini, reducedMotion],
  render,
  { deep: true },
)

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
/* 雷达居中陈列：大尺寸单图 + 下方数字表，不再并排挤占。 */
.radar-box {
  width: min(560px, 100%);
  margin: 12px auto 0;
  flex: 0 0 auto;
}
.radar-box.mini {
  width: 220px;
  max-width: 100%;
  margin-top: 0;
}
.radar-host {
  width: 100%;
  outline-offset: 2px;
}
.radar-host:focus-visible {
  outline: 1px solid var(--ink);
}
@media (max-width: 640px) {
  .radar-box {
    width: 100%;
    max-width: 100%;
  }
}
</style>
