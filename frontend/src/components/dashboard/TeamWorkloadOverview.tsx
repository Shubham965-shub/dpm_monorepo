import { useState } from 'react'
import { Users } from 'lucide-react'
import { initials, pluralize } from '@/lib/utils'
import { useMyAnalysts } from '@/hooks/useMyAnalysts'
import type { TeamMember } from '@/lib/authSchemas'
import { WORKLOAD_PROFILES } from './mockData'
import { severityMeta } from './types'
import { computeTeamMemberHealth } from './teamHealth'
import { TeamMemberDetailModal } from './TeamMemberDetailModal'

/** One severity-colored dot + label, matching the legend/pill language used across every other
 * severity surface in the app (SectionSummaryBar's chips, SeverityBadgeSummary's badges) rather
 * than a separate colour scheme just for this card. */
function HealthLegendEntry({ severity, label }: { severity: Parameters<typeof severityMeta>[0]; label: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <span className="size-2.5 rounded-full" style={{ backgroundColor: severityMeta(severity).solidHex }} />
      {label}
    </span>
  )
}

/** Per-analyst KPI/task ownership - only shown in the Supervisor lens (see DashboardPage), since an
 * analyst only manages their own work, not a team's. The roster (who shows up) is real - GET
 * /api/auth/me/analysts, scoped to the logged-in supervisor's own direct reports. Each card's own
 * health score, and the drill-down behind it (see TeamMemberDetailModal), uses the same per-person
 * weighted-KPI model as every card here (see teamHealth.ts's computeTeamMemberHealth) - the KPI/task
 * content itself is still placeholder (see WORKLOAD_PROFILES's own doc comment in mockData.ts): no
 * KPI/task-assignment system exists server-side yet. Design reference: team-overview.html. */
export function TeamWorkloadOverview() {
  const { data: analysts, isLoading } = useMyAnalysts()
  const roster = analysts ?? []
  // The id, not the TeamMember object itself - roster is fresh TanStack Query data on every
  // refetch (new array, new object references even with identical content), so holding onto an
  // object captured at click time would silently stop matching roster.findIndex below.
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const selectedIndex = roster.findIndex((a) => a.id === selectedId)
  const selected: TeamMember | null = selectedIndex >= 0 ? roster[selectedIndex] : null
  const selectedProfile = selected ? WORKLOAD_PROFILES[selectedIndex % WORKLOAD_PROFILES.length] : null

  return (
    <div className="rounded-lg border border-sky-200 bg-sky-50/60">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-sky-200/70 px-5 py-4">
        <div className="flex flex-wrap items-center gap-2">
          <Users className="size-4 text-foreground" />
          <h2 className="text-base font-semibold">Team overview</h2>
          <span className="text-xs text-muted-foreground">— click any analyst to see their KPIs and tasks</span>
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
        <>
          <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2 lg:grid-cols-4">
            {roster.map((analyst, i) => {
              const profile = WORKLOAD_PROFILES[i % WORKLOAD_PROFILES.length]
              const health = computeTeamMemberHealth(profile.kpis)
              const meta = severityMeta(health.severity)
              const criticalBreached = profile.kpis.filter((k) => k.critical && k.status !== 'ok').length
              const metaLine =
                pluralize(profile.kpis.length, 'KPI') +
                (criticalBreached > 0 ? ` · ${pluralize(criticalBreached, 'critical')}` : '') +
                ' · ' +
                pluralize(profile.openTasks.length, 'task')
              return (
                <button
                  key={analyst.id}
                  type="button"
                  onClick={() => setSelectedId(analyst.id)}
                  className="flex flex-col gap-2 rounded-lg border border-border bg-card p-3 text-left transition-colors hover:border-sky-300"
                >
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
                    <span
                      className="shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold"
                      style={{ backgroundColor: meta.paleHex, color: meta.solidHex }}
                    >
                      {health.label}
                    </span>
                  </div>

                  <div
                    className="h-1.5 w-full rounded-full bg-muted"
                    title={health.pct == null ? 'No KPIs assigned' : `KPI health ${health.pct}%`}
                  >
                    <div
                      className="h-1.5 rounded-full"
                      style={{ width: `${health.pct == null ? 0 : Math.max(health.pct, 6)}%`, backgroundColor: meta.solidHex }}
                    />
                  </div>

                  <div className="flex items-center justify-between text-xs text-muted-foreground">
                    <span>{metaLine}</span>
                    {health.pct != null && <span className="font-semibold text-foreground">{health.pct}%</span>}
                  </div>
                </button>
              )
            })}
          </div>
          <div className="flex flex-wrap gap-x-5 gap-y-1.5 border-t border-sky-200/70 px-5 py-3 text-xs text-muted-foreground">
            <span>Bar = share of the person&apos;s KPIs within limit (critical KPIs count double)</span>
            <HealthLegendEntry severity="in-control" label="80%+ On track" />
            <HealthLegendEntry severity="alert" label="50 to 79% At risk" />
            <HealthLegendEntry severity="action" label="Below 50% Off track" />
            <HealthLegendEntry severity={null} label="No KPIs assigned" />
          </div>
        </>
      )}

      {selected && selectedProfile && (
        <TeamMemberDetailModal analyst={selected} profile={selectedProfile} onClose={() => setSelectedId(null)} />
      )}
    </div>
  )
}
