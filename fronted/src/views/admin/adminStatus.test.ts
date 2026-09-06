import { describe, expect, it } from 'vitest'
import { adminStatusPresentation } from './adminStatus'

describe('adminStatusPresentation', () => {
  it.each([
    ['PUBLISHED', { label: '已发布', tone: 'success' }],
    ['PROCESSING', { label: '处理中', tone: 'warning' }],
    ['FAILED', { label: '失败', tone: 'danger' }],
    ['DISABLED', { label: '已停用', tone: 'neutral' }],
  ] as const)('gives %s a readable label and non-color-only tone', (status, expected) => {
    expect(adminStatusPresentation(status)).toEqual(expected)
  })
})
