import { Sun, Moon, Bell } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import heliosLogo from '@/assets/helios-logo.svg'
import stridesLogo from '@/assets/strides-logo.svg'
import { useTheme } from '@/hooks/useTheme'
import { cn, initials } from '@/lib/utils'
import type { UserPublic } from '@/lib/authSchemas'
import type { RoleTab, OperatorSupervisorView } from '@/lib/roleAccess'

/** A real vertical bar, not a text "|" glyph - matches InvestigationAI's header divider. */
function NavDivider({ className }: { className?: string }) {
  return <div className={cn('h-6 w-px bg-white/20', className)} />
}

// Wireframe shows CXO / Leadership / Operations tabs, which don't map onto the backend's fixed
// Role enum (CXO/HOD/MANAGER/SUPERVISOR/ANALYST/ADMIN) 1:1 - each renders the same dashboard
// shell, filtered by that tab's own KPI Framework-derived lens (see SectionSummaryList's
// KPI_VISIBILITY). Which tab a user lands on, and whether they can switch, is derived from their
// real roles (see lib/roleAccess.ts) - only ADMIN gets the switchable nav shown here (and can
// preview any of the 4 lens variants: CXO, Leadership, Operator, Supervisor); everyone else is
// locked to one tab with no nav to the others (see `locked` below).
const ROLE_TABS: RoleTab[] = ['CXO', 'Leadership', 'Operations']

export function TopNav({
  user,
  onLogout,
  activeRole,
  onActiveRoleChange,
  locked,
}: {
  user: UserPublic
  onLogout: () => void
  activeRole: RoleTab
  onActiveRoleChange: (role: RoleTab) => void
  /** True for every role except ADMIN (see resolveRoleTab) - renders activeRole as plain text
   * instead of a switchable pill row, since the user has no other tab to switch to anyway. */
  locked: boolean
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
        <button
          onClick={onLogout}
          title={user.email}
          className="flex size-7 items-center justify-center rounded-full bg-brand text-xs font-semibold"
        >
          {initials(user.fullName)}
        </button>
      </nav>
    </header>
  )
}

export type SubNavView = 'dashboard' | 'performance'

const OPERATIONS_LENS_OPTIONS: { value: OperatorSupervisorView; label: string }[] = [
  { value: 'operator', label: 'Operator' },
  { value: 'supervisor', label: 'Supervisor' },
]

/** Only rendered inside the Operations role tab (see DashboardPage) - Operator/Supervisor each
 * gate the KPI catalogue down to a different role-visibility list from the KPI Framework spec
 * (see SectionSummaryList's OPERATOR_VISIBLE_KPI_IDS/SUPERVISOR_VISIBLE_KPI_IDS), not a "some
 * filter vs no filter" toggle - both are real, separately-scoped lists. `locked` (true for
 * SUPERVISOR/ANALYST - see resolveOperatorSupervisorView) renders the current value as plain text
 * instead of a switchable control, since those roles have no other list to switch to. */
function OperatorSupervisorToggle({
  value,
  onChange,
  locked,
}: {
  value: OperatorSupervisorView
  onChange: (value: OperatorSupervisorView) => void
  locked: boolean
}) {
  if (locked) {
    return (
      <span className="rounded-md border border-input px-3 py-1 text-xs font-medium capitalize text-muted-foreground">
        {value}
      </span>
    )
  }
  return (
    <div className="flex overflow-hidden rounded-md border border-input text-xs font-medium">
      {OPERATIONS_LENS_OPTIONS.map((option) => (
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
  operatorSupervisorView,
  onOperatorSupervisorViewChange,
  operatorSupervisorLocked,
}: {
  view: SubNavView
  onViewChange: (view: SubNavView) => void
  /** undefined = not in the Operations role tab - hides the toggle entirely rather than showing a meaningless default. */
  operatorSupervisorView?: OperatorSupervisorView
  onOperatorSupervisorViewChange?: (value: OperatorSupervisorView) => void
  operatorSupervisorLocked?: boolean
}) {
  return (
    <div className="flex items-center justify-between border-b border-border px-6 py-2 md:px-10 lg:px-20">
      <div className="flex items-center gap-4 text-sm">
        <button
          onClick={() => onViewChange('dashboard')}
          className={view === 'dashboard' ? 'font-semibold text-brand' : 'text-muted-foreground hover:text-foreground'}
        >
          Dashboard
        </button>
        <NavDivider className="bg-border" />
        <button
          onClick={() => onViewChange('performance')}
          className={view === 'performance' ? 'font-semibold text-brand' : 'text-muted-foreground hover:text-foreground'}
        >
          Performance
        </button>
      </div>
      <div className="flex items-center gap-4">
        {operatorSupervisorView && onOperatorSupervisorViewChange && (
          <OperatorSupervisorToggle
            value={operatorSupervisorView}
            onChange={onOperatorSupervisorViewChange}
            locked={operatorSupervisorLocked ?? false}
          />
        )}
        <Button size="sm" className="gap-1.5 bg-brand hover:bg-brand/90" variant="default">
          <Bell className="size-3.5" />
          Alerts &amp; Activities
          <Badge variant="secondary" className="bg-white/20 text-white">
            3
          </Badge>
        </Button>
      </div>
    </div>
  )
}
