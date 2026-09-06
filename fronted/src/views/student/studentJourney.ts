type MissingField = { key: string; name: string }

export type StudentNextAction = {
  title: string
  description: string
  label: string
  path: '/student/profile' | '/student/experiences'
}

export function nextStudentAction(missing: MissingField[] = []): StudentNextAction {
  if (missing.length > 0) {
    return {
      title: '下一步：补充档案',
      description: `仍缺少：${missing.map((item) => item.name).join('、')}`,
      label: '维护个人资料',
      path: '/student/profile',
    }
  }

  return {
    title: '档案信息已完善',
    description: '补充个人经历，让后续辅导拥有更完整的参考信息。',
    label: '维护个人经历',
    path: '/student/experiences',
  }
}
