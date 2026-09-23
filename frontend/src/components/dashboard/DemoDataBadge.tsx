import { FlaskConical } from 'lucide-react'
import { cn } from '@/lib/utils'

/** Visible marker for a card whose content is placeholder data, not wired to a real backend
 * endpoint - so a viewer never mistakes it for a live number. */
export function DemoDataBadge({ className }: { className?: string }) {
  return (
    <span
      title="Placeholder data - not wired to a real backend endpoint yet"
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
