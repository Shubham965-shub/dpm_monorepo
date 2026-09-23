import { ChevronDown } from 'lucide-react'
import { GRAINS, type Grain } from '@/lib/kpiSchemas'
import { KPI_DOMAINS } from '@/lib/kpiConfig'
import type { SectionKey } from './mockData'

export type SectionFilterValue = 'all' | SectionKey

export interface DashboardFilters {
  section: SectionFilterValue
  grain: Grain
}

const GRAIN_LABEL: Record<Grain, string> = {
  daily: 'Daily',
  weekly: 'Weekly',
  monthly: 'Monthly',
  quarterly: 'Quarterly',
  yearly: 'Yearly',
}

/** Only 1 plant ever exists today (see DashboardPage's hardcoded site="KRSG" elsewhere) - stays a
 * fixed display rather than a dropdown with nothing to actually choose between. Section/Timeline
 * are real, since there are 4 real sections (KPI_DOMAINS) and 5 real grains (GRAINS) to pick from. */
function FixedPill({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center gap-1.5 rounded-lg border border-border bg-muted/50 px-3 py-1.5 text-sm">
      <span className="text-muted-foreground">{label}:</span>
      <span className="font-medium">{value}</span>
    </div>
  )
}

function FilterSelect<T extends string>({
  label,
  value,
  options,
  onChange,
}: {
  label: string
  value: T
  options: { value: T; label: string }[]
  onChange: (value: T) => void
}) {
  return (
    <div className="flex items-center gap-1.5 rounded-lg border border-border bg-muted/50 px-3 py-1.5 text-sm">
      <span className="text-muted-foreground">{label}:</span>
      {/* The chevron must sit inside the <select>'s own box (via this relative wrapper + absolute
      position + the select's pr-4 reserving that space) - laid out as a plain sibling instead, it
      visually sat next to the select but outside its clickable bounds, so clicking the arrow itself
      did nothing. */}
      <div className="relative flex items-center">
        <select
          value={value}
          onChange={(e) => onChange(e.target.value as T)}
          className="appearance-none bg-transparent pr-4 font-medium outline-none"
        >
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        <ChevronDown className="pointer-events-none absolute right-0 size-3.5 text-muted-foreground" />
      </div>
    </div>
  )
}

export function FilterBar({
  value,
  onChange,
}: {
  value: DashboardFilters
  onChange: (value: DashboardFilters) => void
}) {
  const sectionOptions = [
    { value: 'all' as const, label: 'All sections' },
    ...KPI_DOMAINS.map((d) => ({ value: d.key as SectionKey, label: d.label })),
  ]
  const grainOptions = GRAINS.map((g) => ({ value: g, label: GRAIN_LABEL[g] }))

  return (
    <div className="flex flex-wrap gap-3 px-6 py-3 md:px-10 lg:px-20">
      <FixedPill label="Plant" value="KRSG" />
      <FilterSelect
        label="Section"
        value={value.section}
        options={sectionOptions}
        onChange={(section) => onChange({ ...value, section })}
      />
      <FilterSelect
        label="Timeline"
        value={value.grain}
        options={grainOptions}
        onChange={(grain) => onChange({ ...value, grain })}
      />
    </div>
  )
}
