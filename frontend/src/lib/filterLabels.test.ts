import { describe, expect, it } from 'vitest'
import { filterLabel, summarizeFilter } from './filterLabels'

describe('filterLabel', () => {
  it('turns a camelCase param into a sentence-case label', () => {
    expect(filterLabel('sampleType')).toBe('Sample type')
    expect(filterLabel('minAgingDays')).toBe('Min aging days')
    expect(filterLabel('site')).toBe('Site')
  })

  it('overrides a param whose segments are abbreviations the regex split would read literally', () => {
    expect(filterLabel('respDept')).toBe('Responsible Department')
    expect(filterLabel('instGroup')).toBe('Instrument Group')
  })
})

describe('summarizeFilter', () => {
  const multi = { kind: 'multiSelect', param: 'sampleType' } as const

  it('summarizes a multiSelect as All, the single value, or a count', () => {
    expect(summarizeFilter(multi, { kind: 'multiSelect', value: [], allChecked: true })).toBe('All')
    expect(summarizeFilter(multi, { kind: 'multiSelect', value: ['RM'], allChecked: false })).toBe('RM')
    expect(summarizeFilter(multi, { kind: 'multiSelect', value: ['RM', 'IP'], allChecked: false })).toBe('2 selected')
  })

  it('asks for a choice on an unset required select, and shows All on an optional one', () => {
    expect(summarizeFilter({ kind: 'select', param: 'x', required: true }, { kind: 'select', value: '' })).toBe('Choose…')
    expect(summarizeFilter({ kind: 'select', param: 'x' }, { kind: 'select', value: '' })).toBe('All')
    expect(summarizeFilter({ kind: 'select', param: 'x' }, { kind: 'select', value: 'KRSG' })).toBe('KRSG')
  })

  it('shows Any for an empty number input', () => {
    expect(summarizeFilter({ kind: 'numberInput', param: 'minAgingDays' }, { kind: 'numberInput', value: '' })).toBe('Any')
    expect(summarizeFilter({ kind: 'numberInput', param: 'minAgingDays' }, { kind: 'numberInput', value: '30' })).toBe('30')
  })
})
