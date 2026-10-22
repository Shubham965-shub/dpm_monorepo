import type { WorkloadProfile } from './teamHealth'

// Matches KPI_DOMAINS' keys in kpiConfig.ts (the backend's Qms/PeoplePerformance/InstrumentHealth/
// LabPerformance controllers).
export type SectionKey = 'qms' | 'people-performance' | 'instrument-health' | 'lab-performance'

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

// The roster (who shows up) is real - GET /api/auth/me/analysts, scoped to the logged-in
// supervisor's own direct reports (see useMyAnalysts, TeamWorkloadOverview). Each person's own
// KPI/task picture below is still placeholder: no KPI/task-assignment system exists server-side
// yet to source it from (confirmed against every agg_*/fact_* table - none carry a per-analyst
// dimension). TeamWorkloadOverview cycles through this array by roster index to pair each real
// analyst with one of these profiles (ported as-is from the team-overview.html design reference,
// just decoupled from its own hardcoded names - the content itself, not just its shape, is the
// reference), rather than inventing a second ad-hoc placeholder scheme.
export const WORKLOAD_PROFILES: WorkloadProfile[] = [
  {
    kpis: [
      { name: 'Batch release TAT', target: '≤ 3 days', current: '2.6 days', status: 'ok' },
      { name: 'Deviation closure within 30 days', target: '≥ 90%', current: '93%', status: 'ok' },
    ],
    openTasks: [
      { task: 'Release batches 24B130 to 24B133', linkedKpi: 'Batch release TAT', priority: 'Medium', due: 'Tomorrow', status: 'In progress' },
      { task: 'Review open deviations, line 3', linkedKpi: 'Deviation closure within 30 days', priority: 'Low', due: '10 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Weekly release summary', linkedKpi: 'Batch release TAT', completedOn: '2 Oct 2026', onTime: true },
      { task: 'Close DV-1190', linkedKpi: 'Deviation closure within 30 days', completedOn: '29 Sep 2026', onTime: true },
    ],
  },
  {
    kpis: [
      { name: 'OOS closure TAT', target: '≤ 5 days', current: '7.1 days', status: 'breach', critical: true },
      { name: 'Lab investigation aging', target: '≤ 10 days', current: '11.5 days', status: 'warn' },
    ],
    openTasks: [
      { task: 'Close OOS-2291 investigation', linkedKpi: 'OOS closure TAT', priority: 'High', due: 'Today', status: 'In progress' },
      { task: 'Escalate aged investigation LI-0873', linkedKpi: 'Lab investigation aging', priority: 'High', due: '7 Oct', status: 'Not started' },
      { task: 'Review analyst retraining plan', linkedKpi: 'Lab investigation aging', priority: 'Medium', due: '11 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Close OOS-2274', linkedKpi: 'OOS closure TAT', completedOn: '2 Oct 2026', onTime: false },
      { task: 'Root-cause review, LI-0861', linkedKpi: 'Lab investigation aging', completedOn: '26 Sep 2026', onTime: true },
    ],
  },
  {
    kpis: [
      { name: 'Plant OEE', target: '≥ 78%', current: '74%', status: 'warn', critical: true },
      { name: 'Right-first-time', target: '≥ 97%', current: '97.6%', status: 'ok' },
      { name: 'Batch release adherence', target: '≥ 95%', current: '96%', status: 'ok' },
    ],
    openTasks: [
      { task: 'Review OEE loss plan with team leaders', linkedKpi: 'Plant OEE', priority: 'High', due: 'Today', status: 'In progress' },
      { task: 'Monthly performance review', linkedKpi: 'Right-first-time', priority: 'Medium', due: '9 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'September KPI review', linkedKpi: 'Plant OEE', completedOn: '1 Oct 2026', onTime: true },
      { task: 'Approve line 2 improvement plan', linkedKpi: 'Plant OEE', completedOn: '24 Sep 2026', onTime: true },
    ],
  },
  {
    kpis: [
      { name: 'Critical equipment downtime', target: '≤ 3%', current: '2.4%', status: 'ok', critical: true },
      { name: 'Calibration compliance', target: '≥ 98%', current: '99%', status: 'ok' },
    ],
    openTasks: [
      { task: 'Plan shutdown maintenance, line 1', linkedKpi: 'Critical equipment downtime', priority: 'Medium', due: '12 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Calibrate compression machine C-02', linkedKpi: 'Calibration compliance', completedOn: '1 Oct 2026', onTime: true },
      { task: 'Spare parts audit', linkedKpi: 'Critical equipment downtime', completedOn: '27 Sep 2026', onTime: true },
    ],
  },
  {
    kpis: [
      { name: 'Yield, solid dosage', target: '≥ 96%', current: '94.8%', status: 'warn', critical: true },
      { name: 'Material variance', target: '≤ 0.5%', current: '0.9%', status: 'breach' },
      { name: 'Line clearance compliance', target: '100%', current: '100%', status: 'ok' },
    ],
    openTasks: [
      { task: 'Investigate yield loss, batch 24B118', linkedKpi: 'Yield, solid dosage', priority: 'High', due: 'Today', status: 'In progress' },
      { task: 'Review material variance, granulation', linkedKpi: 'Material variance', priority: 'High', due: '8 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Weekly yield report', linkedKpi: 'Yield, solid dosage', completedOn: '2 Oct 2026', onTime: true },
      { task: 'Line clearance audit', linkedKpi: 'Line clearance compliance', completedOn: '28 Sep 2026', onTime: true },
      { task: 'Variance review, September', linkedKpi: 'Material variance', completedOn: '22 Sep 2026', onTime: false },
    ],
  },
  {
    kpis: [
      { name: 'Safety observation closure', target: '≥ 95%', current: '96%', status: 'ok', critical: true },
      { name: 'GMP training compliance', target: '≥ 95%', current: '91%', status: 'warn' },
    ],
    openTasks: [
      { task: 'Schedule refresher training, packing team', linkedKpi: 'GMP training compliance', priority: 'Medium', due: '10 Oct', status: 'Not started' },
      { task: 'Close 2 open safety observations', linkedKpi: 'Safety observation closure', priority: 'Medium', due: '8 Oct', status: 'In progress' },
    ],
    doneTasks: [
      { task: 'Monthly PPE walk', linkedKpi: 'Safety observation closure', completedOn: '3 Oct 2026', onTime: true },
      { task: 'Training matrix update', linkedKpi: 'GMP training compliance', completedOn: '30 Sep 2026', onTime: false },
    ],
  },
  {
    kpis: [
      { name: 'Deviation closure within 30 days', target: '≥ 90%', current: '71%', status: 'breach', critical: true },
      { name: 'CAPA on-time closure', target: '≥ 90%', current: '84%', status: 'warn' },
      { name: 'Change control TAT', target: '≤ 15 days', current: '12 days', status: 'ok' },
    ],
    openTasks: [
      { task: 'Close 5 aged deviations', linkedKpi: 'Deviation closure within 30 days', priority: 'High', due: 'Today', status: 'In progress' },
      { task: 'Review overdue CAPAs', linkedKpi: 'CAPA on-time closure', priority: 'High', due: '7 Oct', status: 'Not started' },
      { task: 'Update change control tracker', linkedKpi: 'Change control TAT', priority: 'Low', due: '12 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Close DV-1167', linkedKpi: 'Deviation closure within 30 days', completedOn: '19 Sep 2026', onTime: false },
      { task: 'Approve CC-0412', linkedKpi: 'Change control TAT', completedOn: '27 Sep 2026', onTime: true },
    ],
  },
  {
    kpis: [
      { name: 'Stock cover, key materials', target: '≥ 30 days', current: '34 days', status: 'ok' },
      { name: 'On-time in-full dispatch', target: '≥ 98%', current: '99.1%', status: 'ok' },
      { name: 'Dispatch damage rate', target: '≤ 0.2%', current: '0.3%', status: 'warn' },
    ],
    openTasks: [
      { task: 'Plan dispatch for export order EX-4410', linkedKpi: 'On-time in-full dispatch', priority: 'Medium', due: '7 Oct', status: 'In progress' },
      { task: 'Review packing damage causes', linkedKpi: 'Dispatch damage rate', priority: 'Medium', due: '11 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Monthly stock ageing report', linkedKpi: 'Stock cover, key materials', completedOn: '1 Oct 2026', onTime: true },
      { task: 'Carrier scorecard update', linkedKpi: 'On-time in-full dispatch', completedOn: '25 Sep 2026', onTime: true },
    ],
  },
  {
    kpis: [
      { name: 'Sample testing TAT', target: '≤ 48 h', current: '52 h', status: 'warn' },
      { name: 'Stability pull-out adherence', target: '100%', current: '100%', status: 'ok' },
    ],
    openTasks: [
      { task: 'Clear pending HPLC samples', linkedKpi: 'Sample testing TAT', priority: 'High', due: 'Tomorrow', status: 'In progress' },
      { task: 'Prepare stability pull-out list, October', linkedKpi: 'Stability pull-out adherence', priority: 'Medium', due: '9 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Stability pull-out, September', linkedKpi: 'Stability pull-out adherence', completedOn: '30 Sep 2026', onTime: true },
      { task: 'Weekly TAT report', linkedKpi: 'Sample testing TAT', completedOn: '2 Oct 2026', onTime: false },
    ],
  },
  {
    kpis: [{ name: 'Document review TAT', target: '≤ 2 days', current: '1.8 days', status: 'ok' }],
    openTasks: [{ task: 'Review 12 batch records', linkedKpi: 'Document review TAT', priority: 'Medium', due: '7 Oct', status: 'In progress' }],
    doneTasks: [
      { task: 'Review SOP revisions', linkedKpi: 'Document review TAT', completedOn: '2 Oct 2026', onTime: true },
      { task: 'Batch record review, 24B112', linkedKpi: 'Document review TAT', completedOn: '26 Sep 2026', onTime: true },
      { task: 'Log book audit', linkedKpi: 'Document review TAT', completedOn: '20 Sep 2026', onTime: true },
    ],
  },
  {
    kpis: [{ name: 'Data entry accuracy', target: '≥ 99%', current: '97.2%', status: 'breach', critical: true }],
    openTasks: [
      { task: 'Correct entries flagged in audit trail review', linkedKpi: 'Data entry accuracy', priority: 'High', due: 'Today', status: 'In progress' },
      { task: 'Data integrity refresher', linkedKpi: 'Data entry accuracy', priority: 'Medium', due: '9 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Daily log reconciliation', linkedKpi: 'Data entry accuracy', completedOn: '3 Oct 2026', onTime: false },
      { task: 'Weekly accuracy check', linkedKpi: 'Data entry accuracy', completedOn: '28 Sep 2026', onTime: false },
    ],
  },
  {
    kpis: [{ name: 'Raw material sampling TAT', target: '≤ 24 h', current: '20 h', status: 'ok' }],
    openTasks: [
      { task: 'Sample incoming excipient lots', linkedKpi: 'Raw material sampling TAT', priority: 'Medium', due: 'Today', status: 'In progress' },
      { task: 'Update sampling log', linkedKpi: 'Raw material sampling TAT', priority: 'Low', due: '8 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Sample lot RM-5521', linkedKpi: 'Raw material sampling TAT', completedOn: '3 Oct 2026', onTime: true },
      { task: 'Weekly sampling summary', linkedKpi: 'Raw material sampling TAT', completedOn: '28 Sep 2026', onTime: true },
    ],
  },
  {
    kpis: [
      { name: 'Calibration due closure', target: '≥ 98%', current: '94%', status: 'warn' },
      { name: 'Audit observation closure', target: '≥ 90%', current: '92%', status: 'ok' },
    ],
    openTasks: [
      { task: 'Calibrate 6 overdue instruments', linkedKpi: 'Calibration due closure', priority: 'High', due: 'Tomorrow', status: 'In progress' },
      { task: 'Close audit observation AO-208', linkedKpi: 'Audit observation closure', priority: 'Medium', due: '12 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Calibration schedule, October', linkedKpi: 'Calibration due closure', completedOn: '1 Oct 2026', onTime: true },
      { task: 'Close AO-199', linkedKpi: 'Audit observation closure', completedOn: '24 Sep 2026', onTime: true },
    ],
  },
  {
    kpis: [
      { name: 'Water system monitoring compliance', target: '100%', current: '100%', status: 'ok', critical: true },
      { name: 'Environmental monitoring excursions', target: '≤ 2 per month', current: '3', status: 'warn' },
    ],
    openTasks: [
      { task: 'Investigate EM excursion, grade C area', linkedKpi: 'Environmental monitoring excursions', priority: 'High', due: 'Today', status: 'In progress' },
      { task: 'Monthly water trend report', linkedKpi: 'Water system monitoring compliance', priority: 'Medium', due: '10 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Water sampling, week 39', linkedKpi: 'Water system monitoring compliance', completedOn: '28 Sep 2026', onTime: true },
      { task: 'EM trend review, September', linkedKpi: 'Environmental monitoring excursions', completedOn: '2 Oct 2026', onTime: true },
    ],
  },
  {
    kpis: [
      { name: 'Pending reviews', target: '≤ 5', current: '9', status: 'breach' },
      { name: 'Report issue TAT', target: '≤ 2 days', current: '1.7 days', status: 'ok' },
    ],
    openTasks: [
      { task: 'Clear backlog of pending reviews', linkedKpi: 'Pending reviews', priority: 'High', due: 'Tomorrow', status: 'In progress' },
      { task: 'Issue analytical reports, week 41', linkedKpi: 'Report issue TAT', priority: 'Medium', due: '9 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Issue reports, week 40', linkedKpi: 'Report issue TAT', completedOn: '2 Oct 2026', onTime: true },
      { task: 'Review pending data sets', linkedKpi: 'Pending reviews', completedOn: '27 Sep 2026', onTime: false },
    ],
  },
  {
    kpis: [],
    openTasks: [
      { task: 'Complete GMP induction modules', linkedKpi: '—', priority: 'Medium', due: '9 Oct', status: 'In progress' },
      { task: 'Shadow QC lab walkthrough', linkedKpi: '—', priority: 'Low', due: '12 Oct', status: 'Not started' },
    ],
    doneTasks: [
      { task: 'Onboarding module 1', linkedKpi: '—', completedOn: '30 Sep 2026', onTime: true },
      { task: 'Safety induction', linkedKpi: '—', completedOn: '25 Sep 2026', onTime: true },
    ],
  },
]

/** Same 5 incidents read three ways - the Analyst/Supervisor/Leadership reference screenshots each
 * frame the SAME underlying pending action differently (who raised/escalated it, and at what
 * severity), since that framing is relative to the viewer's place in the hierarchy (e.g. an item
 * an Analyst sees as "Assigned by HOD" reads as "Raised by CXO" one level up at Supervisor, and as
 * "Concern Raised by CXO" one level up again at Leadership). */
interface PendingActionSource {
  label: string
  severity: 'critical' | 'warning'
}

export interface NeedsAttentionItem {
  id: string
  status: 'no-action' | 'escalated'
  category: string
  date: string
  kpiName: string
  detail: string
  assignee: string
  action: 'nudge' | 'act-now'
  /** Only 'analyst', 'supervisor', and 'leadership' have a reference screenshot (see
   * NeedsAttentionPanel's persona "Pending Actions" view) - CXO falls back to the status/action
   * fields above. */
  pendingActionSource: Record<'analyst' | 'supervisor' | 'leadership', PendingActionSource>
}

// Same caveat as Positive Signals - there's no "agenda" or nudge/escalation endpoint
// server-side yet. Placeholder until that workflow exists. Ordered chronologically (Jan -> Aug),
// matching both persona reference screenshots exactly.
export const NEEDS_ATTENTION_MOCK: NeedsAttentionItem[] = [
  {
    id: 'difot-jan',
    status: 'escalated',
    category: 'Lab Performance',
    date: 'Jan 26',
    kpiName: 'DIFOT Support Index',
    detail: '79.9% vs 96% Action limit',
    assignee: 'Ravi',
    action: 'act-now',
    pendingActionSource: {
      analyst: { label: 'Assigned by HOD (CXO Concern)', severity: 'critical' },
      supervisor: { label: 'Raised by CXO', severity: 'critical' },
      leadership: { label: 'Concern Raised by CXO', severity: 'critical' },
    },
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
    pendingActionSource: {
      analyst: { label: 'Assigned by HOD (CXO Concern)', severity: 'critical' },
      supervisor: { label: 'Raised by CXO', severity: 'critical' },
      leadership: { label: 'Concern Raised by CXO', severity: 'critical' },
    },
  },
  {
    id: 'sample-tat-jul',
    status: 'no-action',
    category: 'Lab Performance',
    date: 'Jul 26',
    kpiName: 'Sample TAT',
    detail: '29.02 hrs vs 24 hrs Action limit',
    assignee: 'Ravi',
    action: 'nudge',
    pendingActionSource: {
      analyst: { label: 'Escalated by Group Leader', severity: 'warning' },
      supervisor: { label: 'Escalated by HOD', severity: 'warning' },
      leadership: { label: 'Escalated by Group Leader', severity: 'warning' },
    },
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
    pendingActionSource: {
      analyst: { label: 'Raised by HOD', severity: 'warning' },
      supervisor: { label: 'Escalated by HOD', severity: 'warning' },
      leadership: { label: 'No Action Taken', severity: 'warning' },
    },
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
    pendingActionSource: {
      analyst: { label: 'Raised by HOD', severity: 'warning' },
      supervisor: { label: 'Escalated by HOD', severity: 'warning' },
      leadership: { label: 'No Action Taken', severity: 'warning' },
    },
  },
]
