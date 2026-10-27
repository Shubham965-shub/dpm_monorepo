import { keepPreviousData, useQueries } from '@tanstack/react-query'
import type { Grain, KpiCardResponse, KpiThresholds } from '@/lib/kpiSchemas'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { defaultGrainFor } from '@/lib/kpiDefaultGrain'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import { seriesKeyFor } from '@/lib/kpiRowName'
import { filterRootCauseRowsToDimension, pickMaximalRootCauseDimension } from '@/lib/rootCauseDimensions'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { severityFromStatus, type Severity, type TierCounts } from '@/components/dashboard/types'
import { chartAxisFor, toChartData, type ChartAxis } from '@/components/kpi/KpiChartCard'
import { EMPTY_PERIOD_IS_NOT_STALE_KPI_IDS, coarserGrain, staleSince } from '@/lib/kpiFreshness'
import { toIsoDate } from '@/lib/kpiDateRange'
import { buildKpiQueryString, fetchKpiCardData, type GlobalKpiFilters } from './useKpiData'

/** Default (no user selection yet) reduction applied per-KPI before deriving chart metrics.
 * Root Cause Breakdown defaults to its single highest-count-in-the-latest-period dimension rather
 * than every dimension overlaid - SeverityKpiCard's own dimension dropdown re-slices the same
 * already-fetched rows to a different one without needing this function at all. */
export function filterRowsForKpi(kpiId: string, rows: KpiCardResponse[]): KpiCardResponse[] {
  if (kpiId === 'root-cause') {
    const maximal = pickMaximalRootCauseDimension(rows)
    return maximal ? filterRootCauseRowsToDimension(rows, maximal) : rows
  }
  return rows
}

export interface KpiMetrics {
  shownSeries: ReturnType<typeof toChartData>['shownSeries']
  hiddenCount: number
  chartData: ReturnType<typeof toChartData>['chartData']
  /** Latest/previous/severity are all derived from the *first* shown series - for the handful of
   * multi-series KPIs (e.g. Priority Sample Compliance's P1-P4) the chart still plots every
   * series, but one representative series drives the big value/delta/severity numbers since the
   * mock thresholds are per-KPI, not per-series. */
  latestValue: number | null
  previousValue: number | null
  /** The period the headline value comes from - the latest completed one with data (see deriveKpiMetrics). */
  latestPeriodStart: string | null
  /** Set when the data stopped arriving: the last period that has data (see staleSince). The card says
   * so instead of showing a status. */
  staleSince: string | null
  severity: Severity | null
  unit: string | null
  /** The primary series' band boundaries from the backend threshold config - null when unconfigured. */
  thresholds: KpiThresholds | null
  /** The raw, already-fetched per-period rows this card's chart was built from (after
   * filterRowsForKpi's reduction) - e.g. KpiDrilldownTable's "By period" tab reads these directly
   * (one row per periodStart, each with its own status/numerator/denominator) instead of a second
   * fetch, so the two can never disagree (see KpiDetailModal). */
  rows: KpiCardResponse[]
}

// A threshold line can sit far from the real value's own range - left to Recharts' default
// auto-domain, a ReferenceLine at that distant y still stretches the axis to include it (a
// documented Recharts quirk: ifOverflow only governs rendering *after* the domain is computed, but
// auto domain calculation itself already factored the reference line in), which is what made the
// line look flat. Computing the Y domain from the KPI's own values plus the threshold lines it
// must always show (see `includeValues`) - but NOT the padding a wildly-off action threshold would
// otherwise add - keeps the line's real movement visible while keeping the boundary lines in view.
const Y_DOMAIN_PADDING_RATIO = 0.15
// When a boundary line is the outermost thing on a side, pad that side more, so the zone beyond it
// (e.g. the red above "R>45", the green above "G≥93") shows as a clear band, not a sliver.
const ZONE_PADDING_RATIO = 0.3

export function computeValueDomain(
  chartData: KpiMetrics['chartData'],
  shownSeries: KpiMetrics['shownSeries'],
  includeValues: number[] = [],
): [number, number] {
  const dataValues = chartData.flatMap((row) =>
    shownSeries.map((s) => row[s.seriesKey]).filter((v): v is number => typeof v === 'number'),
  )
  const values = [...dataValues, ...includeValues]
  if (values.length === 0) return [0, 1]
  const min = Math.min(...values)
  const max = Math.max(...values)
  if (min === max) return [min - 1, max + 1]
  const span = max - min
  const boundaryOnTop = includeValues.length > 0 && (dataValues.length === 0 || Math.max(...includeValues) >= Math.max(...dataValues))
  const boundaryOnBottom = includeValues.length > 0 && (dataValues.length === 0 || Math.min(...includeValues) <= Math.min(...dataValues))
  const top = max + span * (boundaryOnTop ? ZONE_PADDING_RATIO : Y_DOMAIN_PADDING_RATIO)
  const bottom = min - span * (boundaryOnBottom ? ZONE_PADDING_RATIO : Y_DOMAIN_PADDING_RATIO)
  // Every KPI here is a count/rate/% - never let padding push the axis below zero.
  return [min >= 0 ? Math.max(bottom, 0) : bottom, top]
}

/** Turns a raw KpiCardResponse list into the derived metrics a card needs (chart data, latest/
 * previous value, severity) - shared between the board-level batch fetch below and
 * SeverityKpiCard's own per-card grain-override fetch, so the "first series is primary, severity
 * = its latest row's backend status" logic isn't duplicated. `kpiId` drives filterRowsForKpi's
 * per-KPI reduction (currently just Root Cause Breakdown's default dimension). */
export function deriveKpiMetrics(kpiId: string, rows: KpiCardResponse[], axis?: ChartAxis): KpiMetrics {
  const filteredRows = filterRowsForKpi(kpiId, rows)
  const { shownSeries, hiddenCount, chartData } = toChartData(filteredRows, axis)
  const primary = shownSeries[0]

  // The headline is the primary series' latest COMPLETED period that has data (rows arrive ordered by
  // period) and its colour is that same row's status - an empty or still-running period never decides
  // the status, and number and colour always describe the same period.
  const primaryRows = primary ? filteredRows.filter((row) => seriesKeyFor(row) === primary.seriesKey) : []
  const valuedRows = primaryRows.filter((row) => row.value != null)
  const completedRows = valuedRows.filter((row) => !row.inProgress)
  const latestRow = completedRows.at(-1)
  const latestValue = latestRow?.value ?? null
  const previousValue = completedRows.at(-2)?.value ?? null

  const lastDataStart = valuedRows.at(-1)?.periodStart ?? null
  const asOf = axis ? (axis.to < toIsoDate(new Date()) ? axis.to : toIsoDate(new Date())) : null
  const stale = axis && asOf && !EMPTY_PERIOD_IS_NOT_STALE_KPI_IDS.has(kpiId) ? staleSince(lastDataStart, coarserGrain(axis.grain, axis.frequency), asOf) : null

  const severity = stale ? null : severityFromStatus(latestRow?.status)
  const thresholds = primaryRows.findLast((row) => row.thresholds)?.thresholds ?? null

  return {
    shownSeries, hiddenCount, chartData, latestValue, previousValue, latestPeriodStart: latestRow?.periodStart ?? null,
    staleSince: stale, severity, unit: primary?.unit ?? null, thresholds, rows: filteredRows,
  }
}

export interface DomainTrendKpiResult extends KpiMetrics {
  kpi: KpiDefinition
  isLoading: boolean
  isError: boolean
}

/** Each KPI's own D/W/M/Q/Y default grain (see kpiDefaultGrain.ts), falling back to the page's
 * shared Timeline grain when the framework doc has no cell for this KPI/lens - exactly
 * useCardGrain's own resolution, so the board fetches (and tiers severity by) the identical
 * grain/date-range SeverityKpiCard will independently resolve and display. Sharing that
 * resolution (rather than the whole batch always fetching at globalFilters.grain) is what makes
 * the two agree - before this, a KPI whose own default grain differs from the page's (e.g.
 * Calibration Compliance's CXO default of quarterly vs the page's monthly) could be tiered by its
 * monthly-grain severity while its own card displayed (and colored itself by) quarterly's. */
function resolvedKpiFilters(kpi: KpiDefinition, lens: KpiVisibilityLens, globalFilters: GlobalKpiFilters): GlobalKpiFilters {
  const grain = (globalFilters.forceGrain ? null : defaultGrainFor(kpi.id, lens)) ?? (globalFilters.grain as Grain)
  return grain === globalFilters.grain ? globalFilters : { ...globalFilters, grain, ...getGrainDateRange(grain) }
}

/** Fetches real data for every given KPI in a domain (via useQueries, so the hook-count stays
 * fixed regardless of how many KPIs a domain has); each one's severity is the backend's own
 * status (computed from its KPI threshold config), not decided here. Every extra filter starts at
 * All (param omitted), or its defaultValue - the card's own Filters control (SeverityKpiCard) can narrow it. */
export function useDomainTrendKpis(
  domain: KpiDomain,
  kpis: KpiDefinition[],
  globalFilters: GlobalKpiFilters,
  lens: KpiVisibilityLens,
): DomainTrendKpiResult[] {
  const results = useQueries({
    queries: kpis.map((kpi) => {
      const resolvedFilters = resolvedKpiFilters(kpi, lens, globalFilters)
      const queryString = buildKpiQueryString(resolvedFilters, kpi)
      return {
        queryKey: ['kpi', domain.key, kpi.id, queryString] as const,
        queryFn: () => fetchKpiCardData(domain, kpi, resolvedFilters, queryString),
        // Without this, a sibling card's own interaction (e.g. another KPI's grain toggle) that
        // causes DomainSeverityBoard to re-render makes this whole useQueries call momentarily
        // report every untouched KPI's data as undefined too (confirmed live: no new network
        // request fires, so this isn't a real refetch, just useQueries' own re-invocation) - severity
        // flashes to null, moving that KPI into the "pending" tier and remounting it, wiping its own
        // local state (its GrainToggle pick, filters, ...) right back to default. Same fix useKpiData
        // already applies to the per-card override fetch, for the same reason.
        placeholderData: keepPreviousData,
      }
    }),
  })

  return kpis.map((kpi, i) => {
    const query = results[i]
    const metrics = deriveKpiMetrics(kpi.id, query.data ?? [], chartAxisFor(kpi, resolvedKpiFilters(kpi, lens, globalFilters), defaultGrainFor(kpi.id, lens)))

    return { ...metrics, kpi, isLoading: query.isLoading, isError: query.isError }
  })
}

/** A domain's tier breakdown straight off its board-level results (`result.severity`) - mirrors
 * DomainSeverityBoard's own default computation before any card reports a live override
 * (its `liveSeverities` state starts from this same `severity` field), not the
 * `liveSeverities`-augmented one - see DomainTierCounts, which uses this for the collapsed-header
 * seed that DomainSeverityBoard's own live-override layer then keeps correcting once expanded. */
export function tierCountsFromResults(results: DomainTrendKpiResult[]): TierCounts {
  let critical = 0
  let warning = 0
  let onTrack = 0
  let noThreshold = 0
  for (const result of results) {
    if (result.severity === 'action') critical++
    else if (result.severity === 'alert') warning++
    else if (result.severity === 'in-control') onTrack++
    else noThreshold++
  }
  return { critical, warning, onTrack, noThreshold }
}
