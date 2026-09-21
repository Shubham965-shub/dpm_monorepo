import { ChevronDown } from 'lucide-react'

// Visual-only for now: swap for a real Select once the endpoints these filters drive
// (plant/section/shift/date-range query params, per each controller) are decided.
const FILTERS = [
  { label: 'Plant', value: 'KRSG' },
  { label: 'Section', value: 'All sections' },
  { label: 'Shift', value: 'All' },
  { label: 'Timeline', value: 'T-30' },
]

export function FilterBar() {
  return (
    <div className="flex gap-3 px-6 py-3">
      {FILTERS.map((f) => (
        <div
          key={f.label}
          className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm"
        >
          <span className="text-muted-foreground">{f.label}:</span>
          <span className="font-medium">{f.value}</span>
          <ChevronDown className="size-3.5 text-muted-foreground" />
        </div>
      ))}
    </div>
  )
}
