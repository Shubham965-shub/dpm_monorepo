import { useState } from 'react'
import type { Grain } from '@/lib/kpiSchemas'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { defaultGrainFor } from '@/lib/kpiDefaultGrain'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import type { GlobalKpiFilters } from './useKpiData'

/**
 * A card's own D/W/M/Q/Y pick (GrainToggle) and the filters its fetch should use. Defaults to the
 * KPI Framework's per-role grain (kpiDefaultGrain.ts), else the page's Timeline grain. Stores only
 * the pick, tagged with the default it was made under - so the card follows that default until
 * picked, and a later change to it (Timeline filter, or lens) drops a stale pick.
 */
export function useCardGrain(kpiId: string, lens: KpiVisibilityLens, globalFilters: GlobalKpiFilters) {
  const defaultGrain = defaultGrainFor(kpiId, lens) ?? (globalFilters.grain as Grain)
  const [grainPick, setGrainPick] = useState<{ baseGrain: Grain; grain: Grain } | null>(null)
  const grain = grainPick?.baseGrain === defaultGrain ? grainPick.grain : defaultGrain
  const setGrain = (next: Grain) => setGrainPick({ baseGrain: defaultGrain, grain: next })
  const cardFilters: GlobalKpiFilters = { ...globalFilters, ...getGrainDateRange(grain), grain }
  return { grain, setGrain, cardFilters }
}
