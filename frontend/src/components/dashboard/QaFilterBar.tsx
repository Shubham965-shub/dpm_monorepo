import type { KpiDomain } from '@/lib/kpiConfig'
import { DEFAULT_TIMELINE, FilterSelect, FixedPill, ResetFiltersButton, TIMELINE_OPTIONS, type Timeline } from './FilterBar'

export type QaTimeline = Timeline

export const DEFAULT_QA_TIMELINE = DEFAULT_TIMELINE

export type QaSectionFilter = 'all' | string

export const DEFAULT_QA_SECTION: QaSectionFilter = 'all'

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
