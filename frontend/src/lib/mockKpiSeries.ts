import type { KpiDomain, KpiDefinition } from './kpiConfig'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import type { KpiCardResponse, Grain } from './kpiSchemas'
import { toIsoDate } from './kpiDateRange'

// A tiny deterministic PRNG (mulberry32) seeded from a string hash - no new dependency, and unlike
// Math.random() the same (domain, kpi, period) always renders the same value, so a demo dashboard
// doesn't visibly jitter on every refetch/re-render and its tests are reproducible. Exported for
// reuse by qaSectionScore.ts's mock section scores - same determinism rationale, different seed.
export function hashString(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0
  return h >>> 0
}

export function mulberry32(seed: number): () => number {
  let a = seed
  return () => {
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

function stepDate(d: Date, grain: Grain): void {
  switch (grain) {
    case 'daily':
      d.setDate(d.getDate() + 1)
      break
    case 'weekly':
      d.setDate(d.getDate() + 7)
      break
    case 'monthly':
      d.setMonth(d.getMonth() + 1)
      break
    case 'quarterly':
      d.setMonth(d.getMonth() + 3)
      break
    case 'yearly':
      d.setFullYear(d.getFullYear() + 1)
      break
  }
}

/** Every periodStart from `from` to `to` (inclusive), one per `grain` step. */
function periodStarts(from: string, to: string, grain: Grain): string[] {
  const end = new Date(`${to}T00:00:00`)
  const cur = new Date(`${from}T00:00:00`)
  const starts: string[] = []
  // Guard against an unexpected inverted/zero range looping forever - a demo series has no reason
  // to ever need more than a few hundred points.
  const MAX_PERIODS = 500
  while (cur <= end && starts.length < MAX_PERIODS) {
    starts.push(toIsoDate(cur))
    stepDate(cur, grain)
  }
  return starts
}

/** A plausible unit + value range from the KPI's own id - most of this dashboard's KPIs are rates/
 * adherence percentages, a few are cycle-time/aging durations, the rest plain counts. Approximate
 * on purpose: there's no real column/scale to read this off (see mockKpiSeries.ts's caller
 * doc) - good enough for a demo card's axis to look sensible, not a claim about the real KPI. */
function unitAndRangeFor(kpi: KpiDefinition): { unit: string; min: number; max: number } {
  const id = kpi.id
  // Unlike every other *-tat KPI here, Line Clearance TAT is logged in minutes, not days (see its
  // qa_kpi_info DB text) - a per-id carve-out before the generic "tat" match below, which would
  // otherwise mislabel it the same as day-denominated cycle times.
  if (id === 'qa-line-clearance-tat') return { unit: 'minutes', min: 10, max: 30 }
  if (/time|tat|aging|cycle/.test(id)) return { unit: 'days', min: 0, max: 20 }
  if (/index|density|count|logged|complaints/.test(id) && !/rate/.test(id)) return { unit: 'count', min: 0, max: 40 }
  return { unit: '%', min: 0, max: 100 }
}

/**
 * Synthetic `KpiCardResponse[]` for a `KpiDomain` with `mock: true` (see useKpiData.ts), standing
 * in for a real backend fetch until this domain has a real data pipeline. Deterministic per
 * (domain, kpi, period) - not a real metric, so `status` is always null (no RAG judgement - see
 * QaDashboardPage's demo-data banner), matching the existing "no threshold configured yet"
 * treatment real QC KPIs already get when their catalogue threshold doesn't apply (see
 * backend/CLAUDE.md's KPI threshold guidance) - no new UI concept needed for a demo card.
 */
export function generateMockKpiSeries(domain: KpiDomain, kpi: KpiDefinition, globalFilters: GlobalKpiFilters): KpiCardResponse[] {
  const rand = mulberry32(hashString(`${domain.key}:${kpi.id}`))
  const { unit, min, max } = unitAndRangeFor(kpi)
  const grain = globalFilters.grain as Grain
  const periods = kpi.snapshot ? [globalFilters.to] : periodStarts(globalFilters.from, globalFilters.to, grain)

  return periods.map((periodStart) => ({
    kpiId: kpi.id,
    name: kpi.label,
    value: Math.round((min + rand() * (max - min)) * 10) / 10,
    unit,
    status: null,
    grain,
    periodStart,
    note: null,
    numerator: null,
    denominator: null,
  }))
}
