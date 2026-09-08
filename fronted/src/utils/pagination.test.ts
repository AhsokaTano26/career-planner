import { describe, expect, it } from 'vitest'
import { resolvePage } from './pagination'

describe('resolvePage', () => {
  it('正常页直接采用服务端页码', () => {
    expect(resolvePage(2, 2, 5)).toEqual({ page: 2, refetch: false })
  })
  it('旧大页码回落末页并重拉', () => {
    expect(resolvePage(9, 9, 3)).toEqual({ page: 3, refetch: true })
  })
  it('第一页不重拉（避免循环）', () => {
    expect(resolvePage(1, 1, 1)).toEqual({ page: 1, refetch: false })
  })
  it('非法值兜底到合法区间', () => {
    expect(resolvePage(0, 0, 0)).toEqual({ page: 1, refetch: false })
  })
})
