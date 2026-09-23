import type { KpiCardResponse } from './kpiSchemas'

const ROOT_CAUSE_NAME_PREFIX = 'Root Cause: '

// The backend gives each root-cause dimension its own distinct kpiId (e.g. "root-cause-by-
// instrument") and a "Root Cause: <Dimension>" name, one row per (period, dimension) - see
// QmsService.rootCauseShares/dimensionCounts.
export function extractRootCauseDimension(name: string): string {
  return name.startsWith(ROOT_CAUSE_NAME_PREFIX) ? name.slice(ROOT_CAUSE_NAME_PREFIX.length) : name
}

export interface RootCauseDimensionOption {
  kpiId: string
  dimension: string
}

/** Every distinct dimension present in the rows, alphabetical - used to populate the "other root
 * causes" dropdown regardless of which one is currently filtered to. */
export function listRootCauseDimensions(rows: KpiCardResponse[]): RootCauseDimensionOption[] {
  const byKpiId = new Map<string, RootCauseDimensionOption>()
  for (const row of rows) {
    if (!byKpiId.has(row.kpiId)) {
      byKpiId.set(row.kpiId, { kpiId: row.kpiId, dimension: extractRootCauseDimension(row.name) })
    }
  }
  return Array.from(byKpiId.values()).sort((a, b) => a.dimension.localeCompare(b.dimension))
}

/** The dimension with the highest count in the latest period present in the rows - per-product
 * decision, "maximal" means the current snapshot, not a whole-range total. Null if there are no
 * rows to compare at all. */
export function pickMaximalRootCauseDimension(rows: KpiCardResponse[]): string | null {
  if (rows.length === 0) return null
  const latestPeriod = rows.reduce((max, r) => (r.periodStart > max ? r.periodStart : max), rows[0].periodStart)
  let best: KpiCardResponse | null = null
  for (const row of rows) {
    if (row.periodStart !== latestPeriod) continue
    if (!best || (row.value ?? -Infinity) > (best.value ?? -Infinity)) best = row
  }
  return best ? best.kpiId : null
}

export function filterRootCauseRowsToDimension(rows: KpiCardResponse[], kpiId: string): KpiCardResponse[] {
  return rows.filter((row) => row.kpiId === kpiId)
}
