import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { QC_SCORE_MOCK, SECTION_SCORE_MOCK } from './mockData'

export function QcScoreCard() {
  const { overall, label, bestEver } = QC_SCORE_MOCK
  const breakdown = Object.values(SECTION_SCORE_MOCK)

  return (
    <Card className="border-t-4 border-t-amber-500">
      <CardHeader>
        <CardTitle className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
          QC Score · KRSG Operations
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-baseline gap-2">
          <span className="text-5xl font-bold text-amber-700">{overall}</span>
          <span className="text-muted-foreground">/ 100</span>
        </div>
        <span className="mt-1 inline-block rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
          {label}
        </span>

        <div className="mt-4 grid grid-cols-4 gap-2 border-t border-border pt-3">
          {breakdown.map((b) => (
            <div key={b.shortLabel} className="text-center">
              <div className="text-lg font-semibold">{b.score}</div>
              <div className="text-[11px] tracking-wide text-muted-foreground uppercase">
                {b.shortLabel}
              </div>
            </div>
          ))}
        </div>

        <div className="mt-3 flex items-center justify-between border-t border-border pt-3 text-xs text-muted-foreground">
          <span>
            Best ever: <strong className="text-foreground">{bestEver.score}</strong> · {bestEver.period}
          </span>
          <a href="#" className="font-medium text-primary hover:underline">
            View breakdown →
          </a>
        </div>
      </CardContent>
    </Card>
  )
}
