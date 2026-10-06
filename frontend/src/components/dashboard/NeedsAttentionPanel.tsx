import { Clock, ChevronRight, User } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { KPI_DOMAINS } from '@/lib/kpiConfig'
import { NEEDS_ATTENTION_MOCK, type NeedsAttentionItem } from './mockData'
import { DemoDataBadge, UPCOMING_FEATURE_CLASS } from './DemoDataBadge'
import type { SectionFilterValue } from './FilterBar'
import type { KpiVisibilityLens } from '@/lib/roleAccess'

const STATUS_STYLE = {
  'no-action': { label: 'No Action Taken', dot: 'bg-[#ba0000]', text: 'text-[#ba0000]' },
  escalated: { label: 'Escalated', dot: 'bg-[#cf7e00]', text: 'text-[#cf7e00]' },
} as const

// Same red/amber pair STATUS_STYLE above uses - the persona view's dot/label colour follows each
// item's own severity (see mockData's pendingActionSource) rather than the legacy no-action/escalated split.
const SOURCE_SEVERITY_STYLE = {
  critical: { dot: 'bg-[#ba0000]', text: 'text-[#ba0000]' },
  warning: { dot: 'bg-[#cf7e00]', text: 'text-[#cf7e00]' },
} as const

type PendingActionsLens = 'analyst' | 'supervisor' | 'leadership'

/** Per-lens differences the three reference screenshots show: Analyst and Leadership both name
 * the assignee (Supervisor's screenshot already names the team in Team Workload Overview above),
 * and only Leadership keeps a single "Act Now" action - Analyst/Supervisor both use a flat "Mark
 * as Completed" instead. */
const PENDING_ACTIONS_VARIANT: Record<PendingActionsLens, { showAssignee: boolean; buttonLabel: string }> = {
  analyst: { showAssignee: true, buttonLabel: 'Mark as Completed' },
  supervisor: { showAssignee: false, buttonLabel: 'Mark as Completed' },
  leadership: { showAssignee: true, buttonLabel: 'Act Now' },
}

/** The Analyst/Supervisor/Leadership reference screenshots' "Pending Actions" panel - same
 * underlying NEEDS_ATTENTION_MOCK items as the legacy view below, framed per pendingActionSource
 * (see PENDING_ACTIONS_VARIANT for what varies by lens). */
function PendingActionsView({ items, lens }: { items: NeedsAttentionItem[]; lens: PendingActionsLens }) {
  const variant = PENDING_ACTIONS_VARIANT[lens]
  return (
    <div className="rounded-lg border border-border bg-card">
      <div className="flex items-center justify-between border-b border-border px-5 py-4">
        <div className="flex items-center gap-2">
          <Clock className="size-4 text-foreground" />
          <h2 className="text-base font-semibold">Pending Actions</h2>
          <DemoDataBadge />
        </div>
        <a href="#" className="flex items-center gap-1 text-xs font-semibold text-brand hover:underline">
          Open agenda drill-down
          <ChevronRight className="size-3" />
        </a>
      </div>

      {items.length === 0 ? (
        <p className={cn('px-5 py-6 text-sm text-muted-foreground', UPCOMING_FEATURE_CLASS)}>No pending actions in this section.</p>
      ) : (
        <div className={cn('flex flex-col divide-y divide-border md:flex-row md:divide-x md:divide-y-0', UPCOMING_FEATURE_CLASS)}>
          {items.map((item) => {
            const source = item.pendingActionSource[lens]
            const style = SOURCE_SEVERITY_STYLE[source.severity]
            return (
              <div key={item.id} className="flex flex-1 flex-col justify-between gap-3 p-4">
                <div className="flex flex-col gap-1">
                  <div className="flex items-center gap-1">
                    <span className={cn('size-3 rounded-full', style.dot)} />
                    <span className={cn('text-xs font-semibold', style.text)}>{source.label}</span>
                  </div>
                  <p className="text-xs font-bold text-muted-foreground">
                    {item.category} · {item.date}
                  </p>
                  <p className="text-sm font-semibold">{item.kpiName}</p>
                  <p className="text-xs text-muted-foreground">{item.detail}</p>
                </div>

                <div className="flex items-center justify-between gap-2">
                  {variant.showAssignee && (
                    <span className="flex items-center gap-1 text-xs text-muted-foreground">
                      <User className="size-3" />
                      {item.assignee}
                    </span>
                  )}
                  <Button size="xs" variant="outline" className={cn('border-emerald-600 text-emerald-700 hover:bg-emerald-50', !variant.showAssignee && 'ml-auto')}>
                    {variant.buttonLabel}
                  </Button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export function NeedsAttentionPanel({ section, lens }: { section: SectionFilterValue; lens: KpiVisibilityLens }) {
  // NEEDS_ATTENTION_MOCK.category holds a domain's real kpiConfig.ts label (e.g. "Lab
  // Performance") - filter by that so the top Section filter actually narrows this panel too.
  const sectionLabel = section === 'all' ? null : KPI_DOMAINS.find((d) => d.key === section)?.label
  const items = sectionLabel ? NEEDS_ATTENTION_MOCK.filter((i) => i.category === sectionLabel) : NEEDS_ATTENTION_MOCK

  if (lens === 'analyst' || lens === 'supervisor' || lens === 'leadership') {
    return <PendingActionsView items={items} lens={lens} />
  }

  return (
    <div className="rounded-lg border border-border bg-card">
      <div className="flex items-center justify-between border-b border-border px-5 py-4">
        <div className="flex items-center gap-2">
          <Clock className="size-4 text-foreground" />
          <h2 className="text-base font-semibold">Needs Attention</h2>
          <DemoDataBadge />
        </div>
        <a href="#" className="flex items-center gap-1 text-xs font-semibold text-brand hover:underline">
          Open agenda drill-down
          <ChevronRight className="size-3" />
        </a>
      </div>

      {items.length === 0 ? (
        <p className={cn('px-5 py-6 text-sm text-muted-foreground', UPCOMING_FEATURE_CLASS)}>Nothing needs attention in this section.</p>
      ) : (
      <div className={cn('flex flex-col divide-y divide-border md:flex-row md:divide-x md:divide-y-0', UPCOMING_FEATURE_CLASS)}>
        {items.map((item) => {
          const status = STATUS_STYLE[item.status]
          return (
            <div key={item.id} className="flex flex-1 flex-col justify-between gap-4 p-4">
              <div className="flex flex-col gap-1">
                <div className="flex items-center gap-1">
                  <span className={cn('size-3 rounded-full', status.dot)} />
                  <span className={cn('text-xs font-semibold', status.text)}>{status.label}</span>
                </div>
                <p className="text-xs font-bold text-muted-foreground">
                  {item.category} · {item.date}
                </p>
                <p className="text-sm font-semibold">{item.kpiName}</p>
              </div>

              <div className="flex flex-col gap-2">
                <p className="text-xs text-muted-foreground">{item.detail}</p>
                <div className="flex items-center justify-between">
                  <span className="flex items-center gap-1 text-xs text-muted-foreground">
                    <User className="size-3" />
                    {item.assignee}
                  </span>
                  {item.action === 'nudge' ? (
                    <Button size="xs" variant="outline" className="border-brand text-brand hover:bg-brand/10">
                      Nudge
                    </Button>
                  ) : (
                    <Button size="xs" className="bg-amber-500 text-white hover:bg-amber-600">
                      Act Now
                    </Button>
                  )}
                </div>
              </div>
            </div>
          )
        })}
      </div>
      )}
    </div>
  )
}
