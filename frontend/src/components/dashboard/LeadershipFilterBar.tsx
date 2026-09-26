import { FilterSelect } from './FilterBar'
import { DEFAULT_SITE } from '@/lib/kpiConfig'

/** Leadership's reference screenshot shows only a Plant + Timeline pill (no Section filter) - a
 * trailing-day-count window ("T-30") rather than the shared FilterBar's aggregation-grain Timeline
 * (Daily/Weekly/.../Yearly), since only one plant/day-count is ever shown, not an aggregation
 * period. */
export const LEADERSHIP_TRAILING_DAY_OPTIONS = [7, 30, 90, 180] as const
export type LeadershipTrailingDays = (typeof LEADERSHIP_TRAILING_DAY_OPTIONS)[number]

export function LeadershipFilterBar({
  trailingDays,
  onChange,
}: {
  trailingDays: LeadershipTrailingDays
  onChange: (days: LeadershipTrailingDays) => void
}) {
  const dayOptions = LEADERSHIP_TRAILING_DAY_OPTIONS.map((d) => ({ value: String(d), label: `T-${d}` }))

  return (
    <div className="flex flex-wrap gap-3 px-6 py-3 md:px-10 lg:px-20">
      {/* Only 1 plant ever exists today, same as FilterBar's own Plant pill - a real FilterSelect
      here (not a FixedPill) only because the reference screenshot draws Plant with a dropdown
      chevron for Leadership specifically. */}
      <FilterSelect label="Plant" value={DEFAULT_SITE} options={[{ value: DEFAULT_SITE, label: DEFAULT_SITE }]} onChange={() => {}} />
      <FilterSelect
        label="Timeline"
        value={String(trailingDays)}
        options={dayOptions}
        onChange={(v) => onChange(Number(v) as LeadershipTrailingDays)}
      />
    </div>
  )
}
