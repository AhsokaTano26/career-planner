import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BaseModal from './BaseModal.vue'

describe('BaseModal', () => {
  it('offers a labelled close control when the modal can be dismissed', async () => {
    const wrapper = mount(BaseModal, { slots: { default: '<section class="modal-card"><h2>编辑资料</h2></section>' } })
    await wrapper.get('[data-testid="dialog-close"]').trigger('click')
    expect(wrapper.emitted('close')).toHaveLength(1)
  })
})
