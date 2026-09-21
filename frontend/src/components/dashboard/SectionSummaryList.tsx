import { useState } from 'react'
import { useHeatmapData } from '@/hooks/useHeatmapData'
import { SECTION_SCORE_MOCK } from './mockData'
import { SectionSummaryBar } from './SectionSummaryBar'

export function SectionSummaryList({ site }: { site: string }) {
  const { sections } = useHeatmapData(site)
  const [expandedKey, setExpandedKey] = useState<string | null>('lab-performance')

  return (
    <div className="flex flex-col gap-3">
      {sections.map((section) => {
        const meta = SECTION_SCORE_MOCK[section.key]
        let critical = 0
        let warning = 0
        let onTrack = 0
        // Tier counts are real, derived from the latest month of the already-wired heatmap data
        // (see useHeatmapData) - not the wireframe's placeholder numbers, which didn't match the
        // real KPI catalogue's per-section counts anyway.
        for (const row of section.rows) {
          const latest = row.cells.at(-1)?.status
          if (latest === 'action') critical++
          else if (latest === 'alert') warning++
          else if (latest === 'in-control') onTrack++
        }
        const expanded = expandedKey === section.key

        return (
          <div key={section.key}>
            <SectionSummaryBar
              category={meta.category}
              title={section.title}
              critical={critical}
              warning={warning}
              onTrack={onTrack}
              score={meta.score}
              deltaPts={meta.deltaPts}
              starred={meta.starred}
              expanded={expanded}
              onToggle={() => setExpandedKey((k) => (k === section.key ? null : section.key))}
            />
            {expanded && (
              <p className="px-4 py-2 text-xs text-muted-foreground">
                Per-KPI trend-chart breakdown for this section isn't built yet.
              </p>
            )}
          </div>
        )
      })}
    </div>
  )
}
