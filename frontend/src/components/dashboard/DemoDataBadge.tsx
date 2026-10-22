import { Rocket } from 'lucide-react'
import { cn } from '@/lib/utils'

/** Dims and disables whatever sits beside a DemoDataBadge - wrap the placeholder content (not the
 * badge itself) in a div with this className. pointer-events-none on top of the dimming since
 * every mock action in these sections (buttons, links) can't do anything real yet either. */
export const UPCOMING_FEATURE_CLASS = 'pointer-events-none opacity-50 grayscale'

/** Visible marker for a card whose content is placeholder, not wired to a real backend endpoint
 * yet - so a viewer never mistakes it for a live number, and the greyed-out content beside it
 * (see each usage site) reads as "coming later" rather than "broken". `title` overrides the
 * default tooltip for a card that's only partially placeholder (e.g. TeamWorkloadOverview's
 * roster is real, just not its per-person workload numbers). */
export function DemoDataBadge({ className, title = 'Upcoming feature - not wired to a real backend endpoint yet' }: { className?: string; title?: string }) {
  return (
    <span
      title={title}
      className={cn(
        'inline-flex w-fit items-center gap-1 rounded-full bg-violet-100 px-2 py-0.5 text-[10px] font-semibold text-violet-700',
        className,
      )}
    >
      <Rocket className="size-3" />
      Upcoming feature
    </span>
  )
}
