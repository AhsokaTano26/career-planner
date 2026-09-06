import type { EChartsCoreOption } from 'echarts/core'
import type { RadarSeriesOption } from 'echarts/charts'
import { THEME_TOKENS } from './theme-tokens'

/** 雷达维度（调用方把后端字段归一化为 label/value 后传入）。 */
export interface RadarDimension {
  label: string
  value: number
}

export interface RadarOptionInput {
  dimensions: RadarDimension[]
  /** 轴最大值：测评 5，画像 100。 */
  max: number
  /** 迷你模式（总览页）：隐藏提示框与动画，弱化轴线。 */
  mini?: boolean
  /** 是否开启动画（默认开；prefers-reduced-motion 时调用方传 false）。 */
  animate?: boolean
}

// 设计系统色板：唯一来源 THEME_TOKENS（跟随 cqu-theme.css :root 重大蓝），禁止 echarts 默认主题。
export const RADAR_INK = THEME_TOKENS.ink
const RADAR_MUTED = THEME_TOKENS.muted
const RADAR_LINE = THEME_TOKENS.line
const RADAR_SURFACE = THEME_TOKENS.surface
const RADAR_BLUE = THEME_TOKENS.blue
const RADAR_BLUE_DEEP = THEME_TOKENS.blueDeep
const RADAR_MONO = THEME_TOKENS.mono

/** 把任意输入钳到 [0, max]，非数值按 0 处理。 */
export function clampRadarValue(value: unknown, max: number): number {
  const n = Number(value)
  if (!Number.isFinite(n)) return 0
  return Math.min(Math.max(n, 0), max)
}

/**
 * 生成与「生涯同行」设计系统对齐的雷达图 option。
 *
 * HLTV 广播语言·重大蓝版（2026-09 用户裁决：去绿，要蓝色系）：
 * 标准蓝 3px 描边 + 同色辉光 + 14% 淡蓝填充 + 深海军蓝方点 +
 * 顶点 HUD 数字小牌（白底藏青边）+ 浅蓝灰实线网格 + 12px 等宽轴名。
 * 不使用渐变/圆角/彩虹色。
 */
export function buildRadarOption(input: RadarOptionInput): EChartsCoreOption {
  const max = input.max > 0 ? input.max : 5
  const mini = input.mini ?? false
  const animate = input.animate ?? true
  const values = input.dimensions.map((d) => clampRadarValue(d.value, max))
  const decimals = max <= 5 ? 1 : 0

  const series: RadarSeriesOption[] = [
    {
      type: 'radar',
      symbol: 'rect',
      symbolSize: mini ? 4 : 7,
      lineStyle: mini
        ? { color: RADAR_INK, width: 1.5 }
        : {
            color: RADAR_BLUE,
            width: 3,
            // 重大蓝辉光。
            shadowColor: 'rgba(0,75,157,0.45)',
            shadowBlur: 12,
          },
      itemStyle: { color: RADAR_BLUE_DEEP, borderColor: RADAR_BLUE_DEEP, borderWidth: 1.5 },
      areaStyle: mini ? { opacity: 0 } : { color: RADAR_BLUE, opacity: 0.14 },
      // HUD 数字：顶点直角小牌，撞车自动隐藏。
      label: mini
        ? { show: false }
        : {
            show: true,
            formatter: (p: any) => Number(p.value).toFixed(decimals),
            fontFamily: RADAR_MONO,
            fontSize: 11,
            fontWeight: 700,
            color: RADAR_INK,
            backgroundColor: RADAR_SURFACE,
            borderColor: RADAR_INK,
            borderWidth: 1,
            borderRadius: 0,
            padding: [1, 4],
          },
      labelLayout: mini ? undefined : { hideOverlap: true },
      emphasis: mini ? { disabled: true } : undefined,
      data: [{ value: values, name: '本人' }],
    },
  ]

  return {
    animation: animate && !mini,
    animationDuration: 1200,
    animationEasing: 'cubicOut',
    backgroundColor: 'transparent',
    tooltip: mini
      ? { show: false }
      : {
          trigger: 'item',
          backgroundColor: RADAR_SURFACE,
          borderColor: RADAR_INK,
          borderWidth: 1,
          borderRadius: 0,
          padding: [10, 12],
          textStyle: { color: RADAR_INK, fontFamily: RADAR_MONO, fontSize: 11 },
        },
    radar: {
      shape: 'polygon',
      splitNumber: 4,
      center: ['50%', '50%'],
      radius: '68%',
      axisName: {
        color: RADAR_INK,
        fontFamily: RADAR_MONO,
        fontSize: mini ? 9 : 12,
        fontWeight: 500,
      },
      axisNameGap: 14,
      // HUD 式密集网格：辐条淡去，实线浅灰同心环。
      axisLine: { lineStyle: { color: RADAR_LINE, width: 1 } },
      splitLine: { lineStyle: { color: RADAR_LINE, width: 1 } },
      splitArea: { show: false },
      indicator: input.dimensions.map((d) => ({ name: d.label, max })),
    },
    series,
    // 无障碍：与容器 aria-label 互补，读屏可播报。
    aria: { enabled: true },
  } as EChartsCoreOption
}

/** 生成「兴趣3.0、价值观4.0…」式读屏文本（容器 aria-label 用）。 */
export function radarAriaLabel(dimensions: RadarDimension[], max: number): string {
  if (!dimensions.length) return '暂无雷达数据'
  const parts = dimensions.map(
    (d) => `${d.label}${clampRadarValue(d.value, max)}分(满分${max}分)`,
  )
  return `六维雷达图：${parts.join('，')}`
}

export { RADAR_MUTED }
