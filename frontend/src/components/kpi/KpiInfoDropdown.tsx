import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { useKpiInfo } from '@/hooks/useKpiInfo'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { SEVERITY_META } from '@/components/dashboard/types'
import { cn } from '@/lib/utils'

/**
 * A light "markdown" renderer for a KPI's `summary` text (qc_kpi_info / qa_kpi_info) - just the
 * conventions both KPI Logic sheets actually use: a `**heading**`-only line, a `Source: X` line
 * (styled the same green accent as the dedicated `sourceSystem` field below), a `• ` bullet line
 * (grouped into one <ul>), and everything else as a plain paragraph. Not a general markdown parser -
 * there's no need for one beyond these four line shapes. Plain pre-`**`-convention text (an older
 * hand-written qc_kpi_info row this sheet hasn't touched yet) still renders fine here too - every
 * line just falls into the plain-paragraph case.
 */
function renderInfoText(text: string) {
  const lines = text.split('\n')
  const blocks: { type: 'heading' | 'source' | 'bullets' | 'text'; content: string | string[] }[] = []
  for (const raw of lines) {
    const line = raw.trim()
    if (!line) continue
    const headingMatch = /^\*\*(.+)\*\*$/.exec(line)
    if (headingMatch) {
      blocks.push({ type: 'heading', content: headingMatch[1] })
    } else if (line.startsWith('Source:')) {
      blocks.push({ type: 'source', content: line })
    } else if (line.startsWith('• ')) {
      const last = blocks.at(-1)
      if (last?.type === 'bullets') (last.content as string[]).push(line.slice(2))
      else blocks.push({ type: 'bullets', content: [line.slice(2)] })
    } else {
      blocks.push({ type: 'text', content: line })
    }
  }
  return blocks.map((block, i) => {
    const key = `${block.type}-${i}`
    if (block.type === 'heading') return <p key={key} className="font-semibold text-foreground">{block.content as string}</p>
    if (block.type === 'source') {
      return (
        <span key={key} className="font-medium" style={{ color: SEVERITY_META['in-control'].solidHex }}>
          {block.content as string}
        </span>
      )
    }
    if (block.type === 'bullets') {
      return (
        <ul key={key} className="list-disc pl-4">
          {(block.content as string[]).map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      )
    }
    return (
      <p key={key} className="text-muted-foreground leading-relaxed">
        {block.content as string}
      </p>
    )
  })
}

/**
 * "Info ▾" in an expanded KPI view - hovering unfolds the KPI's source system and summary inline
 * above it, pushing the header down rather than floating over the chart. A click pins it open (and
 * unpins), which is also how touch and keyboard users open it.
 *
 * Fetched via useKpiInfo(domain, kpi.id) - QC domains read qc_kpi_info (via /api/kpi-thresholds),
 * QA domains read qa_kpi_info (via /api/qa-kpi-info, see kpiConfig.ts's KpiDomain.infoBasePath) -
 * both tables, both rendered the same way here.
 */
export function KpiInfoDropdown({ domain, kpi, className }: { domain: KpiDomain; kpi: KpiDefinition; className?: string }) {
  // Kept apart so a click never undoes the hover: hovering shows it, clicking pins/unpins it.
  const [hovered, setHovered] = useState(false)
  const [pinned, setPinned] = useState(false)
  const open = hovered || pinned
  // Fetched with the view (not on hover) so the panel never opens onto a "Loading…" flash.
  const { data, isLoading, isError } = useKpiInfo(domain, kpi.id)
  const hasInfo = Boolean(data?.sourceSystem || data?.summary)

  return (
    <div
      className={cn('flex max-w-xl flex-col gap-2 border-b border-border pb-1', className)}
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
          {data?.summary && renderInfoText(data.summary)}
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
