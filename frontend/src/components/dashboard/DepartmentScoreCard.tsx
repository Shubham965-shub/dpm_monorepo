import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { KpiDomain } from '@/lib/kpiConfig'
import type { QcScore } from '@/lib/kpiSchemas'
import { bandSeverity, formatScore, scoreBandLabel, scoreBandMeta, SEVERITY_ORDER } from './types'

/**
 * The composite-score card (plant score + one tile per scored section), given the score to show: QA's comes from
 * GET /api/qa-score (see QaScoreSummaryCard), and a department with no scoring yet passes none and a `noDataLabel`
 * (see AdminDepartmentDashboardPage). Cross-Functional Context is never included (see `scoredDomains` below /
 * kpiConfig.ts's KpiDomain.scored) - it carries no score at all, matching the sheet's "excluded from any roll-up that
 * averages or totals the section scores" rule.
 */
export function DepartmentScoreCard({
  title,
  site,
  domains,
  data,
  isLoading = false,
  isError = false,
  noDataLabel,
}: {
  /** "QA Score", "Manufacturing Score" - shown above the site. */
  title: string
  site: string
  domains: KpiDomain[]
  data?: QcScore
  isLoading?: boolean
  isError?: boolean
  /** Overrides the band's own "No data" wording when there is no score to show. */
  noDataLabel?: string
}) {
  const scoredDomains = domains.filter((d) => d.scored ?? true)
  const plantBand = data?.band ?? null
  const meta = scoreBandMeta(plantBand)
  const statusLabel = isLoading ? 'Loading…' : isError ? 'Unavailable' : data == null && noDataLabel ? noDataLabel : scoreBandLabel(plantBand)

  return (
    <Card className="gap-0 overflow-hidden py-0">
      <div className="h-1.5 w-full" style={{ backgroundColor: meta.solidHex }} />
      <CardHeader className="pt-5">
        <CardTitle className="flex items-center gap-2 text-[10px] font-bold tracking-[1px] uppercase" style={{ color: '#99a1af' }}>
          {title} · {site} Operations
        </CardTitle>
      </CardHeader>
      <CardContent className="pb-5">
        <div className="flex items-end gap-3 pt-3">
          <span className="text-[60px] leading-none font-black" style={{ color: meta.solidHex }}>
            {formatScore(data?.score)}
          </span>
          <div className="flex flex-col gap-1.5 pb-1">
            <span className="text-lg" style={{ color: '#99a1af' }}>
              / 100
            </span>
            <span
              className="w-fit rounded-full border px-2 py-1 text-[10px] font-bold"
              style={{ backgroundColor: meta.bandBg, borderColor: meta.paleHex, color: meta.solidHex }}
            >
              {statusLabel}
            </span>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-4 gap-2 border-t border-border pt-4">
          {/* Worst first (left), best last (right) - same rule as QcScoreCard: band severity first,
          score only as the tiebreaker within a band, no-data sections sort last. */}
          {scoredDomains
            .map((domain) => ({ domain, section: data?.sections.find((s) => s.sectionId === domain.key) }))
            .sort((a, b) => {
              const severityRank = (s: typeof a) => {
                const severity = bandSeverity(s.section?.band ?? null)
                return severity ? SEVERITY_ORDER.indexOf(severity) : SEVERITY_ORDER.length
              }
              return severityRank(a) - severityRank(b) || (a.section?.score ?? Infinity) - (b.section?.score ?? Infinity)
            })
            .map(({ domain, section }) => {
              const sectionMeta = scoreBandMeta(section?.band ?? null)
              return (
                <div key={domain.key} className="flex flex-col items-center text-center">
                  <div className="text-xl font-black" style={{ color: sectionMeta.solidHex }}>
                    {formatScore(section?.score)}
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
