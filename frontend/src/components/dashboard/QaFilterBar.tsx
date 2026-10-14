import { GRAINS, type Grain } from '@/lib/kpiSchemas'
import type { KpiDomain } from '@/lib/kpiConfig'
import { FilterSelect, FixedPill, ResetFiltersButton } from './FilterBar'

/** 'default' keeps every KPI on its own per-lens cadence (see qaDefaultGrain.ts); a real grain
 * overrides all of them at once. */
export type QaTimeline = 'default' | Grain

export const DEFAULT_QA_TIMELINE = 'default' satisfies QaTimeline

export type QaSectionFilter = 'all' | string

export const DEFAULT_QA_SECTION: QaSectionFilter = 'all'

const TIMELINE_LABEL: Record<QaTimeline, string> = {
  default: 'Default (per KPI)',
  daily: 'Daily',
  weekly: 'Weekly',
  monthly: 'Monthly',
  quarterly: 'Quarterly',
  yearly: 'Yearly',
}

const TIMELINE_OPTIONS = (['default', ...GRAINS] as QaTimeline[]).map((value) => ({ value, label: TIMELINE_LABEL[value] }))

export function QaFilterBar({
  site,
  domains,
  section,
  onSectionChange,
  timeline,
  onTimelineChange,
}: {
  site: string
  domains: KpiDomain[]
  section: QaSectionFilter
  onSectionChange: (value: QaSectionFilter) => void
  timeline: QaTimeline
  onTimelineChange: (value: QaTimeline) => void
}) {
  const sectionOptions = [{ value: 'all', label: 'All sections' }, ...domains.map((d) => ({ value: d.key, label: d.label }))]
  const filtered = section !== DEFAULT_QA_SECTION || timeline !== DEFAULT_QA_TIMELINE
  return (
    <div className="flex flex-wrap gap-3">
      <FixedPill label="Plant" value={site} />
      <FilterSelect label="Section" value={section} defaultValue={DEFAULT_QA_SECTION} options={sectionOptions} onChange={onSectionChange} />
      <FilterSelect label="Timeline" value={timeline} defaultValue={DEFAULT_QA_TIMELINE} options={TIMELINE_OPTIONS} onChange={onTimelineChange} />
      {filtered && (
        <ResetFiltersButton
          onClick={() => {
            onSectionChange(DEFAULT_QA_SECTION)
            onTimelineChange(DEFAULT_QA_TIMELINE)
          }}
        />
      )}
    </div>
  )
}
