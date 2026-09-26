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

export interface TeamWorkloadMember {
  name: string
  title: string
  status: 'available' | 'busy' | 'overloaded'
  kpiCount: number
  criticalCount: number
  taskCount: number
}

// No team-roster/workload endpoint (or KPI/task reassignment) exists server-side yet -
// placeholder until that feature is built.
export const TEAM_WORKLOAD_MOCK: TeamWorkloadMember[] = [
  { name: 'Ravi Shankar', title: 'Operator', status: 'busy', kpiCount: 3, criticalCount: 1, taskCount: 4 },
  { name: 'Arjun Mehta', title: 'Engineer', status: 'overloaded', kpiCount: 2, criticalCount: 1, taskCount: 3 },
  { name: 'Divya Rao', title: 'Safety Officer', status: 'busy', kpiCount: 1, criticalCount: 1, taskCount: 2 },
  { name: 'Imran Sheikh', title: 'Inventory Manager', status: 'available', kpiCount: 1, criticalCount: 0, taskCount: 2 },
  { name: 'Mahesh Gowda', title: 'Dispatch Lead', status: 'available', kpiCount: 1, criticalCount: 0, taskCount: 2 },
  { name: 'Anand Pillai', title: 'Operator', status: 'available', kpiCount: 0, criticalCount: 0, taskCount: 1 },
  { name: 'Lakshmi Reddy', title: 'Operator', status: 'busy', kpiCount: 2, criticalCount: 0, taskCount: 3 },
  { name: 'Priya Nair', title: 'HR Lead', status: 'available', kpiCount: 1, criticalCount: 0, taskCount: 1 },
]

/** Same 5 incidents read three ways - the Operator/Supervisor/Leadership reference screenshots each
 * frame the SAME underlying pending action differently (who raised/escalated it, and at what
 * severity), since that framing is relative to the viewer's place in the hierarchy (e.g. an item
 * an Operator sees as "Assigned by HOD" reads as "Raised by CXO" one level up at Supervisor, and as
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
  /** Only 'operator', 'supervisor', and 'leadership' have a reference screenshot (see
   * NeedsAttentionPanel's persona "Pending Actions" view) - CXO falls back to the status/action
   * fields above. */
  pendingActionSource: Record<'operator' | 'supervisor' | 'leadership', PendingActionSource>
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
      operator: { label: 'Assigned by HOD (CXO Concern)', severity: 'critical' },
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
      operator: { label: 'Assigned by HOD (CXO Concern)', severity: 'critical' },
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
      operator: { label: 'Escalated by Group Leader', severity: 'warning' },
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
      operator: { label: 'Raised by HOD', severity: 'warning' },
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
      operator: { label: 'Raised by HOD', severity: 'warning' },
      supervisor: { label: 'Escalated by HOD', severity: 'warning' },
      leadership: { label: 'No Action Taken', severity: 'warning' },
    },
  },
]
