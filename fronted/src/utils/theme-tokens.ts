/**
 * 主题 Token 单源（TS 侧）。
 *
 * 唯一来源：`src/assets/styles/cqu-theme.css` 的 `:root`（后加载覆盖 styles.css，
 * 为最终生效值；重庆大学蓝）。
 * 改色只改 cqu-theme.css，这里跟随；回归测试会逐项比对两边（见 radarOption.test.ts），
 * 防止手抄 hex 漂移（ECharts 社区称之为 theme drift）。
 */
export const THEME_TOKENS = {
  paper: '#f5f8fc',
  surface: '#ffffff',
  ink: '#172b45',
  muted: '#607189',
  line: '#c9d8e8',
  blue: '#004b9d',
  blueDeep: '#003b7b',
  blue80: '#336fae',
  blue60: '#6694c0',
  blue20: '#d9e7f4',
  orange: '#ff5c35',
  black: '#003b7b',
  sans: '"Noto Sans SC",Arial,sans-serif',
  mono: '"IBM Plex Mono",ui-monospace,monospace',
} as const

export type ThemeTokens = typeof THEME_TOKENS
