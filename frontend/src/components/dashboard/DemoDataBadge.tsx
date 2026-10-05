import { FlaskConical } from 'lucide-react'
import { cn } from '@/lib/utils'

/** Visible marker for a card whose content is placeholder data, not wired to a real backend
 * endpoint - so a viewer never mistakes it for a live number. `title` overrides the default
 * tooltip for a card that's only partially placeholder (e.g. TeamWorkloadOverview's roster is
 * real, just not its per-person workload numbers). */
export function DemoDataBadge({ className, title = 'Placeholder data - not wired to a real backend endpoint yet' }: { className?: string; title?: string }) {
  return (
    <span
      title={title}
      className={cn(
        'inline-flex w-fit items-center gap-1 rounded-full bg-violet-100 px-2 py-0.5 text-[10px] font-semibold text-violet-700',
        className,
      )}
    >
      <FlaskConical className="size-3" />
      Demo data
    </span>
  )
}
