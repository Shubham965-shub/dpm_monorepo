import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Grain } from '@/lib/kpiSchemas'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import { useQcScore } from '@/hooks/useQcScore'
import type { SectionKey } from './mockData'
import { SECTION_LABELS } from './sectionLabels'
import { formatScore, scoreBandLabel, scoreBandMeta } from './types'

/**
 * The plant QC score (GET /api/qc-score - see backend QcScoreService / KPI Scoring Specification):
 * every KPI's latest value at the page's Timeline grain scored 0-100, averaged per section, and the
 * sections averaged into this number. Colours follow the score's band (green/amber/red, grey = no data).
 */
export function QcScoreCard({ site, grain }: { site: string; grain: Grain }) {
  const { from, to } = getGrainDateRange(grain)
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
          {(Object.keys(SECTION_LABELS) as SectionKey[]).map((key) => {
            const section = data?.sections.find((s) => s.sectionId === key)
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
      </CardContent>
    </Card>
  )
}
