import { useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardAction } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { useHeatmapData } from '@/hooks/useHeatmapData'
import { RagDot } from './RagDot'
import type { HeatmapRow } from './types'

// Only 3M is wired to real data (trailing 3 calendar months - see useHeatmapData). 4M/5M are
// kept visible to match the wireframe but disabled until a longer range is actually decided.
const MONTH_RANGES = ['3M', '4M', '5M'] as const

function SectionGrid({ title, rows }: { title: string; rows: HeatmapRow[] }) {
  return (
    <div className="min-w-56 flex-1">
      <div className="rounded-t-md bg-foreground px-3 py-1.5 text-xs font-semibold text-background">
        <div className="flex items-center justify-between gap-2">
          <span>{title}</span>
          <span className="font-normal opacity-70">{rows.length} KPIs</span>
        </div>
      </div>
      <div className="rounded-b-md border border-t-0 border-border px-3 py-2">
        <div className="mb-1 flex gap-4 text-[11px] text-muted-foreground">
          {rows[0]?.cells.map((c) => (
            <span key={c.month} className="w-6 text-center">
              {c.month}
            </span>
          ))}
        </div>
        <div className="flex flex-col gap-1.5">
          {rows.map((row) => (
            <div key={row.slug} className="flex gap-4" title={row.name}>
              {row.cells.map((cell) => (
                <span key={cell.month} className="flex w-6 items-center justify-center">
                  <RagDot status={cell.status} />
                </span>
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export function OverallHeatmap({ site }: { site: string }) {
  const [onlyReds, setOnlyReds] = useState(false)
  const [range, setRange] = useState<(typeof MONTH_RANGES)[number]>('3M')
  const { sections, isLoading, isError } = useHeatmapData(site)

  return (
    <Card size="sm">
      <CardHeader>
        <CardTitle className="text-sm font-semibold tracking-wide text-muted-foreground uppercase">
          Overall Heatmap
        </CardTitle>
        <CardAction className="flex items-center gap-2">
          <Button
            size="sm"
            variant={onlyReds ? 'default' : 'outline'}
            onClick={() => setOnlyReds((v) => !v)}
          >
            Only reds
          </Button>
          {MONTH_RANGES.map((r) => (
            <Button
              key={r}
              size="sm"
              variant={r === range ? 'default' : 'outline'}
              disabled={r !== '3M'}
              title={r !== '3M' ? 'Not wired to real data yet - only the trailing 3 months is' : undefined}
              onClick={() => setRange(r)}
            >
              {r}
            </Button>
          ))}
        </CardAction>
      </CardHeader>
      <CardContent>
        <div className="mb-3 flex items-center gap-4 text-xs text-muted-foreground">
          <span className="flex items-center gap-1.5">
            <RagDot status="in-control" /> In Control
          </span>
          <span className="flex items-center gap-1.5">
            <RagDot status="alert" /> Alert Limit
          </span>
          <span className="flex items-center gap-1.5">
            <RagDot status="action" /> Action Limit
          </span>
          <span className="flex items-center gap-1.5">
            <RagDot status="no-data" /> No data
          </span>
        </div>
        {isError && (
          <p className="mb-2 text-xs text-red-600">
            Some KPIs failed to load - showing whatever came back successfully.
          </p>
        )}
        <div className="flex flex-wrap gap-4">
          {isLoading && sections.every((s) => s.rows.length === 0) ? (
            <p className="text-sm text-muted-foreground">Loading KPI data…</p>
          ) : (
            sections.map((section) => (
              <SectionGrid
                key={section.title}
                title={section.title}
                rows={
                  onlyReds
                    ? section.rows.filter((r) => r.cells.some((c) => c.status === 'action'))
                    : section.rows
                }
              />
            ))
          )}
        </div>
      </CardContent>
    </Card>
  )
}
