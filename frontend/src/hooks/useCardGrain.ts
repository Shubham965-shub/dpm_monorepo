import { useState } from 'react'
import type { Grain } from '@/lib/kpiSchemas'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { defaultGrainFor } from '@/lib/kpiDefaultGrain'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import type { GlobalKpiFilters } from './useKpiData'

export interface CardDateRange {
  from: string
  to: string
}

/**
 * A card's own D/W/M/Q/Y pick (GrainToggle) and the filters its fetch should use. Defaults to the
 * KPI Framework's per-role grain (kpiDefaultGrain.ts), else the page's Timeline grain. Stores only
 * the pick, tagged with the default it was made under - so the card follows that default until
 * picked, and a later change to it (Timeline filter, or lens) drops a stale pick.
 *
 * Also owns an optional manually-picked period range (see PeriodRangePicker, shown in
 * KpiDetailModal) - tagged with the grain it was picked under by the same rule, so switching grain
 * drops a stale range instead of reinterpreting (say) a daily range under monthly grain.
 */
export function useCardGrain(kpiId: string, lens: KpiVisibilityLens, globalFilters: GlobalKpiFilters) {
  const defaultGrain = defaultGrainFor(kpiId, lens) ?? (globalFilters.grain as Grain)
  const [grainPick, setGrainPick] = useState<{ baseGrain: Grain; grain: Grain } | null>(null)
  const grain = grainPick?.baseGrain === defaultGrain ? grainPick.grain : defaultGrain

  const [rangePick, setRangePick] = useState<{ grain: Grain; range: CardDateRange } | null>(null)
  const dateRange = rangePick?.grain === grain ? rangePick.range : null

  const setGrain = (next: Grain) => {
    setGrainPick({ baseGrain: defaultGrain, grain: next })
    // A grain switch invalidates any manually-picked range - it was chosen against the old grain's
    // period boundaries (e.g. a Q3 pick means nothing once the toggle moves to daily).
    setRangePick(null)
  }
  const setDateRange = (range: CardDateRange) => setRangePick({ grain, range })
  const clearDateRange = () => setRangePick(null)

  const cardFilters: GlobalKpiFilters = { ...globalFilters, ...(dateRange ?? getGrainDateRange(grain)), grain }
  return { grain, setGrain, cardFilters, dateRange, setDateRange, clearDateRange }
}
