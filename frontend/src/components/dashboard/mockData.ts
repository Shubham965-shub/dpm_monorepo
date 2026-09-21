import type { KpiCatalogueSection } from '@/lib/kpiCatalogue'

// The Overall Heatmap is wired to real data - see useHeatmapData.ts / kpiApi.ts.
//
// The composite "QC Score" and its per-section score/trend have no backend counterpart at all
// (no endpoint, no aggregation logic) - this is a UI placeholder pending a product decision on
// how that composite should be computed. Single source shared by QcScoreCard and
// SectionSummaryList so the same mocked numbers aren't duplicated in two files.
export const SECTION_SCORE_MOCK: Record<
  KpiCatalogueSection['key'],
  { shortLabel: string; category: string; score: number; deltaPts: number; starred: boolean }
> = {
  'lab-performance': { shortLabel: 'Lab', category: 'Operational Efficiency', score: 76, deltaPts: -2.1, starred: true },
  'instrument-health': { shortLabel: 'Instrument', category: 'Asset Reliability', score: 82, deltaPts: 1.4, starred: false },
  qms: { shortLabel: 'QMS', category: 'Compliance System', score: 69, deltaPts: -4.8, starred: false },
  'people-performance': { shortLabel: 'People', category: 'Workforce', score: 89, deltaPts: 2.2, starred: false },
}

export const QC_SCORE_MOCK = {
  overall: 79,
  label: 'Acceptable',
  bestEver: { score: 91, period: 'February 2026' },
}

// Same caveat as QC Score - no "positive signals" feed exists server-side yet. Note the
// Training Completion Rate signal in particular: the real endpoint
// (GET /api/people-performance/training-completion-rate) exists, but its source table
// (agg_cw_training_completion_rate) is documented as empty in backend/README.md, so this
// specific signal would render nothing if wired to real data today.
export const POSITIVE_SIGNALS_MOCK = [
  {
    title: 'Calibration compliance recovered to 97.8%',
    detail: 'Up +2.4% after June sprint — highest in 6 months',
  },
  {
    title: 'Training completion rate at 96.2%',
    detail: 'Best quarter in 18 months — People section score 89',
  },
]
