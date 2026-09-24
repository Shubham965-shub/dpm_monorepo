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

// The composite "QC Score" and its per-section score/trend have no backend counterpart at all
// (no endpoint, no aggregation logic) - this is a UI placeholder pending a product decision on
// how that composite should be computed. Single source shared by QcScoreCard and
// SectionSummaryList so the same mocked numbers aren't duplicated in two files.
// "starred" used to live here as a static mock flag - it's now real, per-user state (see
// useFavoriteSections.ts), so it's not part of this placeholder data anymore.
export const SECTION_SCORE_MOCK: Record<
  SectionKey,
  {
    shortLabel: string
    category: string
    score: number
    deltaPts: number
    // Exact hex from Figma (file T5oSGlK2eTYF8KvKP5M9lF, node 5024:20119 "Frame 128") - the
    // QcScoreCard breakdown's number color and its underline bar color aren't always the same hue.
    numberHex: string
    barHex: string
  }
> = {
  'lab-performance': { shortLabel: 'Lab', category: 'Operational Efficiency', score: 76, deltaPts: -2.1, numberHex: '#bb4d00', barHex: '#f59e0b' },
  'instrument-health': { shortLabel: 'Instrument', category: 'Asset Reliability', score: 82, deltaPts: 1.4, numberHex: '#bb4d00', barHex: '#ef4444' },
  qms: { shortLabel: 'QMS', category: 'Compliance System', score: 69, deltaPts: -4.8, numberHex: '#c10007', barHex: '#ef4444' },
  'people-performance': { shortLabel: 'People', category: 'Workforce', score: 89, deltaPts: 2.2, numberHex: '#008236', barHex: '#028662' },
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

export interface KpiThresholdMock {
  /** Must match a real kpi.id in KPI_DOMAINS (kpiConfig.ts) for this domain. */
  id: string
  domainKey: SectionKey
  // Target/Alert/Action zone-boundary thresholds (the dashed reference lines + "T:/A:/Act:"
  // legend on SeverityKpiCard). Sourced directly from Figma (file T5oSGlK2eTYF8KvKP5M9lF, node
  // 5024:13253 "KPI List") via get_design_context for the KPIs Figma actually shows worked
  // examples for (see the "Figma-sourced" vs "extrapolated" grouping below) - not guessed, since
  // neither the backend nor kpiConfig.ts has a threshold concept yet (see backend/README.md's
  // "Known data gaps"). Placeholder until the backend computes real zone boundaries - value/trend
  // themselves are real (useDomainTrendKpis fetches them live and classifies severity against
  // these thresholds - see types.ts's classifySeverity), only these three numbers are mock.
  target: number
  alertThreshold: number
  actionThreshold: number
}

// Covers every real KPI_DOMAINS entry (kpiConfig.ts) EXCEPT: the 3 chartType:'bar' QMS KPIs
// (capa-aging-status, human-error-by-product, product-wise-failure) - those are category rankings
// summed across the whole date range (see KpiChartCard's toBarData comment), not a per-period
// single value/trend, so a target/alert/action zone doesn't apply to them; they stay on the plain
// real-data ranking-bar card with no thresholds (see SectionSummaryList.tsx's "uncovered"
// fallback) - and priority-sample-compliance, which gets its own stacked-percentage card instead
// (see the comment right above its excluded id below).
//
// Threshold numbers were sanity-checked against real observed values (see backend/DB_details.py-
// style live curl of each endpoint, trailing 3 months, site=KRSG) so the target/alert/action scale
// at least sits in the same ballpark as what the KPI actually reports - direction (higher/lower-
// is-better) inferred from each KPI's meaning. Still placeholders (no real business-defined zone
// boundary exists anywhere), just no longer off by 10-100x from the real unit/scale.
const AVAILABILITY_THRESHOLDS = { target: 40, alertThreshold: 25, actionThreshold: 10 }

export const KPI_THRESHOLD_MOCK: KpiThresholdMock[] = [
  // --- Lab Performance (real unit: days/tests/%/samples/sequences,injections per analyst) ---
  { id: 'sample-tat', domainKey: 'lab-performance', target: 24, alertThreshold: 28, actionThreshold: 34 },
  // Real ~8-9 tests/analyst/day - original 12/16/18 assumed a much higher scale.
  { id: 'tests-per-analyst', domainKey: 'lab-performance', target: 12, alertThreshold: 9, actionThreshold: 6 },
  // Real ~51-63% - original 82/90/98 assumed adherence was already high; real KRSG data runs low.
  { id: 'sla-adherence', domainKey: 'lab-performance', target: 85, alertThreshold: 70, actionThreshold: 55 },
  // priority-sample-compliance is deliberately absent here: the real data is a raw sample COUNT
  // per priority tier (P1..P4, always summing to the period's total), not a single trending value
  // a target/alert/action zone can classify - see PriorityComplianceCard.tsx's stacked-percentage
  // breakdown instead (SectionSummaryList.tsx routes it there, not into DomainSeverityBoard).
  // Real ~56-86 sequences/analyst - original 20/30/40 was far too low.
  { id: 'hplc-sequences-per-analyst', domainKey: 'lab-performance', target: 90, alertThreshold: 70, actionThreshold: 50 },
  // Real ~7-11 injections/analyst - original 70/95/120 was ~10x too high.
  { id: 'hplc-injections-per-analyst', domainKey: 'lab-performance', target: 12, alertThreshold: 9, actionThreshold: 6 },

  // --- Instrument Health (real unit: breakdowns/day, %, ratio, days) ---
  // Real ~1.4-1.6 breakdowns/DAY - original 3/6/10 assumed a monthly count.
  { id: 'breakdown-frequency', domainKey: 'instrument-health', target: 0.8, alertThreshold: 1.2, actionThreshold: 1.8 },
  // downtime defaults to all instGroups combined (Dissolution/GC/HPLC); availability and utilization
  // are one card per group. Real per-group ranges below checked across all 3 groups, trailing 3 months.
  // Real ~1.2-17.7% across groups (Dissolution best, GC worst) - close to the original 2/5/8 scale.
  { id: 'downtime', domainKey: 'instrument-health', target: 2, alertThreshold: 5, actionThreshold: 8 },
  // Real ~3-47% (Dissolution's "actual_available_time/(rows*24)" formula runs far lower than a
  // typical uptime % - see InstrumentHealthService's javadoc) - original 98/93/88 assumed near-100%.
  // One shared threshold for all 3 groups (confirmed) - Dissolution (~5%) sits far below GC/HPLC
  // (~35-40%), so it reads red by design.
  ...(['dissolution', 'gc', 'hplc'] as const).map((group) => ({
    id: `${group}-availability`, domainKey: 'instrument-health' as const, ...AVAILABILITY_THRESHOLDS,
  })),
  // Real ~52-96% (Dissolution highest, GC/HPLC ~50-60) - close to the original 85/75/65 scale.
  // One shared threshold for all 3 groups (same as availability above) - one card per group.
  ...(['dissolution', 'gc', 'hplc'] as const).map((group) => ({
    id: `${group}-utilization`, domainKey: 'instrument-health' as const, target: 85, alertThreshold: 75, actionThreshold: 65,
  })),
  { id: 'calibration-compliance', domainKey: 'instrument-health', target: 100, alertThreshold: 95, actionThreshold: 90 },
  { id: 'pm-adherence', domainKey: 'instrument-health', target: 95, alertThreshold: 88, actionThreshold: 80 },
  // Real ~0.09-0.19 (a ratio, not %) - original 1/2/4 was ~10x too high.
  { id: 'failure-rate', domainKey: 'instrument-health', target: 0.05, alertThreshold: 0.10, actionThreshold: 0.20 },
  // Real 1 data point at 55 days - original 20/30/45 had too low a ceiling.
  { id: 'qualification-cycle-time', domainKey: 'instrument-health', target: 25, alertThreshold: 40, actionThreshold: 60 },

  // --- People Performance (real unit: %, "weight" index) ---
  // Real ~1.6-5.2% - original 5/10/15 assumed a higher baseline.
  { id: 'repeat-analysis-rate', domainKey: 'people-performance', target: 2, alertThreshold: 4, actionThreshold: 7 },
  // Real ~10,000-18,000 ("weight" unit) - original 10/15/20 was off by ~1000x.
  { id: 'reviewer-productivity', domainKey: 'people-performance', target: 18000, alertThreshold: 14000, actionThreshold: 9000 },
  { id: 'rft-documentation', domainKey: 'people-performance', target: 98, alertThreshold: 92, actionThreshold: 85 },
  { id: 'training-completion-rate', domainKey: 'people-performance', target: 95, alertThreshold: 85, actionThreshold: 75 },

  // --- QMS's 17 non-bar-chart real KPIs (3 chartType:'bar' KPIs excluded, see file-level
  // comment). Units confirmed live: rates are "per 1000 tests", most breakdowns are "count". ---
  { id: 'invalid-oos-rate', domainKey: 'qms', target: 1.0, alertThreshold: 1.5, actionThreshold: 2.5 },
  { id: 'confirmed-oos-rate', domainKey: 'qms', target: 0.5, alertThreshold: 1.0, actionThreshold: 2.0 },
  // Real ~46-50 count/month - original 5/10/11 was ~5x too low.
  { id: 'oos-trend', domainKey: 'qms', target: 40, alertThreshold: 48, actionThreshold: 55 },
  // Real ~72-99 count/month - original 5/10/18 was ~5x too low.
  { id: 'oot-trend', domainKey: 'qms', target: 70, alertThreshold: 90, actionThreshold: 110 },
  // Real ~42-95 count/month - original 20/35/50 was too low a ceiling.
  { id: 'deviations', domainKey: 'qms', target: 40, alertThreshold: 70, actionThreshold: 100 },
  { id: 'investigation-closure-tat', domainKey: 'qms', target: 15, alertThreshold: 25, actionThreshold: 28 },
  // Real ~500 count - original 5/10/15 was ~50x too low (this endpoint sums across a wide window).
  { id: 'rci-beyond-timeline', domainKey: 'qms', target: 400, alertThreshold: 480, actionThreshold: 550 },
  { id: 'repeat-failure-rate', domainKey: 'qms', target: 5, alertThreshold: 10, actionThreshold: 15 },
  // root-cause is a per-category breakdown (14 series/period, real per-category counts 0-2) - the
  // card uses the first series as primary, so thresholds are scaled to a single category's count,
  // not a cross-category total. root-cause-consolidated is absent here on purpose: it renders as
  // RootCauseHeatmapCard's dimension x period matrix instead (see SectionSummaryList.tsx), which
  // has no single trending value a target/alert/action zone could classify.
  { id: 'root-cause', domainKey: 'qms', target: 1, alertThreshold: 3, actionThreshold: 6 },
  { id: 'capa-open-by-date-opened', domainKey: 'qms', target: 10, alertThreshold: 20, actionThreshold: 30 },
  { id: 'capa-effectiveness', domainKey: 'qms', target: 90, alertThreshold: 80, actionThreshold: 72 },
  // Real ~19-33% - original 5/10/15 was too low a ceiling.
  { id: 'capa-recurrence-rate', domainKey: 'qms', target: 15, alertThreshold: 25, actionThreshold: 35 },
  // Real ~1.06-1.9 per 1000 tests - original 2/4/6 was too high a ceiling.
  { id: 'human-error-rate', domainKey: 'qms', target: 1.0, alertThreshold: 1.5, actionThreshold: 2.2 },
  // Real ~0-0.12 per 1000 tests - original 1/2/3 was ~10x too high.
  { id: 'repeat-human-error-rate', domainKey: 'qms', target: 0.05, alertThreshold: 0.1, actionThreshold: 0.2 },
  // Real value is always null (a category-status breakdown, no single numeric reading) - severity
  // never resolves for this one regardless of these numbers; kept for shape consistency only.
  { id: 'human-error-severity', domainKey: 'qms', target: 3, alertThreshold: 6, actionThreshold: 9 },
  { id: 'method-failure-rate', domainKey: 'qms', target: 5, alertThreshold: 10, actionThreshold: 15 },
]
