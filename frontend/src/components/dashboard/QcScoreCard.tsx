import { ChevronRight } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Grain } from '@/lib/kpiSchemas'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import { useQcScore } from '@/hooks/useQcScore'
import type { SectionKey } from './mockData'
import { SECTION_LABELS } from './sectionLabels'
import { bandSeverity, formatScore, scoreBandLabel, scoreBandMeta, SEVERITY_ORDER } from './types'
import { DemoDataBadge } from './DemoDataBadge'

export interface BestEverScore {
  score: number
  /** "February 2026" - already formatted, since there's no real historical series behind this yet
   * (see the `bestEver` prop) to derive a date format from. */
  period: string
}

/**
 * The plant QC score (GET /api/qc-score - see backend QcScoreService / KPI Scoring Specification):
 * every KPI's latest value at the page's Timeline grain scored 0-100, averaged per section, and the
 * sections averaged into this number. Colours follow the score's band (green/amber/red, grey = no data).
 */
export function QcScoreCard({
  site,
  grain,
  dateRangeOverride,
  bestEver,
}: {
  site: string
  grain: Grain
  /** Leadership's Timeline: T-7/T-30/T-90/T-180 filter (see LeadershipFilterBar) picks a trailing
   * day-count window directly rather than one of `grain`'s fixed per-grain windows - `grain` is
   * still sent to the backend (the score's own aggregation period), just paired with this explicit
   * range instead of getGrainDateRange(grain)'s. Omitted elsewhere, where grain alone decides the
   * window as before. */
  dateRangeOverride?: { from: string; to: string }
  /** Leadership's reference screenshot only - no real historical-best endpoint exists yet, so this
   * stays undefined (hiding the row entirely) everywhere else rather than guessing a number. */
  bestEver?: BestEverScore
}) {
  const { from, to } = dateRangeOverride ?? getGrainDateRange(grain)
  const { data, isLoading, isError } = useQcScore({ site, grain, from, to })
  const plantBand = data?.band ?? null
  const meta = scoreBandMeta(plantBand)
  const statusLabel = isLoading ? 'Loading…' : isError ? 'Unavailable' : scoreBandLabel(plantBand)

  return (
    <Card className="gap-0 overflow-hidden py-0">
      <div className="h-1.5 w-full" style={{ backgroundColor: meta.solidHex }} />
      <CardHeader className="pt-5">
        <CardTitle className="flex items-center gap-2 text-[10px] font-bold tracking-[1px] uppercase" style={{ color: '#99a1af' }}>
          QC Score · {site} Operations
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
          {/* Worst first (left), best last (right) - re-sorted every render rather than the
          domain's fixed declaration order. Ordered by band severity first, not raw score: a RED
          section is worse than an AMBER one regardless of their two numeric scores (band isn't
          simply "score below a cutoff" - see KPI Scoring Specification section 8), so sorting by
          score alone could rank a RED section above an AMBER one if its average happened to be
          higher. Score is only the tiebreaker within the same band. No-data sections sort last:
          they aren't a bad score, just an unavailable one. */}
          {(Object.keys(SECTION_LABELS) as SectionKey[])
            .map((key) => ({ key, section: data?.sections.find((s) => s.sectionId === key) }))
            .sort((a, b) => {
              const severityRank = (s: typeof a) => {
                const severity = bandSeverity(s.section?.band ?? null)
                return severity ? SEVERITY_ORDER.indexOf(severity) : SEVERITY_ORDER.length
              }
              return severityRank(a) - severityRank(b) || (a.section?.score ?? Infinity) - (b.section?.score ?? Infinity)
            })
            .map(({ key, section }) => {
              const sectionMeta = scoreBandMeta(section?.band ?? null)
              return (
                <div key={key} className="flex flex-col items-center text-center">
                  <div className="text-xl font-black" style={{ color: sectionMeta.solidHex }}>
                    {formatScore(section?.score)}
                  </div>
                  <div className="pt-0.5 text-[8px] font-bold tracking-wide uppercase" style={{ color: '#99a1af' }}>
                    {SECTION_LABELS[key].shortLabel}
                  </div>
                  <div className="mt-1 h-1 w-full rounded-full" style={{ backgroundColor: sectionMeta.solidHex }} />
                </div>
              )
            })}
        </div>

        {bestEver && (
          <div className="mt-3 flex items-center justify-between gap-2 border-t border-border pt-3 text-[11px]">
            <span className="flex items-center gap-1.5 text-muted-foreground">
              Best ever: <span className="font-semibold text-foreground">{bestEver.score}</span> · {bestEver.period}
              <DemoDataBadge />
            </span>
            <a href="#" className="flex shrink-0 items-center gap-0.5 font-semibold text-brand hover:underline">
              View breakdown
              <ChevronRight className="size-3" />
            </a>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
