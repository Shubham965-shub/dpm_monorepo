// Matches KPI_DOMAINS' keys in kpiConfig.ts (the backend's Qms/PeoplePerformance/InstrumentHealth/
// LabPerformance controllers).
export type SectionKey = 'qms' | 'people-performance' | 'instrument-health' | 'lab-performance'

// The Overall Heatmap is wired to real data - see useHeatmapData.ts / kpiConfig.ts.
//
// The composite "QC Score" and its per-section score/trend have no backend counterpart at all
// (no endpoint, no aggregation logic) - this is a UI placeholder pending a product decision on
// how that composite should be computed. Single source shared by QcScoreCard and
// SectionSummaryList so the same mocked numbers aren't duplicated in two files.
export const SECTION_SCORE_MOCK: Record<
  SectionKey,
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

export interface NeedsAttentionItem {
  id: string
  status: 'no-action' | 'escalated'
  category: string
  date: string
  kpiName: string
  detail: string
  assignee: string
  action: 'nudge' | 'act-now'
}

// Same caveat as QC Score / Positive Signals - there's no "agenda" or nudge/escalation endpoint
// server-side yet. Placeholder until that workflow exists.
export const NEEDS_ATTENTION_MOCK: NeedsAttentionItem[] = [
  {
    id: 'sample-tat-jul',
    status: 'no-action',
    category: 'Lab Performance',
    date: 'Jul 26',
    kpiName: 'Sample TAT',
    detail: '29.02 hrs vs 24 hrs Action limit',
    assignee: 'Ravi',
    action: 'nudge',
  },
  {
    id: 'sample-tat-aug',
    status: 'no-action',
    category: 'Lab Performance',
    date: 'Aug 26',
    kpiName: 'Sample TAT',
    detail: '33.44 hrs vs 24 hrs Action limit',
    assignee: 'Ravi',
    action: 'nudge',
  },
  {
    id: 'sla-adherence-aug',
    status: 'no-action',
    category: 'Lab Performance',
    date: 'Aug 26',
    kpiName: 'SLA Adherence',
    detail: '83.63% vs 95% Action limit',
    assignee: 'Ravi',
    action: 'nudge',
  },
  {
    id: 'difot-jan',
    status: 'escalated',
    category: 'Lab Performance',
    date: 'Jan 26',
    kpiName: 'DIFOT Support Index',
    detail: '79.9% vs 96% Action limit',
    assignee: 'Ravi',
    action: 'act-now',
  },
  {
    id: 'tests-analyst-mar',
    status: 'escalated',
    category: 'Lab Performance',
    date: 'Mar 26',
    kpiName: 'Tests / Analyst / Day',
    detail: '12.89 nos vs 15 nos Action limit',
    assignee: 'Ravi',
    action: 'act-now',
  },
]
