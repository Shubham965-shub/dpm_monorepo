import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { cn, initials } from '@/lib/utils'
import { useBackdropClose } from '@/hooks/useBackdropClose'
import type { TeamMember } from '@/lib/authSchemas'
import { severityMeta } from './types'
import { computeTeamMemberHealth, KPI_STATUS_LABEL, PRIORITY_SEVERITY, type WorkloadProfile } from './teamHealth'

/** One severity-colored pill, reusing the app's own severity palette (severityMeta) rather than a
 * second ad-hoc color scheme - null severity renders the same neutral grey NO_THRESHOLD_META uses
 * everywhere else a KPI/value has no colour judgement to make. */
function Pill({ severity, label }: { severity: Parameters<typeof severityMeta>[0]; label: string }) {
  const meta = severityMeta(severity)
  return (
    <span
      className="inline-flex w-fit items-center rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap"
      style={{ backgroundColor: meta.paleHex, color: meta.solidHex }}
    >
      {label}
    </span>
  )
}

function StatTile({ value, label }: { value: string; label: string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="text-2xl font-bold">{value}</div>
      <div className="text-xs text-muted-foreground">{label}</div>
    </div>
  )
}

function TableSection({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <h3 className="text-base font-semibold">{title}</h3>
      <p className="mb-3 text-sm text-muted-foreground">{subtitle}</p>
      <div className="overflow-x-auto">{children}</div>
    </section>
  )
}

const TH_CLASS = 'border-b border-border px-2.5 py-2 text-left text-xs font-semibold text-muted-foreground'
const TD_CLASS = 'border-b border-border px-2.5 py-2.5 align-top text-sm last:border-b-0'

/** Per-person KPI ownership and task drill-down from the Supervisor lens's Team Workload Overview
 * roster - design reference: team-overview.html. The roster (who this is) is real (GET
 * /api/auth/me/analysts); the KPI/task content itself is still placeholder (see
 * WORKLOAD_PROFILES's own doc comment in mockData.ts) - no KPI/task-assignment system exists
 * server-side yet. Portaled to document.body like every other detail modal (see KpiDetailModal's
 * own comment on why - an ancestor section's inline viewTransitionName makes it a containing block
 * for position: fixed, which a plain z-index can't escape). */
export function TeamMemberDetailModal({
  analyst,
  profile,
  onClose,
}: {
  analyst: TeamMember
  profile: WorkloadProfile
  onClose: () => void
}) {
  const health = computeTeamMemberHealth(profile.kpis)
  const meta = severityMeta(health.severity)
  const inLimit = profile.kpis.filter((k) => k.status === 'ok').length
  const onTime = profile.doneTasks.filter((t) => t.onTime).length

  return createPortal(
    // z-[100]: above the page's own cards and their z-50 chart tooltips/z-60 Filters popovers, so
    // an open modal can never z-fight with whatever's still rendered behind it on the page.
    <div className="fixed inset-0 z-[100] flex items-start justify-center overflow-y-auto bg-black/40 p-6" {...useBackdropClose(onClose)}>
      <div className="w-full max-w-3xl rounded-xl bg-card shadow-xl">
        <div className="h-1.5 w-full rounded-t-xl" style={{ backgroundColor: meta.solidHex }} />

        <div className="flex items-center justify-between border-b border-border px-6 py-3">
          <button onClick={onClose} className="text-sm text-muted-foreground hover:text-foreground">
            ← Back to Team Workload Overview
          </button>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">
            <X className="size-4" />
          </button>
        </div>

        <div className="flex flex-col gap-4 border-b border-border p-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <span className="flex size-14 shrink-0 items-center justify-center rounded-full bg-muted text-lg font-semibold">
              {initials(analyst.fullName)}
            </span>
            <div>
              <h2 className="text-xl font-bold">{analyst.fullName}</h2>
              <div className="text-sm text-muted-foreground">{analyst.title}</div>
            </div>
            <Pill severity={health.severity} label={health.label} />
          </div>
          <div className="sm:w-56">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>KPI health</span>
              <span className="font-semibold text-foreground">{health.pct == null ? 'No KPIs assigned' : `${health.pct}%`}</span>
            </div>
            <div className="mt-1.5 h-2.5 w-full rounded-full bg-muted">
              <div
                className="h-2.5 rounded-full"
                style={{ width: `${health.pct == null ? 0 : Math.max(health.pct, 4)}%`, backgroundColor: meta.solidHex }}
              />
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 p-6 pb-0 sm:grid-cols-4">
          <StatTile value={`${inLimit} of ${profile.kpis.length}`} label="KPIs within limit" />
          <StatTile value={String(profile.openTasks.length)} label="Open tasks" />
          <StatTile value={String(profile.doneTasks.length)} label="Tasks completed (last 30 days)" />
          <StatTile value={profile.doneTasks.length ? `${Math.round((onTime / profile.doneTasks.length) * 100)}%` : '–'} label="Completed on time" />
        </div>

        <div className="flex flex-col gap-4 p-6">
          <TableSection title="KPIs owned" subtitle="Current value against the target limit">
            {profile.kpis.length === 0 ? (
              <p className="py-2 text-sm text-muted-foreground">No KPIs assigned yet.</p>
            ) : (
              <table className="w-full min-w-[480px] border-collapse">
                <thead>
                  <tr>
                    <th className={TH_CLASS}>KPI</th>
                    <th className={TH_CLASS}>Target</th>
                    <th className={TH_CLASS}>Current</th>
                    <th className={TH_CLASS}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {profile.kpis.map((kpi) => (
                    <tr key={kpi.name}>
                      <td className={TD_CLASS}>
                        {kpi.name}
                        {kpi.critical && <Pill severity="action" label="Critical" />}
                      </td>
                      <td className={TD_CLASS}>{kpi.target}</td>
                      <td className={cn(TD_CLASS, 'font-semibold')}>{kpi.current}</td>
                      <td className={TD_CLASS}>
                        <Pill
                          severity={kpi.status === 'ok' ? 'in-control' : kpi.status === 'warn' ? 'alert' : 'action'}
                          label={KPI_STATUS_LABEL[kpi.status]}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </TableSection>

          <TableSection title="Open tasks" subtitle="Each task is linked to the KPI it supports">
            <table className="w-full min-w-[560px] border-collapse">
              <thead>
                <tr>
                  <th className={TH_CLASS}>Task</th>
                  <th className={TH_CLASS}>Linked KPI</th>
                  <th className={TH_CLASS}>Priority</th>
                  <th className={TH_CLASS}>Due</th>
                  <th className={TH_CLASS}>Status</th>
                </tr>
              </thead>
              <tbody>
                {profile.openTasks.map((task) => (
                  <tr key={task.task}>
                    <td className={TD_CLASS}>{task.task}</td>
                    <td className={TD_CLASS}>{task.linkedKpi}</td>
                    <td className={TD_CLASS}>
                      <Pill severity={PRIORITY_SEVERITY[task.priority]} label={task.priority} />
                    </td>
                    <td className={TD_CLASS}>{task.due}</td>
                    <td className={TD_CLASS}>{task.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableSection>

          <TableSection title="Completed task history" subtitle="Most recent first">
            <table className="w-full min-w-[480px] border-collapse">
              <thead>
                <tr>
                  <th className={TH_CLASS}>Task</th>
                  <th className={TH_CLASS}>Linked KPI</th>
                  <th className={TH_CLASS}>Completed</th>
                  <th className={TH_CLASS}>Result</th>
                </tr>
              </thead>
              <tbody>
                {profile.doneTasks.map((task) => (
                  <tr key={task.task}>
                    <td className={TD_CLASS}>{task.task}</td>
                    <td className={TD_CLASS}>{task.linkedKpi}</td>
                    <td className={TD_CLASS}>{task.completedOn}</td>
                    <td className={TD_CLASS}>
                      <Pill severity={task.onTime ? 'in-control' : 'alert'} label={task.onTime ? 'On time' : 'Late'} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableSection>
        </div>
      </div>
    </div>,
    document.body,
  )
}
