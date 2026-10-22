import { Fragment, useCallback, useEffect, useState, type ReactNode } from 'react'
import { cn, pluralize } from '@/lib/utils'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { useDomainTrendKpis, type DomainTrendKpiResult } from '@/hooks/useDomainTrendKpis'
import { KPI_CARD_GRID_CLASS, SEVERITY_ORDER, severityMeta, type Severity, type SeverityFilter } from './types'
import { SeverityKpiCard } from './SeverityKpiCard'

/** A KPI's own card, plus its companion (if any) tracking this card's LIVE on-screen severity - not
 * the board-level `result.severity`, which is only the KPI's untouched default-view reading. A KPI
 * like Root Cause Breakdown can move away from that default once more data resolves (its own
 * dimension pick/auto-pick and per-dimension threshold - see SeverityKpiCard's
 * onLiveSeverityChange), so both this card's OWN tier placement (reported up to
 * DomainSeverityBoard) and any companion riding beside it must follow the same live value, not the
 * stale board-level one. A plain component (not a function called from .map()) so its per-row
 * liveSeverity state is a real hook, not one called conditionally. */
function TrendResult({
  result,
  domain,
  globalFilters,
  lens,
  companion,
  onLiveSeverityChange,
}: {
  result: DomainTrendKpiResult
  domain: KpiDomain
  globalFilters: GlobalKpiFilters
  lens: KpiVisibilityLens
  companion?: (severity: Severity | null) => ReactNode
  onLiveSeverityChange: (kpiId: string, severity: Severity | null) => void
}) {
  const [liveSeverity, setLiveSeverity] = useState<Severity | null>(result.severity)
  const kpiId = result.kpi.id
  const handleLiveSeverityChange = useCallback(
    (severity: Severity | null) => {
      setLiveSeverity(severity)
      onLiveSeverityChange(kpiId, severity)
    },
    [onLiveSeverityChange, kpiId],
  )
  return (
    <Fragment>
      <SeverityKpiCard
        domain={domain}
        globalFilters={globalFilters}
        result={result}
        lens={lens}
        onLiveSeverityChange={handleLiveSeverityChange}
      />
      {companion?.(liveSeverity)}
    </Fragment>
  )
}

// items-stretch overrides the shared grid's items-start: every card in a row is the same height,
// and SeverityKpiCard keeps its chart aligned across them (two-line title slot, footer pinned to
// the bottom).
const TIER_GRID_CLASS = cn(KPI_CARD_GRID_CLASS, 'items-stretch px-5 pt-2 pb-4')

/** Renders one domain's severity-tier bands (Figma node 5024:13253's expanded-strip content),
 * fed by real backend data (useDomainTrendKpis) - severity is each KPI's backend status (from its
 * KPI threshold config), not fixed. KPIs still loading, erroring, or with no configured
 * threshold (see useDomainTrendKpis) have no severity and render in a plain
 * ungrouped row below the tiers.
 *
 * `companions` (keyed by kpi.id) renders extra content immediately after that KPI's own card, as
 * its own ordinary grid item right beside it - e.g. Root Cause (Consolidated)'s heatmap has no
 * severity of its own, so it can't land in a tier on its own merit, but per product decision it
 * should always sit beside Root Cause Breakdown wherever that KPI's live severity currently places
 * it (see SectionSummaryList.tsx). Given the host's own severity so the companion's color follows
 * it - stretched to its grid row's height like every other card in the tier. */
export function DomainSeverityBoard({
  domain,
  kpis,
  globalFilters,
  lens,
  companions,
  onTierCounts,
  extraNoThreshold = 0,
  highlightSeverity,
}: {
  domain: KpiDomain
  kpis: KpiDefinition[]
  globalFilters: GlobalKpiFilters
  /** Drives each card's own D/W/M/Q/Y default grain (see kpiDefaultGrain.ts) - the framework doc's
   * per-role cadence, not just the page's shared Timeline filter. */
  lens: KpiVisibilityLens
  companions?: Map<string, (severity: Severity | null) => ReactNode>
  /** SectionSummaryList's own header badge (see SectionSummaryBar) used to count tiers from a
   * separate, fixed-monthly-grain heatmap fetch (useHeatmapData) - a KPI whose own default grain
   * isn't monthly could land in a different tier there than the one actually rendered here, so the
   * badge and the real bands below it could disagree (this hook's own per-KPI grain resolution is
   * exactly what can cause that gap). Reporting these tiers' real counts up lets the badge match
   * what's actually on screen instead of that separate reading. */
  onTierCounts?: (counts: { critical: number; warning: number; onTrack: number; noThreshold: number }) => void
  /** This domain's own STACKED_SHARE_KPI_IDS count (see types.ts's stackedShareNoThresholdCount) -
   * `kpis` here never includes them (they render via PriorityComplianceCard, not this board), but
   * they still have no threshold by design, so the caller folds their count in here rather than the
   * header silently dropping them once a section expands. */
  extraNoThreshold?: number
  /** DashboardPage's own selectedSeverity (see SeverityBadgeSummary) - three real states, not two:
   * `undefined` (prop omitted entirely) means this page has no SeverityBadgeSummary at all - no
   * control exists to ever reveal a hidden band, so every tier shows, no-threshold included (QA's
   * dashboard, where every KPI has no threshold by design - hiding that band here would hide 100%
   * of every section with no way to undo it; also QC's CXO/Leadership views, which have never shown
   * this badge). `null` means the control exists but nothing is currently selected (QC's Operations
   * view) - every real-severity card shows, no-threshold hidden until that badge is clicked. A real
   * severity or 'no-threshold' narrows to just that one bucket either way. The header counts
   * reported via onTierCounts are NOT affected by any of this - they always report the section's
   * true totals, same as a badge's own number never changing just because its cards are hidden. */
  highlightSeverity?: SeverityFilter | null
}) {
  const results = useDomainTrendKpis(domain, kpis, globalFilters, lens)
  // Seeded once from the board-level fetch, then kept current per-KPI by each card's own
  // onLiveSeverityChange (see TrendResult) - tier placement below reads THIS, not results[].severity
  // directly, so a KPI that resolves away from its board-level default (Root Cause Breakdown's own
  // dimension/threshold correction, a grain or filter override) moves to the band it's actually
  // showing instead of staying wherever the initial board-level reading first put it. A key can
  // legitimately be reported as null (no threshold) - hasOwnProperty tells "not yet reported" apart
  // from that.
  const [liveSeverities, setLiveSeverities] = useState<Record<string, Severity | null>>(() =>
    Object.fromEntries(results.map((r) => [r.kpi.id, r.severity])),
  )
  const reportLiveSeverity = useCallback((kpiId: string, severity: Severity | null) => {
    setLiveSeverities((prev) => (prev[kpiId] === severity ? prev : { ...prev, [kpiId]: severity }))
  }, [])
  const effectiveSeverity = (r: DomainTrendKpiResult): Severity | null =>
    Object.hasOwn(liveSeverities, r.kpi.id) ? liveSeverities[r.kpi.id] : r.severity

  // Ground truth for the header badge (onTierCounts below) - always the section's true breakdown,
  // regardless of highlightSeverity (see that prop's own doc comment). Still reflects a live search,
  // same as before: `kpis` (and so `results`) already arrives pre-narrowed by SectionSummaryList's
  // own label filter in that case.
  const pending = results.filter((r) => effectiveSeverity(r) == null)
  const tierCounts = {
    critical: results.filter((r) => effectiveSeverity(r) === 'action').length,
    warning: results.filter((r) => effectiveSeverity(r) === 'alert').length,
    onTrack: results.filter((r) => effectiveSeverity(r) === 'in-control').length,
    noThreshold: pending.length + extraNoThreshold,
  }
  useEffect(() => {
    onTierCounts?.(tierCounts)
    // tierCounts is a fresh object every render - depending on it directly would refire this every
    // render regardless of whether the actual counts changed; its own scalar fields are the real
    // dependency.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [onTierCounts, tierCounts.critical, tierCounts.warning, tierCounts.onTrack, tierCounts.noThreshold])

  function renderResult(r: DomainTrendKpiResult) {
    return (
      <TrendResult
        key={r.kpi.id}
        result={r}
        domain={domain}
        globalFilters={globalFilters}
        lens={lens}
        companion={companions?.get(r.kpi.id)}
        onLiveSeverityChange={reportLiveSeverity}
      />
    )
  }

  // Which band(s) render below - see highlightSeverity's own doc comment for the 3-state meaning:
  // no filter control on this page at all (undefined) shows every band including no-threshold;
  // a control exists but nothing's selected (null) hides no-threshold until that badge is clicked;
  // a selected filter shows only its own single bucket, 'no-threshold' mapping to the null-severity
  // band exactly like the header's own tierCountForSeverity does for its count.
  const visibleGroups: (Severity | null)[] =
    highlightSeverity === undefined
      ? [...SEVERITY_ORDER, null]
      : highlightSeverity == null
        ? SEVERITY_ORDER
        : [highlightSeverity === 'no-threshold' ? null : highlightSeverity]

  return (
    <div className="flex flex-col">
      {visibleGroups.map((severity) => {
        const tier = results.filter((r) => effectiveSeverity(r) === severity)
        if (tier.length === 0) return null
        const meta = severityMeta(severity)

        return (
          <div key={severity ?? 'no-threshold'} style={{ backgroundColor: meta.bandBg }}>
            <div className="flex items-center gap-2 border-l-4 px-5 py-2" style={{ borderColor: meta.solidHex }}>
              <span className="size-2 rounded-full" style={{ backgroundColor: meta.solidHex }} />
              <span className="text-[11px] font-black tracking-wide uppercase" style={{ color: meta.solidHex }}>
                {meta.label}
              </span>
              <span className="text-muted-foreground text-[11px] font-bold">{pluralize(tier.length, 'KPI')}</span>
            </div>
            <div className={TIER_GRID_CLASS}>
              {tier.map(renderResult)}
            </div>
          </div>
        )
      })}
    </div>
  )
}
