import { Construction } from 'lucide-react'

/** The Performance tab (KpiValidationDashboard) is real and functional, but hidden behind this
 * placeholder for now per a deliberate staged-rollout request - see git history for the toggle.
 * Also reused for the CXO/Leadership role tabs (see TopNav's ROLE_TABS), which aren't routed to
 * a real view yet - only Operations (Operator/Supervisor) is built out so far. */
export function PerformanceWorkInProgress({ title = 'Performance' }: { title?: string }) {
  return (
    <div className="flex min-h-[60svh] flex-col items-center justify-center gap-3 px-6 text-center">
      <span className="flex size-14 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <Construction className="size-7" />
      </span>
      <h1 className="text-xl font-semibold">{title}</h1>
      <p className="max-w-sm text-sm text-muted-foreground">Work in progress — check back soon.</p>
    </div>
  )
}
