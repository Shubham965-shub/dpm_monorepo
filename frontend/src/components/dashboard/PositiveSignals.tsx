import { Check } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { POSITIVE_SIGNALS_MOCK } from './mockData'

export function PositiveSignals() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
          Positive Signals This Period
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        {POSITIVE_SIGNALS_MOCK.map((signal) => (
          <div
            key={signal.title}
            className="flex items-start gap-2 rounded-lg border border-border p-3"
          >
            <span className="mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
              <Check className="size-3.5" />
            </span>
            <div>
              <div className="text-sm font-medium">{signal.title}</div>
              <div className="text-xs text-muted-foreground">{signal.detail}</div>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
