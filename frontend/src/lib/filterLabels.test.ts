import { describe, expect, it } from 'vitest'
import { countActiveFilters, defaultFilterSummaries, filterLabel, isFilterActive, summarizeFilter } from './filterLabels'
import type { ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import type { KpiDefinition } from './kpiConfig'

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

describe('isFilterActive / countActiveFilters', () => {
  const multi = { kind: 'multiSelect', param: 'sampleType' } as const
  const withDefault = { kind: 'select', param: 'finalClassification', required: true, defaultValue: 'Critical' } as const

  it('treats All and blank as not active, but a select\'s own default as active', () => {
    expect(isFilterActive(multi, { kind: 'multiSelect', value: [], allChecked: true })).toBe(false)
    expect(isFilterActive(multi, { kind: 'multiSelect', value: [], allChecked: false })).toBe(false)
    expect(isFilterActive(withDefault, { kind: 'select', value: 'Critical' })).toBe(true)
    expect(isFilterActive({ kind: 'select', param: 'testScope' }, { kind: 'select', value: '' })).toBe(false)
    expect(isFilterActive({ kind: 'numberInput', param: 'minAgingDays' }, { kind: 'numberInput', value: '' })).toBe(false)
  })

  it('counts every filter that narrows the data', () => {
    const filters = [multi, withDefault, { kind: 'numberInput', param: 'minAgingDays' } as const]
    const states: ExtraFilterState[] = [
      { kind: 'multiSelect', value: ['RM'], allChecked: false },
      { kind: 'select', value: 'Critical' },
      { kind: 'numberInput', value: '' },
    ]
    expect(countActiveFilters(filters, states)).toBe(2)
  })
})

describe('defaultFilterSummaries', () => {
  const kpi = (overrides: Partial<KpiDefinition>): KpiDefinition => ({ id: 'x', label: 'X', path: 'x', extraFilters: [], ...overrides })

  it('names only the filters that start on something other than All', () => {
    expect(defaultFilterSummaries(kpi({
      extraFilters: [
        { kind: 'multiSelect', param: 'sampleType' },
        { kind: 'select', param: 'finalClassification', required: true, defaultValue: 'Critical' },
      ],
    }))).toEqual(['Final classification: Critical'])
    expect(defaultFilterSummaries(kpi({ extraFilters: [{ kind: 'select', param: 'testScope' }] }))).toEqual([])
  })

  it('includes a card-level narrowing that no filter expresses', () => {
    expect(defaultFilterSummaries(kpi({ defaultNarrowing: 'Root cause: highest in latest period' })))
      .toEqual(['Root cause: highest in latest period'])
  })
})
