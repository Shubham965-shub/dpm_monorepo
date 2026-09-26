import { Users } from 'lucide-react'
import { cn, initials, pluralize } from '@/lib/utils'
import { TEAM_WORKLOAD_MOCK, type TeamWorkloadMember } from './mockData'
import { DemoDataBadge } from './DemoDataBadge'

const STATUS_META: Record<TeamWorkloadMember['status'], { label: string; badgeClass: string }> = {
  available: { label: 'Available', badgeClass: 'bg-emerald-100 text-emerald-700' },
  busy: { label: 'Busy', badgeClass: 'bg-amber-100 text-amber-700' },
  overloaded: { label: 'Overloaded', badgeClass: 'bg-red-100 text-red-700' },
}

// The bar's colour follows severity (any critical KPI reads red regardless of workload label,
// since that's the more urgent signal) rather than literally mirroring the status badge - a
// "Busy" operator with a critical KPI and a "Busy" operator without one shouldn't look the same.
function barColorClass(member: TeamWorkloadMember): string {
  if (member.criticalCount > 0 || member.status === 'overloaded') return 'bg-red-500'
  if (member.status === 'busy') return 'bg-amber-500'
  return 'bg-emerald-500'
}

// Directional only (this is placeholder data - see TEAM_WORKLOAD_MOCK) - scales with how much is
// on someone's plate rather than trying to match one specific real formula.
function barFillPercent(member: TeamWorkloadMember): number {
  return Math.min(100, member.kpiCount * 15 + member.criticalCount * 20 + member.taskCount * 10)
}

/** Per-operator KPI/task assignment and workload - only shown in the Supervisor lens (see
 * DashboardPage), since an operator/analyst manages their own work, not a team's. No real
 * roster/assignment endpoint exists yet (see DemoDataBadge) - "reassign KPIs" isn't wired up. */
export function TeamWorkloadOverview() {
  return (
    <div className="rounded-lg border border-sky-200 bg-sky-50/60">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-sky-200/70 px-5 py-4">
        <div className="flex flex-wrap items-center gap-2">
          <Users className="size-4 text-foreground" />
          <h2 className="text-base font-semibold">Team workload overview</h2>
          <DemoDataBadge />
          <span className="text-xs text-muted-foreground">— click any operator to reassign KPIs</span>
        </div>
        <span className="shrink-0 text-xs font-semibold text-foreground">
          {pluralize(TEAM_WORKLOAD_MOCK.length, 'operator')} on this shift
        </span>
      </div>

      <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2 lg:grid-cols-4">
        {TEAM_WORKLOAD_MOCK.map((member) => {
          const status = STATUS_META[member.status]
          return (
            <div key={member.name} className="flex flex-col gap-2 rounded-lg border border-border bg-card p-3">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold">
                    {initials(member.name)}
                  </span>
                  <div>
                    <div className="text-sm font-semibold">{member.name}</div>
                    <div className="text-xs text-muted-foreground">{member.title}</div>
                  </div>
                </div>
                <span className={cn('shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold', status.badgeClass)}>
                  {status.label}
                </span>
              </div>

              <div className="h-1.5 w-full rounded-full bg-muted">
                <div
                  className={cn('h-1.5 rounded-full', barColorClass(member))}
                  style={{ width: `${barFillPercent(member)}%` }}
                />
              </div>

              <p className="text-xs text-muted-foreground">
                {pluralize(member.kpiCount, 'KPI')}
                {member.criticalCount > 0 && ` · ${pluralize(member.criticalCount, 'critical')}`}
                {' · '}
                {pluralize(member.taskCount, 'task')}
              </p>
            </div>
          )
        })}
      </div>
    </div>
  )
}
