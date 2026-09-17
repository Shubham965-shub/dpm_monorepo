import type { ChangeEvent } from 'react'
import type { ExtraFilter } from '@/lib/kpiConfig'
import type { ExtraParams } from '@/hooks/useKpiData'
import { CAPA_LEVEL_OPTIONS, CAPA_RECORD_STATUS_OPTIONS } from '@/lib/kpiConfig'

export type ExtraFilterState =
  | { kind: 'none' }
  | { kind: 'select'; value: string }
  | { kind: 'multiSelect'; value: string[] }
  | { kind: 'repeatedText'; value: string }
  | { kind: 'capaAging'; recordStatus: string[]; level: string[]; minAgingDays: string; maxAgingDays: string }

export function initialExtraFilterState(filter: ExtraFilter): ExtraFilterState {
  switch (filter.kind) {
    case 'none':
      return { kind: 'none' }
    case 'select':
      return { kind: 'select', value: filter.required ? (filter.options[0] ?? '') : '' }
    case 'multiSelect':
      return { kind: 'multiSelect', value: [] }
    case 'repeatedText':
      return { kind: 'repeatedText', value: '' }
    case 'capaAging':
      return { kind: 'capaAging', recordStatus: [], level: [], minAgingDays: '', maxAgingDays: '' }
  }
}

export function resolveExtraParams(filter: ExtraFilter, state: ExtraFilterState): ExtraParams {
  if (filter.kind === 'none' || state.kind === 'none') return []

  if (filter.kind === 'select' && state.kind === 'select') {
    return state.value ? [[filter.param, state.value]] : []
  }
  if (filter.kind === 'multiSelect' && state.kind === 'multiSelect') {
    return state.value.length > 0 ? [[filter.param, state.value.join(',')]] : []
  }
  if (filter.kind === 'repeatedText' && state.kind === 'repeatedText') {
    const values = state.value
      .split(',')
      .map((v) => v.trim())
      .filter((v) => v.length > 0)
    return values.map((v) => [filter.param, v] as [string, string])
  }
  if (filter.kind === 'capaAging' && state.kind === 'capaAging') {
    const params: ExtraParams = []
    if (state.recordStatus.length > 0) params.push(['recordStatus', state.recordStatus.join(',')])
    if (state.level.length > 0) params.push(['level', state.level.join(',')])
    if (state.minAgingDays !== '') params.push(['minAgingDays', state.minAgingDays])
    if (state.maxAgingDays !== '') params.push(['maxAgingDays', state.maxAgingDays])
    return params
  }
  return []
}

function selectedOptions(e: ChangeEvent<HTMLSelectElement>): string[] {
  return Array.from(e.target.selectedOptions).map((o) => o.value)
}

const selectClass =
  'w-full rounded-md border border-input bg-background px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-ring'
const labelClass = 'text-muted-foreground text-[11px] font-medium'

export function ExtraFilterControl({
  filter,
  state,
  onChange,
}: {
  filter: ExtraFilter
  state: ExtraFilterState
  onChange: (state: ExtraFilterState) => void
}) {
  if (filter.kind === 'none' || state.kind === 'none') return null

  if (filter.kind === 'select' && state.kind === 'select') {
    return (
      <label className="flex flex-col gap-1">
        <span className={labelClass}>{filter.param}{filter.required ? ' *' : ''}</span>
        <select className={selectClass} value={state.value} onChange={(e) => onChange({ kind: 'select', value: e.target.value })}>
          {!filter.required && <option value="">All</option>}
          {filter.options.map((o) => (
            <option key={o} value={o}>{o}</option>
          ))}
        </select>
      </label>
    )
  }

  if (filter.kind === 'multiSelect' && state.kind === 'multiSelect') {
    return (
      <label className="flex flex-col gap-1">
        <span className={labelClass}>{filter.param} (all if none selected)</span>
        {filter.options.length === 0 ? (
          <span className="text-muted-foreground text-xs italic">no values in DB yet</span>
        ) : (
          <select
            className={`${selectClass} h-20`}
            multiple
            value={state.value}
            onChange={(e) => onChange({ kind: 'multiSelect', value: selectedOptions(e) })}
          >
            {filter.options.map((o) => (
              <option key={o} value={o}>{o}</option>
            ))}
          </select>
        )}
      </label>
    )
  }

  if (filter.kind === 'repeatedText' && state.kind === 'repeatedText') {
    return (
      <label className="flex flex-col gap-1">
        <span className={labelClass}>{filter.param} (comma-separated, 1478 possible values)</span>
        <input
          className={selectClass}
          placeholder="e.g. Acetazolamide Tablets USP 250mg"
          value={state.value}
          onChange={(e) => onChange({ kind: 'repeatedText', value: e.target.value })}
        />
      </label>
    )
  }

  if (filter.kind === 'capaAging' && state.kind === 'capaAging') {
    return (
      <div className="flex flex-wrap gap-3">
        <label className="flex flex-col gap-1">
          <span className={labelClass}>recordStatus (all if none)</span>
          <select
            className={`${selectClass} h-20`}
            multiple
            value={state.recordStatus}
            onChange={(e) => onChange({ ...state, recordStatus: selectedOptions(e) })}
          >
            {CAPA_RECORD_STATUS_OPTIONS.map((o) => (
              <option key={o} value={o}>{o}</option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className={labelClass}>level (all if none)</span>
          <select
            className={`${selectClass} h-20`}
            multiple
            value={state.level}
            onChange={(e) => onChange({ ...state, level: selectedOptions(e) })}
          >
            {CAPA_LEVEL_OPTIONS.map((o) => (
              <option key={o} value={o}>{o}</option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className={labelClass}>minAgingDays</span>
          <input
            type="number"
            className={`${selectClass} w-24`}
            value={state.minAgingDays}
            onChange={(e) => onChange({ ...state, minAgingDays: e.target.value })}
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className={labelClass}>maxAgingDays</span>
          <input
            type="number"
            className={`${selectClass} w-24`}
            value={state.maxAgingDays}
            onChange={(e) => onChange({ ...state, maxAgingDays: e.target.value })}
          />
        </label>
      </div>
    )
  }

  return null
}
