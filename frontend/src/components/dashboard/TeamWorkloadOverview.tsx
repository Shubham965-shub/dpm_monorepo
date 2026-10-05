import { Users } from 'lucide-react'
import { cn, initials, pluralize } from '@/lib/utils'
import { useMyAnalysts } from '@/hooks/useMyAnalysts'
import { WORKLOAD_PATTERN_MOCK, type WorkloadPattern } from './mockData'
import { DemoDataBadge } from './DemoDataBadge'

const STATUS_META: Record<WorkloadPattern['status'], { label: string; badgeClass: string }> = {
  available: { label: 'Available', badgeClass: 'bg-emerald-100 text-emerald-700' },
  busy: { label: 'Busy', badgeClass: 'bg-amber-100 text-amber-700' },
  overloaded: { label: 'Overloaded', badgeClass: 'bg-red-100 text-red-700' },
}

// The bar's colour follows severity (any critical KPI reads red regardless of workload label,
// since that's the more urgent signal) rather than literally mirroring the status badge - a
// "Busy" analyst with a critical KPI and a "Busy" analyst without one shouldn't look the same.
function barColorClass(pattern: WorkloadPattern): string {
  if (pattern.criticalCount > 0 || pattern.status === 'overloaded') return 'bg-red-500'
  if (pattern.status === 'busy') return 'bg-amber-500'
  return 'bg-emerald-500'
}

// Directional only (this is placeholder data - see WORKLOAD_PATTERN_MOCK) - scales with how much
// is on someone's plate rather than trying to match one specific real formula.
function barFillPercent(pattern: WorkloadPattern): number {
  return Math.min(100, pattern.kpiCount * 15 + pattern.criticalCount * 20 + pattern.taskCount * 10)
}

/** Per-analyst KPI/task assignment and workload - only shown in the Supervisor lens (see
 * DashboardPage), since an analyst only manages their own work, not a team's. The roster (who
 * shows up) is real - GET /api/auth/me/analysts, scoped to the logged-in supervisor's own direct
 * reports - but the workload numbers on each card are still placeholder (see DemoDataBadge and
 * WORKLOAD_PATTERN_MOCK's own doc comment): no KPI/task-reassignment system exists server-side yet,
 * so "reassign KPIs" isn't wired up either. */
export function TeamWorkloadOverview() {
  const { data: analysts, isLoading } = useMyAnalysts()
  const roster = analysts ?? []

  return (
    <div className="rounded-lg border border-sky-200 bg-sky-50/60">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-sky-200/70 px-5 py-4">
        <div className="flex flex-wrap items-center gap-2">
          <Users className="size-4 text-foreground" />
          <h2 className="text-base font-semibold">Team workload overview</h2>
          <DemoDataBadge title="Workload numbers are placeholder - no KPI/task-assignment system exists server-side yet. The analyst roster itself is real." />
          <span className="text-xs text-muted-foreground">— click any analyst to reassign KPIs</span>
        </div>
        <span className="shrink-0 text-xs font-semibold text-foreground">
          {pluralize(roster.length, 'analyst')} on this shift
        </span>
      </div>

      {isLoading ? (
        <p className="p-4 text-xs text-muted-foreground">Loading…</p>
      ) : roster.length === 0 ? (
        <p className="p-4 text-xs text-muted-foreground">No analysts report to you.</p>
      ) : (
        <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2 lg:grid-cols-4">
          {roster.map((analyst, i) => {
            const pattern = WORKLOAD_PATTERN_MOCK[i % WORKLOAD_PATTERN_MOCK.length]
            const status = STATUS_META[pattern.status]
            return (
              <div key={analyst.id} className="flex flex-col gap-2 rounded-lg border border-border bg-card p-3">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold">
                      {initials(analyst.fullName)}
                    </span>
                    <div>
                      <div className="text-sm font-semibold">{analyst.fullName}</div>
                      <div className="text-xs text-muted-foreground">{analyst.title}</div>
                    </div>
                  </div>
                  <span className={cn('shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold', status.badgeClass)}>
                    {status.label}
                  </span>
                </div>

                <div className="h-1.5 w-full rounded-full bg-muted">
                  <div
                    className={cn('h-1.5 rounded-full', barColorClass(pattern))}
                    style={{ width: `${barFillPercent(pattern)}%` }}
                  />
                </div>

                <p className="text-xs text-muted-foreground">
                  {pluralize(pattern.kpiCount, 'KPI')}
                  {pattern.criticalCount > 0 && ` · ${pluralize(pattern.criticalCount, 'critical')}`}
                  {' · '}
                  {pluralize(pattern.taskCount, 'task')}
                </p>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
