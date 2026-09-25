import type { HeatmapRow, RagStatus } from './types'

// Matches KPI_DOMAINS' keys in kpiConfig.ts (the backend's Qms/PeoplePerformance/InstrumentHealth/
// LabPerformance controllers).
export type SectionKey = 'qms' | 'people-performance' | 'instrument-health' | 'lab-performance'

export interface HeatmapMockSection {
  key: string
  title: string
  rows: HeatmapRow[]
}

const DEMO_MONTHS = ['Jun', 'Jul', 'Aug']
const DEMO_STATUS_CYCLE: RagStatus[] = ['action', 'alert', 'in-control']

// Deterministic (no Math.random), purely illustrative status pattern - this heatmap is a static
// prototype view (see OverallHeatmapDemo.tsx's DemoDataBadge), not wired to useHeatmapData/the
// real backend. Row/section counts intentionally match the Figma wireframe's own placeholder
// numbers, not the real KPI_DOMAINS catalogue (kpiConfig.ts) - see SectionSummaryList.tsx further
// down this page for the real-catalogue-backed view.
function demoHeatmapRows(count: number, seedOffset: number): HeatmapRow[] {
  return Array.from({ length: count }, (_, i) => ({
    slug: `demo-${seedOffset}-${i}`,
    name: `Demo KPI ${i + 1}`,
    cells: DEMO_MONTHS.map((month, m) => ({
      month,
      status: DEMO_STATUS_CYCLE[(seedOffset + i + m) % DEMO_STATUS_CYCLE.length],
    })),
  }))
}

export const HEATMAP_MOCK: HeatmapMockSection[] = [
  { key: 'lab-performance', title: 'Lab Performance', rows: demoHeatmapRows(6, 0) },
  { key: 'instrument-health', title: 'Instrument Health', rows: demoHeatmapRows(5, 1) },
  { key: 'qms', title: 'QMS Dashboard', rows: demoHeatmapRows(31, 2) },
  { key: 'people-performance', title: 'People Performance', rows: demoHeatmapRows(6, 3) },
]

// No "positive signals" feed exists server-side yet. Note the
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

// Same caveat as Positive Signals - there's no "agenda" or nudge/escalation endpoint
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
    kpiName: 'Sample SLA Adherence',
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
