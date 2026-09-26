import { useState } from 'react'
import { cn } from '@/lib/utils'
import { ALERT_LIMIT_HEX, ACTION_LIMIT_HEX } from './types'
import { LegendEntry } from './LegendEntry'
import { DemoDataBadge } from './DemoDataBadge'

const TIMELINE_OPTIONS = ['1D', '3D', '1W'] as const
const CHART_OPTIONS = ['Line', 'Bar', 'Area'] as const

function Pill<T extends string>({ options, value, onChange }: { options: readonly T[]; value: T; onChange: (v: T) => void }) {
  return (
    <div className="flex overflow-hidden rounded-md border border-input text-xs font-medium">
      {options.map((option) => (
        <button
          key={option}
          onClick={() => onChange(option)}
          className={cn('px-2.5 py-1', value === option ? 'bg-foreground text-background' : 'text-muted-foreground hover:text-foreground')}
        >
          {option}
        </button>
      ))}
    </div>
  )
}

/** Matches the persona lens views' reference screenshots exactly (Operator/Supervisor), but neither
 * pill is wired to any real query - unlike SeverityKpiCard's own D/W/M/Q/Y grain and Line/Bar
 * toggles (which do drive that card's chart), this page-level row has no matching real
 * granularity/chart-type concept below it to control, so DemoDataBadge flags it as display-only. */
export function TimelineChartControls() {
  const [timeline, setTimeline] = useState<(typeof TIMELINE_OPTIONS)[number]>('1W')
  const [chart, setChart] = useState<(typeof CHART_OPTIONS)[number]>('Line')

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-1 text-xs">
      <div className="flex flex-wrap items-center gap-4">
        <span className="flex items-center gap-2 font-semibold tracking-wide text-muted-foreground uppercase">
          Timeline:
          <Pill options={TIMELINE_OPTIONS} value={timeline} onChange={setTimeline} />
        </span>
        <span className="flex items-center gap-2 font-semibold tracking-wide text-muted-foreground uppercase">
          Chart:
          <Pill options={CHART_OPTIONS} value={chart} onChange={setChart} />
        </span>
        <DemoDataBadge />
      </div>
      <div className="flex items-center gap-3">
        <LegendEntry colorHex={ALERT_LIMIT_HEX} label="Alert limit" />
        <LegendEntry colorHex={ACTION_LIMIT_HEX} label="Action limit" />
      </div>
    </div>
  )
}
