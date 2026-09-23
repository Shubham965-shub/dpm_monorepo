import { Sun, Moon, Bell } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import heliosLogo from '@/assets/helios-logo.png'
import stridesLogo from '@/assets/strides-logo.svg'
import { useTheme } from '@/hooks/useTheme'
import { cn } from '@/lib/utils'
import type { UserPublic } from '@/lib/authSchemas'

function initials(fullName: string): string {
  const parts = fullName.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts.at(-1)?.[0] ?? '')).toUpperCase()
}

/** A real vertical bar, not a text "|" glyph - matches InvestigationAI's header divider. */
function NavDivider({ className }: { className?: string }) {
  return <div className={cn('h-6 w-px bg-white/20', className)} />
}

// Wireframe shows CXO / Leadership / Operations tabs, which don't map onto the backend's fixed
// Role enum (CXO/HOD/MANAGER/SUPERVISOR/ANALYST/ADMIN) - kept visual-only until there's routing
// and a decision on how roles translate to these views.
const ROLE_TABS = ['CXO', 'Leadership', 'Operations']

export function TopNav({ user, onLogout }: { user: UserPublic; onLogout: () => void }) {
  const { theme, toggleTheme } = useTheme()

  return (
    <header className="flex items-center justify-between border-b border-brand bg-brand-dark px-6 py-3 text-white md:px-10 lg:px-20">
      <div className="flex items-center gap-3">
        <img src={stridesLogo} alt="Strides" className="h-6 w-auto" />
        <NavDivider />
        <span className="text-lg font-semibold">Helios</span>
        <img src={heliosLogo} alt="Helios" className="size-6" />
      </div>
      <nav className="flex items-center gap-6 text-sm">
        <button
          onClick={toggleTheme}
          title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          className="flex items-center justify-center opacity-80 hover:opacity-100"
        >
          {theme === 'dark' ? <Moon className="size-4" /> : <Sun className="size-4" />}
        </button>
        {ROLE_TABS.map((tab, i) => (
          <span
            key={tab}
            className={
              i === 0
                ? 'rounded-full bg-brand px-3 py-1 font-medium'
                : 'text-white/80 hover:text-white'
            }
          >
            {tab}
          </span>
        ))}
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

export function SubNav({
  view,
  onViewChange,
}: {
  view: SubNavView
  onViewChange: (view: SubNavView) => void
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
      <Button size="sm" className="gap-1.5 bg-brand hover:bg-brand/90" variant="default">
        <Bell className="size-3.5" />
        Alerts &amp; Activities
        <Badge variant="secondary" className="bg-white/20 text-white">
          3
        </Badge>
      </Button>
    </div>
  )
}
