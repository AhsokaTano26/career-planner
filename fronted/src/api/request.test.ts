import { describe, expect, it } from 'vitest'
import { toQuery } from './request'

describe('toQuery', () => {
  it('encodes defined query parameters and omits blank values', () => {
    expect(toQuery({ page: 1, keyword: '张 三', blank: '', none: undefined, nil: null }))
      .toBe('page=1&keyword=%E5%BC%A0+%E4%B8%89')
  })
})
