import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const css = readFileSync(`${process.cwd()}/src/assets/styles/motion.css`, 'utf8')
const tokens = readFileSync(`${process.cwd()}/src/assets/styles/tokens.css`, 'utf8')
const adminTemplate = readFileSync(`${process.cwd()}/src/views/admin/AdminWorkbenchView.vue`, 'utf8')
// G1：详情时间线已抽到 AdvisorStudentTimeline（弹窗/详情页复用），断言跟随新位置
const advisorTimelineTemplate = readFileSync(`${process.cwd()}/src/components/AdvisorStudentTimeline.vue`, 'utf8')

describe('动效样式约束', () => {
  it('提供统一的语义化 CQU 交互与状态令牌', () => {
    expect(tokens).toContain('--color-action-primary')
    expect(tokens).toContain('--color-status-warning')
    expect(tokens).toContain('--control-height-md')
  })

  it('减弱动态时取消位移、缩放、扫光、错峰和宽度生长', () => {
    expect(css).toContain('.page-enter-from, .page-leave-to')
    expect(css).toContain('transform: none !important')
    expect(css).toContain('.skeleton::after')
    expect(css).toContain('animation: none !important')
  })

  it('覆盖权重卡、详情时间线与表单焦点态', () => {
    expect(css).toContain('.weight-records > article')
    expect(css).toContain('.detail-timeline article')
    expect(css).toContain('input:focus-visible, select:focus-visible, textarea:focus-visible')
    expect(adminTemplate).toContain("(row,index) in rows")
    expect(advisorTimelineTemplate).toContain("(task,index) in tasks")
  })
})
