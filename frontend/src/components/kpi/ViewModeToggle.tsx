import { cn } from '@/lib/utils'

export type ViewMode = 'line' | 'bar'

const VIEW_MODES: readonly ViewMode[] = ['line', 'bar']

export function ViewModeToggle({ mode, onChange, activeHex, borderHex, className }: {
  mode: ViewMode
  onChange: (mode: ViewMode) => void
  activeHex: string
  borderHex?: string
  className?: string
}) {
  return (
    <div className={cn('flex overflow-hidden rounded-md border', className)} style={borderHex ? { borderColor: borderHex } : undefined}>
      {VIEW_MODES.map((m) => (
        <button
          key={m}
          onClick={() => onChange(m)}
          className={cn('px-1.5 py-0.5 text-[8px] font-bold capitalize', m !== mode && 'text-muted-foreground')}
          style={m === mode ? { backgroundColor: activeHex, color: 'white' } : undefined}
        >
          {m}
        </button>
      ))}
    </div>
  )
}
