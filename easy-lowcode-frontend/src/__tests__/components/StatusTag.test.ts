import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusTag from '@/components/StatusTag.vue'

describe('StatusTag', () => {
  it('renders status value when no label is provided', () => {
    const wrapper = mount(StatusTag, {
      props: {
        status: 1,
      }
    })
    expect(wrapper.text()).toContain('1')
  })

  it('renders custom label when provided', () => {
    const wrapper = mount(StatusTag, {
      props: {
        status: 1,
        label: '启用'
      }
    })
    expect(wrapper.text()).toContain('启用')
  })

  it('renders unknown status as-is', () => {
    const wrapper = mount(StatusTag, {
      props: {
        status: 99,
      }
    })
    expect(wrapper.text()).toContain('99')
  })
})
