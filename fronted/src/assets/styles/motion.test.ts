import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const css = readFileSync(`${process.cwd()}/src/assets/styles/motion.css`, 'utf8')
const adminTemplate = readFileSync(`${process.cwd()}/src/views/admin/AdminWorkbenchView.vue`, 'utf8')
const advisorDialogTemplate = readFileSync(`${process.cwd()}/src/components/AdvisorStudentDialog.vue`, 'utf8')

describe('动效样式约束', () => {
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
    expect(advisorDialogTemplate).toContain("(task,index) in tasks")
  })
})
