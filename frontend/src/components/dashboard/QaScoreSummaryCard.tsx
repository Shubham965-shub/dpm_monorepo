import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { generateMockSectionScore, qaCompositeScore } from '@/lib/qaSectionScore'
import type { KpiDomain } from '@/lib/kpiConfig'
import { DEFAULT_SITE } from '@/lib/kpiConfig'
import { bandSeverity, formatScore, scoreBandLabel, scoreBandMeta, SEVERITY_ORDER } from './types'
import { DemoDataBadge } from './DemoDataBadge'

// A band purely for the demo number's own visual coloring (green/amber/red) - not a real KPI
// threshold claim, since nothing here is measured against a confirmed limit yet (see
// qaSectionScore.ts). Mirrors typical scorecard bands closely enough to read sensibly as a demo.
function demoBand(score: number): 'GREEN' | 'AMBER' | 'RED' {
  if (score >= 85) return 'GREEN'
  if (score >= 70) return 'AMBER'
  return 'RED'
}

/**
 * QA's composite-score equivalent of QcScoreCard - every number here is generateMockSectionScore's
 * deterministic demo output, not a real measurement (see qaSectionScore.ts's own doc comment for
 * why: no real QA threshold data exists yet). Cross-Functional Context is never included (see
 * `scoredDomains` below / kpiConfig.ts's KpiDomain.scored) - it carries no score at all, matching
 * the sheet's "excluded from any roll-up that averages or totals the section scores" rule.
 */
export function QaScoreSummaryCard({ domains }: { domains: KpiDomain[] }) {
  const scoredDomains = domains.filter((d) => d.scored ?? true)
  const composite = qaCompositeScore(scoredDomains)
  const band = demoBand(composite.score)
  const meta = scoreBandMeta(band)

  return (
    <Card className="gap-0 overflow-hidden py-0">
      <div className="h-1.5 w-full" style={{ backgroundColor: meta.solidHex }} />
      <CardHeader className="pt-5">
        <CardTitle className="flex items-center gap-2 text-[10px] font-bold tracking-[1px] uppercase" style={{ color: '#99a1af' }}>
          QA Score · {DEFAULT_SITE} Operations
          <DemoDataBadge title="Demo score - no real QA threshold data exists yet (see qaSectionScore.ts)" />
        </CardTitle>
      </CardHeader>
      <CardContent className="pb-5">
        <div className="flex items-end gap-3 pt-3">
          <span className="text-[60px] leading-none font-black" style={{ color: meta.solidHex }}>
            {formatScore(composite.score)}
          </span>
          <div className="flex flex-col gap-1.5 pb-1">
            <span className="text-lg" style={{ color: '#99a1af' }}>
              / 100
            </span>
            <span
              className="w-fit rounded-full border px-2 py-1 text-[10px] font-bold"
              style={{ backgroundColor: meta.bandBg, borderColor: meta.paleHex, color: meta.solidHex }}
            >
              {scoreBandLabel(band)}
            </span>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-4 gap-2 border-t border-border pt-4">
          {scoredDomains
            .map((domain) => ({ domain, section: generateMockSectionScore(domain) }))
            .sort((a, b) => {
              const aBand = demoBand(a.section.score)
              const bBand = demoBand(b.section.score)
              const severityRank = (s: typeof aBand) => {
                const severity = bandSeverity(s)
                return severity ? SEVERITY_ORDER.indexOf(severity) : SEVERITY_ORDER.length
              }
              return severityRank(aBand) - severityRank(bBand) || a.section.score - b.section.score
            })
            .map(({ domain, section }) => {
              const sectionMeta = scoreBandMeta(demoBand(section.score))
              return (
                <div key={domain.key} className="flex flex-col items-center text-center">
                  <div className="text-xl font-black" style={{ color: sectionMeta.solidHex }}>
                    {formatScore(section.score)}
                  </div>
                  <div className="pt-0.5 text-[8px] font-bold tracking-wide uppercase" style={{ color: '#99a1af' }}>
                    {domain.shortLabel}
                  </div>
                  <div className="mt-1 h-1 w-full rounded-full" style={{ backgroundColor: sectionMeta.solidHex }} />
                </div>
              )
            })}
        </div>
      </CardContent>
    </Card>
  )
}
