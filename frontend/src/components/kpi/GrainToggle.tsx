import type { Grain } from '@/lib/kpiSchemas'
import { cn } from '@/lib/utils'

const GRAIN_OPTIONS: { value: Grain; label: string }[] = [
  { value: 'daily', label: 'D' },
  { value: 'weekly', label: 'W' },
  { value: 'monthly', label: 'M' },
  { value: 'quarterly', label: 'Q' },
  { value: 'yearly', label: 'Y' },
]

/** The D/W/M/Q/Y switch - 'card' for a compact card's header, 'modal' for an expanded view. */
export function GrainToggle({
  grain,
  onChange,
  activeHex,
  borderHex,
  size = 'card',
}: {
  grain: Grain
  onChange: (grain: Grain) => void
  /** Selected button's fill - the card's severity colour. */
  activeHex: string
  /** Card size only; the modal uses the input border. */
  borderHex?: string
  size?: 'card' | 'modal'
}) {
  return (
    <div
      className={cn('flex overflow-hidden rounded-md border', size === 'modal' && 'border-input')}
      style={size === 'card' ? { borderColor: borderHex } : undefined}
    >
      {GRAIN_OPTIONS.map((g) => (
        <button
          key={g.value}
          type="button"
          onClick={() => onChange(g.value)}
          className={cn(
            'font-bold',
            size === 'card' ? 'px-1.5 py-0.5 text-[8px]' : 'px-2.5 py-1 text-xs',
            g.value !== grain && 'text-muted-foreground',
          )}
          style={g.value === grain ? { backgroundColor: activeHex, color: 'white' } : undefined}
        >
          {g.label}
        </button>
      ))}
    </div>
  )
}
