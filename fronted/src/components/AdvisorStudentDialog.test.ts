import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AdvisorStudentDialog from './AdvisorStudentDialog.vue'
import type { AdvisorDetail } from '../types/domain'

const student: AdvisorDetail = {
  id: '20260001',
  detail: { profile: { name: '李同学', className: '计科 2401' } },
  guidance: [],
}

describe('AdvisorStudentDialog', () => {
  it('changes the drafting guidance when an advisor selects a task suggestion', async () => {
    const wrapper = mount(AdvisorStudentDialog, { props: { student, saving: false } })

    expect(wrapper.get('[data-testid="advisor-guidance-summary"]').text()).toContain('请填写学生当前最需要推进的行动。')
    await wrapper.get('select').setValue('SUGGEST_TASK')
    expect(wrapper.get('[data-testid="advisor-guidance-summary"]').text()).toContain('请把建议任务写成可执行的事项。')
  })
})
