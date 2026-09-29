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
    const value = row.value ?? 0
    if (value > max) max = value
    let entry = byKpiId.get(row.kpiId)
    if (!entry) {
      entry = { kpiId: row.kpiId, label: extractRootCauseDimension(row.name), valuesByPeriod: new Map() }
      byKpiId.set(row.kpiId, entry)
    }
    entry.valuesByPeriod.set(row.periodStart, value)
  }

  return {
    dimensionRows: Array.from(byKpiId.values()).sort((a, b) => a.label.localeCompare(b.label)),
    periods: Array.from(periods).sort(),
    max,
  }
}
