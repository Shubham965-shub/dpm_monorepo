import type { KpiCardResponse } from './kpiSchemas'
import { extractRootCauseDimension } from './rootCauseDimensions'

export interface HeatmapDimensionRow {
  kpiId: string
  label: string
  valuesByPeriod: Map<string, number>
}

/** Root Cause (Consolidated)'s raw (period, dimension) rows reshaped into a dimension x period
 * matrix - see RootCauseHeatmapCard, which renders this. Split into its own file (not a component)
 * so that file only exports components, which react-refresh/only-export-components needs for Fast
 * Refresh to work on it. */
export function buildHeatmap(rows: KpiCardResponse[]) {
  const byKpiId = new Map<string, HeatmapDimensionRow>()
  const periods = new Set<string>()
  let max = 0

  for (const row of rows) {
    periods.add(row.periodStart)
    let entry = byKpiId.get(row.kpiId)
    if (!entry) {
      entry = { kpiId: row.kpiId, label: extractRootCauseDimension(row.name), valuesByPeriod: new Map() }
      byKpiId.set(row.kpiId, entry)
    }
    // A period with no value stays out of the map - the card shows it as no data, never a 0.
    if (row.value == null) continue
    if (row.value > max) max = row.value
    entry.valuesByPeriod.set(row.periodStart, row.value)
  }

  return {
    dimensionRows: Array.from(byKpiId.values()).sort((a, b) => a.label.localeCompare(b.label)),
    periods: Array.from(periods).sort(),
    max,
  }
}
