import type { Severity } from './types'

export type KpiStatus = 'ok' | 'warn' | 'breach'

export interface OwnedKpi {
  name: string
  target: string
  current: string
  status: KpiStatus
  /** Counts double toward the weighted health score below - a breached critical KPI drags the
   * score down harder than an equally-breached non-critical one. */
  critical?: boolean
}

export interface OpenTask {
  task: string
  /** The KPI this task supports - '—' when the person has no KPIs assigned at all yet (onboarding). */
  linkedKpi: string
  priority: 'High' | 'Medium' | 'Low'
  due: string
  status: string
}

export interface CompletedTask {
  task: string
  linkedKpi: string
  completedOn: string
  onTime: boolean
}

/** One person's full KPI/task picture - still placeholder (see mockData.ts's WORKLOAD_PROFILES'
 * own doc comment): no KPI/task-assignment system exists server-side yet. */
export interface WorkloadProfile {
  kpis: OwnedKpi[]
  openTasks: OpenTask[]
  doneTasks: CompletedTask[]
}

const KPI_SCORE: Record<KpiStatus, number> = { ok: 1, warn: 0.5, breach: 0 }

export interface TeamMemberHealth {
  /** null when the person has no KPIs assigned at all - reads as "No KPIs" (grey), not a 0%
   * off-track score, since an empty KPI list isn't the same claim as "failing every KPI". */
  pct: number | null
  severity: Severity | null
  label: string
}

/** Weighted average of each KPI's score (within limit = 1, warning = 0.5, breach = 0), critical
 * KPIs counting double. 80%+ on track, 50-79% at risk, below 50% off track. */
export function computeTeamMemberHealth(kpis: OwnedKpi[]): TeamMemberHealth {
  if (kpis.length === 0) return { pct: null, severity: null, label: 'No KPIs' }
  let num = 0
  let den = 0
  for (const kpi of kpis) {
    const weight = kpi.critical ? 2 : 1
    num += KPI_SCORE[kpi.status] * weight
    den += weight
  }
  const pct = Math.round((num / den) * 100)
  if (pct >= 80) return { pct, severity: 'in-control', label: 'On track' }
  if (pct >= 50) return { pct, severity: 'alert', label: 'At risk' }
  return { pct, severity: 'action', label: 'Off track' }
}

export const KPI_STATUS_LABEL: Record<KpiStatus, string> = {
  ok: 'Within limit',
  warn: 'Warning',
  breach: 'Breach',
}

export const PRIORITY_SEVERITY: Record<OpenTask['priority'], Severity | null> = {
  High: 'action',
  Medium: 'alert',
  Low: null,
}
