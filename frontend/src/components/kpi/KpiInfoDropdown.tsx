import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { useKpiInfo } from '@/hooks/useKpiInfo'
import { SEVERITY_META } from '@/components/dashboard/types'
import { cn } from '@/lib/utils'

/**
 * "Info ▾" in an expanded KPI view - hovering unfolds the KPI's source system and summary (from
 * kpi_info) inline above it, pushing the header down rather than floating over the chart. A click
 * pins it open (and unpins), which is also how touch and keyboard users open it.
 */
export function KpiInfoDropdown({ kpiId }: { kpiId: string }) {
  // Kept apart so a click never undoes the hover: hovering shows it, clicking pins/unpins it.
  const [hovered, setHovered] = useState(false)
  const [pinned, setPinned] = useState(false)
  const open = hovered || pinned
  // Fetched with the view (not on hover) so the panel never opens onto a "Loading…" flash.
  const { data, isLoading, isError } = useKpiInfo(kpiId)
  const hasInfo = Boolean(data?.sourceSystem || data?.summary)

  return (
    <div
      className="flex max-w-xl flex-col gap-2 border-b border-border pb-1"
      // Mouse only: a touch tap fires emulated pointer events that would open and close it again.
      onPointerEnter={(e) => e.pointerType === 'mouse' && setHovered(true)}
      onPointerLeave={(e) => e.pointerType === 'mouse' && setHovered(false)}
    >
      {open && (
        <div className="flex flex-col gap-1 px-1 pt-1 text-sm">
          {isLoading && <p className="text-muted-foreground">Loading…</p>}
          {!isLoading && (isError || !hasInfo) && <p className="text-muted-foreground">No KPI info available.</p>}
          {data?.sourceSystem && (
            <span className="font-medium" style={{ color: SEVERITY_META['in-control'].solidHex }}>
              Source: {data.sourceSystem}
            </span>
          )}
          {/* pre-line: line breaks typed into kpi_info.summary show as separate lines. */}
          {data?.summary && <p className="text-muted-foreground leading-relaxed whitespace-pre-line">{data.summary}</p>}
        </div>
      )}
      <button
        type="button"
        aria-expanded={open}
        onClick={() => setPinned((p) => !p)}
        className="text-muted-foreground hover:text-foreground flex w-fit items-center gap-1 px-1 text-sm"
      >
        Info
        <ChevronDown className={cn('size-3.5 transition-transform', open && 'rotate-180')} />
      </button>
    </div>
  )
}
