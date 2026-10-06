import { Check, TrendingUp } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { POSITIVE_SIGNALS_MOCK } from './mockData'
import { DemoDataBadge, UPCOMING_FEATURE_CLASS } from './DemoDataBadge'

// Exact hex/sizing pulled from Figma (file T5oSGlK2eTYF8KvKP5M9lF, node 5024:20119 "Frame 128")
// via get_design_context - not eyeballed from a screenshot. text-brand/bg-brand match Figma's
// #028662 exactly (see index.css's --brand).
export function PositiveSignals() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-brand flex items-center gap-2 text-xs font-bold tracking-[0.3px] uppercase">
          <TrendingUp className="size-3.5" />
          Positive Signals This Period
          <DemoDataBadge />
        </CardTitle>
      </CardHeader>
      <CardContent className={cn('flex flex-col gap-2', UPCOMING_FEATURE_CLASS)}>
        {POSITIVE_SIGNALS_MOCK.map((signal) => (
          <div
            key={signal.title}
            className="flex items-start gap-3 rounded-xl border border-border bg-card px-4 py-3"
          >
            <span className="bg-brand mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full text-white">
              <Check className="size-3" />
            </span>
            <div>
              <div className="text-xs font-semibold" style={{ color: '#1e2939' }}>
                {signal.title}
              </div>
              <div className="pt-0.5 text-[11px]" style={{ color: '#6a7282' }}>
                {signal.detail}
              </div>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
