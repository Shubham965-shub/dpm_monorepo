import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { QC_SCORE_MOCK, SECTION_SCORE_MOCK } from './mockData'
import { DemoDataBadge } from './DemoDataBadge'

// Exact hex values pulled from Figma (file T5oSGlK2eTYF8KvKP5M9lF, node 5024:20119 "Frame 128")
// via get_design_context - not eyeballed from a screenshot.
export function QcScoreCard() {
  const { overall, label, bestEver } = QC_SCORE_MOCK
  const breakdown = Object.values(SECTION_SCORE_MOCK)

  return (
    <Card className="gap-0 overflow-hidden py-0">
      <div className="h-1.5 w-full" style={{ backgroundColor: '#ffb900' }} />
      <CardHeader className="pt-5">
        <CardTitle className="flex items-center gap-2 text-[10px] font-bold tracking-[1px] uppercase" style={{ color: '#99a1af' }}>
          QC Score · KRSG Operations
          <DemoDataBadge />
        </CardTitle>
      </CardHeader>
      <CardContent className="pb-5">
        <div className="flex items-end gap-3 pt-3">
          <span className="text-[60px] leading-none font-black" style={{ color: '#bb4d00' }}>
            {overall}
          </span>
          <div className="flex flex-col gap-1.5 pb-1">
            <span className="text-lg" style={{ color: '#99a1af' }}>
              / 100
            </span>
            <span
              className="w-fit rounded-full border px-2 py-1 text-[10px] font-bold"
              style={{ backgroundColor: '#fffbeb', borderColor: '#fee685', color: '#bb4d00' }}
            >
              {label}
            </span>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-4 gap-2 border-t border-border pt-4">
          {breakdown.map((b) => (
            <div key={b.shortLabel} className="flex flex-col items-center text-center">
              <div className="text-xl font-black" style={{ color: b.numberHex }}>
                {b.score}
              </div>
              <div className="pt-0.5 text-[8px] font-bold tracking-wide uppercase" style={{ color: '#99a1af' }}>
                {b.shortLabel}
              </div>
              <div className="mt-1 h-1 w-full rounded-full" style={{ backgroundColor: b.barHex }} />
            </div>
          ))}
        </div>

        <div className="mt-3 flex items-center justify-between border-t border-border pt-3 text-[10px]">
          <span style={{ color: '#99a1af' }}>
            Best ever: <strong style={{ color: '#364153' }}>{bestEver.score}</strong> · {bestEver.period}
          </span>
          <a href="#" className="text-brand text-xs font-semibold hover:underline">
            View breakdown →
          </a>
        </div>
      </CardContent>
    </Card>
  )
}
