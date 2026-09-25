import type { KpiCardResponse } from './kpiSchemas'

// Several breakdown-style KPI endpoints (priority-sample-compliance, human-error-severity,
// human-error-by-product, ...) return one row per (period, category) under a shared kpiId, with
// the category embedded in `name` as "<kpi label> (<category>)" rather than a separate field - see
// e.g. LabPerformanceService.getPrioritySampleCompliance / QmsService.getHumanErrorSeverity.
export function extractRowCategory(name: string): string {
  const match = /\(([^)]+)\)\s*$/.exec(name)
  return match ? match[1] : name
}

// Some endpoints (capa-aging-status, human-error-severity, human-error-by-product) return many
// rows per period under one shared kpiId, differing only by `name` (one per level/classification/
// material) - kpiId alone is not a unique series identity for those, so it's combined with `name`.
export function seriesKeyFor(row: KpiCardResponse) {
  return `${row.kpiId}|${row.name}`
}
