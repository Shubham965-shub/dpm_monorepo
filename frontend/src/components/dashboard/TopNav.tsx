import { Sun, Moon, Bell, Compass } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import heliosLogo from '@/assets/helios-logo.svg'
import stridesLogo from '@/assets/strides-logo.svg'
import { useTheme } from '@/hooks/useTheme'
import { cn } from '@/lib/utils'
import type { UserPublic } from '@/lib/authSchemas'
import type { RoleTab, KpiVisibilityLens } from '@/lib/roleAccess'
import { DataFreshnessLabel } from './DataFreshnessLabel'
import { FeedbackLauncher } from './FeedbackLauncher'
import { ProfileMenu } from './ProfileMenu'

/** A real vertical bar, not a text "|" glyph - matches InvestigationAI's header divider. */
function NavDivider({ className }: { className?: string }) {
  return <div className={cn('h-6 w-px bg-white/20', className)} />
}

// Wireframe shows CXO / Leadership / Operations tabs, which don't map onto the backend's fixed
// Role enum (CXO/HOD/MANAGER/SUPERVISOR/ANALYST/ADMIN) 1:1 - each renders the same dashboard
// shell, filtered by that tab's own KPI Framework-derived lens (see SectionSummaryList's
// KPI_VISIBILITY). Which tab a user lands on, and whether they can switch, is derived from their
// real roles (see lib/roleAccess.ts) - only ADMIN gets the switchable nav shown here (and can
// preview any of the 4 lens variants: CXO, Leadership, Analyst, Supervisor); everyone else is
// locked to one tab with no nav to the others (see `locked` below).
const ROLE_TABS: RoleTab[] = ['CXO', 'Leadership', 'Operations']

export function TopNav({
  user,
  onLogout,
  activeRole,
  onActiveRoleChange,
  locked,
  onReplayTour,
}: {
  user: UserPublic
  onLogout: () => void
  activeRole: RoleTab
  onActiveRoleChange: (role: RoleTab) => void
  /** True for every role except ADMIN (see resolveRoleTab) - renders activeRole as plain text
   * instead of a switchable pill row, since the user has no other tab to switch to anyway. */
  locked: boolean
  /** Re-runs the guided tour for whichever screen is currently showing (see CoachMarks.tsx) -
   * undefined on a view with no coach tour of its own (QA/Performance), which hides the button
   * rather than wiring it to a no-op. */
  onReplayTour?: () => void
}) {
  const { theme, toggleTheme } = useTheme()

  return (
    <header className="flex items-center justify-between border-b border-brand bg-brand-dark px-6 py-3 text-white md:px-10 lg:px-20">
      <div className="flex items-center gap-3">
        <img src={stridesLogo} alt="Strides" className="h-6 w-auto" />
        <NavDivider />
        <span className="text-lg font-semibold">Helios</span>
        <img src={heliosLogo} alt="Helios" className="h-8 w-auto" />
      </div>
      <nav className="flex items-center gap-6 text-sm">
        <FeedbackLauncher />
        {onReplayTour && (
          <button
            type="button"
            onClick={onReplayTour}
            title="Replay the guided tour for this screen"
            className="flex items-center gap-1.5 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-sm font-medium text-white hover:bg-white/20"
          >
            <Compass className="size-4" />
            Replay tour
          </button>
        )}
        <button
          onClick={toggleTheme}
          title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          className="flex items-center justify-center opacity-80 hover:opacity-100"
        >
          {theme === 'dark' ? <Moon className="size-4" /> : <Sun className="size-4" />}
        </button>
        {locked ? (
          <span className="rounded-full bg-brand px-3 py-1 font-medium">{activeRole}</span>
        ) : (
          ROLE_TABS.map((tab) => (
            <button
              key={tab}
              onClick={() => onActiveRoleChange(tab)}
              className={
                tab === activeRole
                  ? 'rounded-full bg-brand px-3 py-1 font-medium'
                  : 'text-white/80 hover:text-white'
              }
            >
              {tab}
            </button>
          ))
        )}
        <NavDivider />
        <ProfileMenu user={user} onLogout={onLogout} />
      </nav>
    </header>
  )
}

export type SubNavView = 'dashboard' | 'performance' | 'qa'

/** A sub-choice within whichever TopNav role tab is active - QC's Operations tab splits further
 * into Analyst/Supervisor, QA's CXO tab splits further into CXO/Cluster Head (see
 * qaLensForRoleTab). Both are real, separately-scoped KPI-visibility lists, not a "some filter vs
 * no filter" toggle, so this is a generic two-(or-more)-option pill row rather than two bespoke
 * components. `locked` (true for a real, non-ADMIN role already confined to one option - see
 * resolveAnalystSupervisorView/resolveQaLens) renders the current value as plain text instead of a
 * switchable control, since that role has no other option to switch to. */
function SubNavLensToggle({
  value,
  options,
  onChange,
  locked,
}: {
  value: KpiVisibilityLens
  options: { value: KpiVisibilityLens; label: string }[]
  onChange: (value: KpiVisibilityLens) => void
  locked: boolean
}) {
  if (locked) {
    const label = options.find((o) => o.value === value)?.label ?? value
    return (
      <span className="rounded-md border border-input px-3 py-1 text-xs font-medium text-muted-foreground">{label}</span>
    )
  }
  return (
    <div className="flex overflow-hidden rounded-md border border-input text-xs font-medium">
      {options.map((option) => (
        <button
          key={option.value}
          onClick={() => onChange(option.value)}
          className={cn('px-3 py-1', value === option.value ? 'bg-foreground text-background' : 'text-muted-foreground hover:text-foreground')}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}

export function SubNav({
  view,
  onViewChange,
  secondaryToggle,
  showDashboardTab = true,
  showQaTab = false,
}: {
  view: SubNavView
  onViewChange: (view: SubNavView) => void
  /** undefined = the active role tab has no further sub-choice (e.g. CXO/Leadership on the QC
   * dashboard) - hides the toggle entirely rather than showing a meaningless default. Set by
   * DashboardPage to QC's Operations Analyst/Supervisor choice or QA's CXO Cluster-Head choice,
   * whichever applies to the currently active tab/view (see SubNavLensToggle). */
  secondaryToggle?: {
    value: KpiVisibilityLens
    options: { value: KpiVisibilityLens; label: string }[]
    onChange: (value: KpiVisibilityLens) => void
    locked: boolean
  }
  /** True for ADMIN or a user whose departments include QC (see roleAccess.ts's
   * hasDepartmentAccess / DashboardPage) - hides the tab entirely for a QA-only user rather than
   * just blocking it if somehow navigated to. */
  showDashboardTab?: boolean
  /** True for ADMIN or a user whose departments include QA (see roleAccess.ts's
   * hasDepartmentAccess / DashboardPage) - hides the tab entirely for everyone else rather than
   * just being blocked if somehow navigated to. */
  showQaTab?: boolean
}) {
  return (
    <div className="flex items-center justify-between border-b border-border px-6 py-2 md:px-10 lg:px-20">
      {/* Both dashboards (QC, QA) sit together first, Performance last - keeps the two "pick a
      dashboard" tabs adjacent instead of splitting them across Performance. */}
      <div className="flex items-center gap-4 text-sm">
        {showDashboardTab && (
          <button
            onClick={() => onViewChange('dashboard')}
            className={view === 'dashboard' ? 'font-semibold text-brand' : 'text-muted-foreground hover:text-foreground'}
          >
            QC Dashboard
          </button>
        )}
        {showDashboardTab && showQaTab && <NavDivider className="bg-border" />}
        {showQaTab && (
          <button
            onClick={() => onViewChange('qa')}
            className={view === 'qa' ? 'font-semibold text-brand' : 'text-muted-foreground hover:text-foreground'}
          >
            QA Dashboard
          </button>
        )}
        {(showDashboardTab || showQaTab) && <NavDivider className="bg-border" />}
        <button
          onClick={() => onViewChange('performance')}
          className={view === 'performance' ? 'font-semibold text-brand' : 'text-muted-foreground hover:text-foreground'}
        >
          Performance
        </button>
      </div>
      <div className="flex items-center gap-4">
        <DataFreshnessLabel />
        {secondaryToggle && (
          <SubNavLensToggle
            value={secondaryToggle.value}
            options={secondaryToggle.options}
            onChange={secondaryToggle.onChange}
            locked={secondaryToggle.locked}
          />
        )}
        {/* Not yet built (see KpiDetailModal's "Raise Concern" for the same disabled-but-visible convention). */}
        <Button size="sm" className="gap-1.5" variant="secondary" disabled title="Not yet built">
          <Bell className="size-3.5" />
          Alerts &amp; Activities
          <Badge variant="secondary" className="bg-foreground/10 text-muted-foreground">
            3
          </Badge>
        </Button>
      </div>
    </div>
  )
}
