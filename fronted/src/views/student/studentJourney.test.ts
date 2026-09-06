import { describe, expect, it } from 'vitest'
import { nextStudentAction } from './studentJourney'

describe('nextStudentAction', () => {
  it('sends students with incomplete profile fields to the profile editor', () => {
    expect(nextStudentAction([{ key: 'interest', name: '兴趣偏好' }])).toEqual({
      title: '下一步：补充档案',
      description: '仍缺少：兴趣偏好',
      label: '维护个人资料',
      path: '/student/profile',
    })
  })

  it('sends students with a complete profile to record their experiences', () => {
    expect(nextStudentAction([])).toEqual({
      title: '档案信息已完善',
      description: '补充个人经历，让后续辅导拥有更完整的参考信息。',
      label: '维护个人经历',
      path: '/student/experiences',
    })
  })
})
