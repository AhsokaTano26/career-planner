import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { buildRadarOption, clampRadarValue, radarAriaLabel } from './radarOption'
import { THEME_TOKENS } from './theme-tokens'

const DIMS = [
  { label: '兴趣', value: 3 },
  { label: '价值观', value: 4 },
  { label: '能力', value: 5 },
  { label: '学业', value: 3 },
  { label: '倾向', value: 4 },
  { label: '实践', value: 5 },
]

describe('clampRadarValue', () => {
  it('钳制到 [0, max]', () => {
    expect(clampRadarValue(88, 5)).toBe(5)
    expect(clampRadarValue(-2, 5)).toBe(0)
    expect(clampRadarValue(3.2, 5)).toBe(3.2)
  })
  it('非数值按 0 处理', () => {
    expect(clampRadarValue(undefined, 5)).toBe(0)
    expect(clampRadarValue('abc', 5)).toBe(0)
  })
})

describe('buildRadarOption', () => {
  it('测评口径：6 轴 max=5，HLTV 重大蓝版', () => {
    const opt = buildRadarOption({ dimensions: DIMS, max: 5 }) as any
    expect(opt.radar.indicator).toHaveLength(6)
    expect(opt.radar.indicator[0]).toEqual({ name: '兴趣', max: 5 })
    expect(opt.series).toHaveLength(1)
    expect(opt.series[0].data[0].value).toEqual([3, 4, 5, 3, 4, 5])
    // 重大蓝：标准蓝描边 + 辉光 + 淡填充；深海军蓝方点；无图例无渐变
    expect(opt.series[0].lineStyle).toMatchObject({ color: '#004b9d', width: 3 })
    expect(opt.series[0].lineStyle.shadowBlur).toBeGreaterThan(0)
    expect(opt.series[0].symbol).toBe('rect')
    expect(opt.series[0].itemStyle).toMatchObject({ color: '#003b7b' })
    expect(opt.series[0].areaStyle.opacity).toBeLessThanOrEqual(0.2)
    // HUD 数字：顶点直角小牌
    expect(opt.series[0].label.show).toBe(true)
    expect(opt.series[0].label.borderRadius).toBe(0)
    expect(opt.legend).toBeUndefined()
    expect(opt.radar.splitArea).toEqual({ show: false })
    expect(opt.tooltip.borderRadius).toBe(0)
    // 轴名 12px 等宽藏青
    expect(opt.radar.axisName).toMatchObject({ fontSize: 12, color: '#172b45' })
  })
  it('画像口径：max=100 超限值被钳制', () => {
    const opt = buildRadarOption({ dimensions: [{ label: '能力', value: 120 }], max: 100 }) as any
    expect(opt.series[0].data[0].value).toEqual([100])
  })
  it('迷你模式：关提示框与动画', () => {
    const opt = buildRadarOption({ dimensions: DIMS, max: 100, mini: true }) as any
    expect(opt.tooltip).toEqual({ show: false })
    expect(opt.animation).toBe(false)
  })
  it('无障碍模式可关动画', () => {
    const opt = buildRadarOption({ dimensions: DIMS, max: 5, animate: false }) as any
    expect(opt.animation).toBe(false)
  })
})

describe('radarAriaLabel', () => {
  it('生成中文读屏文本', () => {
    expect(radarAriaLabel(DIMS.slice(0, 2), 5)).toBe('六维雷达图：兴趣3分(满分5分)，价值观4分(满分5分)')
  })
  it('空数据兜底', () => {
    expect(radarAriaLabel([], 5)).toBe('暂无雷达数据')
  })
})

describe('theme tokens 回归：TS 侧必须与 cqu-theme.css :root（最终生效值）一致', () => {
  const css = readFileSync('src/assets/styles/cqu-theme.css', 'utf8')
  const varOf = (name: string) => {
    const m = css.match(new RegExp(`--${name}\\s*:\\s*([^;}]+)`))
    return m ? m[1].trim() : null
  }
  const cases: Array<[keyof typeof THEME_TOKENS, string]> = [
    ['paper', 'paper'],
    ['surface', 'surface'],
    ['ink', 'ink'],
    ['muted', 'muted'],
    ['line', 'line'],
    ['blue', 'cqu-blue'],
    ['blueDeep', 'cqu-blue-deep'],
  ]
  for (const [key, cssVar] of cases) {
    it(`--${cssVar} == THEME_TOKENS.${key}`, () => {
      expect(varOf(cssVar)?.toLowerCase()).toBe(String(THEME_TOKENS[key]).toLowerCase())
    })
  }
})
