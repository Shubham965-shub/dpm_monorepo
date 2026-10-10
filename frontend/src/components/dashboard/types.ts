import type { KpiStatus, KpiThresholds, ScoreBand } from '@/lib/kpiSchemas'
import type { BarRow } from '@/components/kpi/KpiChartCard'
import type { KpiDefinition, KpiDomain } from '@/lib/kpiConfig'

/**
 * Shared by every KPI card grid (DomainSeverityBoard's severity tiers, SectionSummaryList's
 * ranking/stacked sections) so the width rule lives in one place. Each card is 200-400px wide;
 * `max(200px, calc(33.333% - 0.5rem))` forces at least 3 columns whenever the row is wide enough
 * for that (0.5rem = this grid's own gap-3 split three ways: 2 gaps / 3 columns), while still
 * capping at 400px - once the container is wide enough for a 4th+ 400px column, one gets added
 * rather than stretching the first three past 400px. A section with under 3 cards just renders
 * fewer columns.
 *
 * auto-fill, not auto-fit: auto-fit collapses empty tracks and lets existing cards stretch into
 * the freed space, so a sparsely-populated section (e.g. only 1-2 KPIs with no configured
 * threshold) would stretch its cards toward 400px while a densely-packed tier next to it computes
 * a narrower width - same rule, different realized widths per section. auto-fill's column count
 * depends only on container width, not item count, so every section resolves to the same per-card
 * width; a sparse section just shows trailing empty space instead of stretching.
 */
export const KPI_CARD_GRID_CLASS = 'grid grid-cols-[repeat(auto-fill,minmax(max(200px,calc(33.333%-0.5rem)),400px))] items-start gap-3'

/** Stable DOM id for a KPI's own card, wherever it renders (SeverityKpiCard, KpiRankingBarCard,
 * PriorityComplianceCard, RootCauseHeatmapCard) - lets the Overall Heatmap scroll straight to a
 * KPI's real card when that KPI's cell is clicked (see OverallHeatmapDemo's onKpiClick and
 * SectionSummaryList's scrollToKpi). */
export function kpiAnchorId(kpiId: string): string {
  return `kpi-card-${kpiId}`
}

export type RagStatus = 'action' | 'alert' | 'in-control' | 'no-data'

export interface HeatmapCell {
  /** "Jan" for a monthly-window heatmap, "W38" for a weekly one - see useHeatmapData. */
  periodLabel: string
  status: RagStatus
  /** The same primary-series value shown as the KPI card's own score - null when this period has no
   * matching row (no-data). */
  value: number | null
}

export interface HeatmapRow {
  slug: string
  name: string
  cells: HeatmapCell[]
  /** Non-All filters the cells were fetched with (see defaultFilterSummaries) - empty for most KPIs. */
  defaultFilters: string[]
}

export type Severity = Exclude<RagStatus, 'no-data'>

// Worst first, best last - shared by DomainSeverityBoard's tier bands and QcScoreCard's
// worst-to-best section ordering, so "worst" means the same thing everywhere in the app.
export const SEVERITY_ORDER: Severity[] = ['action', 'alert', 'in-control']

// Exact hex values pulled from Figma (file T5oSGlK2eTYF8KvKP5M9lF, node 5024:13253 "KPI List") via
// the Figma MCP get_design_context tool - not eyeballed from a screenshot. Single source shared by
// every severity-styled component (OverallHeatmapDemo's dot grid, SectionSummaryBar's pills,
// SeverityKpiCard). SeverityKpiCard picks it from the latest row's status, which the backend
// computes from its KPI threshold config - the color is derived, not a fixed per-card constant.
export const SEVERITY_META: Record<
  Severity,
  {
    label: string
    /** Strong severity color - dot, card left-border, band label text/left-border. */
    solidHex: string
    /** Pale tint of solidHex. */
    paleHex: string
    /** Near-white card background. */
    cardBg: string
    /** Light section-band background. */
    bandBg: string
    /** Pastel dot color used inside the white/20%-bg pill on the dark domain header bar. */
    pillDotHex: string
  }
> = {
  action: {
    label: 'Critical',
    solidHex: '#be123c',
    paleHex: '#fecdd3',
    cardBg: '#fff8f8',
    bandBg: '#fff1f2',
    pillDotHex: '#ffc9c9',
  },
  alert: {
    label: 'Warning',
    solidHex: '#b45309',
    paleHex: '#fde68a',
    cardBg: '#fffdf4',
    bandBg: '#fffbeb',
    pillDotHex: '#fff085',
  },
  'in-control': {
    label: 'On Track',
    solidHex: '#15803d',
    paleHex: '#bbf7d0',
    cardBg: '#f8fff9',
    bandBg: '#f0fdf4',
    pillDotHex: '#b9f8cf',
  },
}

type SeverityMeta = (typeof SEVERITY_META)[Severity]

// Plain black-and-white for a KPI with no configured threshold - it has data, just no colour
// judgement, so it must not borrow a severity's styling (even a pale tint would read as a real
// band) or default to any particular colour at all: no background tint (plain white card,
// matching the page's own background), and the chart/border/value render in flat black.
const NO_THRESHOLD_META: SeverityMeta = {
  label: 'No threshold',
  solidHex: '#000000',
  paleHex: '#e5e7eb',
  cardBg: '#ffffff',
  bandBg: '#ffffff',
  pillDotHex: '#000000',
}

export function severityMeta(severity: Severity | null): SeverityMeta {
  return severity ? SEVERITY_META[severity] : NO_THRESHOLD_META
}

// A QC score band borrows the matching severity's colours - green/amber/red mean the same thing
// on a score as on a KPI card; null (no data) gets the neutral grey.
const BAND_SEVERITY: Record<NonNullable<ScoreBand>, Severity> = { GREEN: 'in-control', AMBER: 'alert', RED: 'action' }

export function scoreBandMeta(band: ScoreBand): SeverityMeta {
  return severityMeta(band ? BAND_SEVERITY[band] : null)
}

/** The band's severity - RED is worse than a merely-low score with no red-zone KPI in it, so
 * ordering "worst to best" must go by this (see QcScoreCard), not by raw score value alone. */
export function bandSeverity(band: ScoreBand): Severity | null {
  return band ? BAND_SEVERITY[band] : null
}

// KPI Scoring Specification section 8's interpretation of each band.
const BAND_LABEL: Record<NonNullable<ScoreBand>, string> = { GREEN: 'Performing', AMBER: 'Watch', RED: 'Unacceptable' }

export function scoreBandLabel(band: ScoreBand): string {
  return band ? BAND_LABEL[band] : 'No data'
}

/** Rounding is display-only (the backend keeps full precision); null = no data. */
export function formatScore(score: number | null | undefined): string {
  return score == null ? '—' : String(Math.round(score))
}

const COMPACT_SCORE_FORMAT = new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 })

/** Same as formatScore, but abbreviates 4+ digit values (1200 -> "1.2K", 20000 -> "20K") - for a
 * dense grid (e.g. the Overall Heatmap) where a long number would crowd the cell; the exact value
 * still shows via the cell's own hover tooltip (formatScore there). Below 1000, defers to
 * formatScore's own whole-number rounding rather than Intl's compact formatter, which would
 * otherwise keep a decimal (e.g. "85.4") on a value that's never actually abbreviated. */
export function formatCompactScore(score: number | null | undefined): string {
  if (score == null) return '—'
  return Math.abs(score) < 1000 ? formatScore(score) : COMPACT_SCORE_FORMAT.format(score)
}

// The dashed limit lines on SeverityKpiCard/KpiDetailModal's charts: the Alert limit (where in
// control ends) in amber, the Action limit (where the action zone starts) in red - independent of
// the card's own severity accent.
export const ALERT_LIMIT_HEX = SEVERITY_META.alert.solidHex
export const ACTION_LIMIT_HEX = SEVERITY_META.action.solidHex

// The backend decides a row's status (see KpiThresholdEvaluator) - the UI only maps it to a severity style.
const STATUS_TO_SEVERITY: Record<Exclude<KpiStatus, null>, Severity> = {
  RED: 'action',
  AMBER: 'alert',
  GREEN: 'in-control',
}

export function severityFromStatus(status: KpiStatus | undefined): Severity | null {
  return status ? STATUS_TO_SEVERITY[status] : null
}

export interface BoundaryLine {
  y: number
  /** alert = the Alert limit (the band's green edge: in control on one side, alert zone on the
   * other); action = the Action limit (the red edge: past it is the action zone). */
  tone: 'alert' | 'action'
  /** Short on-chart label, e.g. "Alert 90" / "Action 82". */
  label: string
  /** Full name for the detail view, e.g. "Alert limit", or "Alert limit (low)" on a two-sided band. */
  name: string
  /** Alert labels sit on the in-control side of their line and action labels on the action side, so
   * an alert/action pair always points away from each other; lower-edge limits go on the left and
   * upper-edge ones on the right, so even a narrow band's labels never stack. (On a horizontal
   * ReferenceLine, Recharts' "insideBottom" draws above the line and "insideTop" below it.) */
  labelPosition: 'insideBottomLeft' | 'insideTopLeft' | 'insideTopRight' | 'insideBottomRight'
}

// Snapshot limits are raw percentiles (e.g. 62.8546805458491) - enough precision to read, without
// rounding a small limit like 0.05 away.
const LIMIT_LABEL_MAX_DECIMALS = 2
const limitLabel = (limit: number) => limit.toLocaleString(undefined, { maximumFractionDigits: LIMIT_LABEL_MAX_DECIMALS })

/**
 * Every set limit as a chart reference line. The snapshot limits map onto QC's limit terms: the
 * amber edges (greenLow/greenHigh = lower_amber/upper_amber) are the Alert limits, the red edges
 * (redLow/redHigh = lower_red/upper_red) the Action limits - so a value past the alert limit but
 * not the action limit is in the alert zone, and past the action limit is in the action zone. A
 * higher_is_worse row only has the High pair, etc.
 */
export function boundaryLines(thresholds: KpiThresholds | null | undefined): BoundaryLine[] {
  if (!thresholds) return []
  const { greenLow, greenHigh, redLow, redHigh } = thresholds
  const twoSidedAlert = greenLow != null && greenHigh != null
  const twoSidedAction = redLow != null && redHigh != null
  const name = (base: string, twoSided: boolean, side: string) => (twoSided ? `${base} (${side})` : base)
  const lines: BoundaryLine[] = []
  if (greenLow != null) lines.push({ y: greenLow, tone: 'alert', label: `Alert ${limitLabel(greenLow)}`, name: name('Alert limit', twoSidedAlert, 'low'), labelPosition: 'insideBottomLeft' })
  if (greenHigh != null) lines.push({ y: greenHigh, tone: 'alert', label: `Alert ${limitLabel(greenHigh)}`, name: name('Alert limit', twoSidedAlert, 'high'), labelPosition: 'insideTopRight' })
  if (redLow != null) lines.push({ y: redLow, tone: 'action', label: `Action ${limitLabel(redLow)}`, name: name('Action limit', twoSidedAction, 'low'), labelPosition: 'insideTopLeft' })
  if (redHigh != null) lines.push({ y: redHigh, tone: 'action', label: `Action ${limitLabel(redHigh)}`, name: name('Action limit', twoSidedAction, 'high'), labelPosition: 'insideBottomRight' })
  return lines
}

export function boundaryHex(tone: BoundaryLine['tone']) {
  return tone === 'alert' ? ALERT_LIMIT_HEX : ACTION_LIMIT_HEX
}

export interface BoundaryZone {
  /** Lower value edge. */
  y1: number
  /** Upper value edge. */
  y2: number
  fillHex: string
  fillOpacity: number
}

/** Tint strength behind the chart - strong enough to read on the tinted card backgrounds, light
 * enough that the dashed lines and the series stay on top visually. The action zone is lighter:
 * it's often the biggest area on a bad KPI, and at full strength it overpowers the chart. */
const ZONE_FILL_OPACITY = 0.45
const ACTION_ZONE_FILL_OPACITY = 0.2

/**
 * The shaded in-control / alert / action zones implied by the band columns, mirroring the
 * backend's rule (KpiThresholdEvaluator): in control inside the alert limits [greenLow, greenHigh],
 * action zone past the action limits (redLow/redHigh), alert zone in between. A missing green edge falls back to the red edge on that side. Open-ended sides run to
 * the chart's own `domain` edges - passed explicitly, because Recharts' ReferenceArea maps an
 * omitted y1/y2 to the opposite edge from what you'd expect (y1 -> top, y2 -> bottom).
 */
export function boundaryZones(thresholds: KpiThresholds | null | undefined, domain: [number, number]): BoundaryZone[] {
  if (!thresholds) return []
  const { redLow, greenLow, greenHigh, redHigh } = thresholds
  if (redLow == null && greenLow == null && greenHigh == null && redHigh == null) return []
  const [bottom, top] = domain
  const green = SEVERITY_META['in-control'].paleHex
  const amber = SEVERITY_META.alert.paleHex
  const red = SEVERITY_META.action.paleHex
  const inControl = { fillHex: green, fillOpacity: ZONE_FILL_OPACITY }
  const alert = { fillHex: amber, fillOpacity: ZONE_FILL_OPACITY }
  const action = { fillHex: red, fillOpacity: ACTION_ZONE_FILL_OPACITY }
  const zones: BoundaryZone[] = [{ y1: greenLow ?? redLow ?? bottom, y2: greenHigh ?? redHigh ?? top, ...inControl }]
  if (greenLow != null) zones.push({ y1: redLow ?? bottom, y2: greenLow, ...alert })
  if (redLow != null) zones.push({ y1: bottom, y2: redLow, ...action })
  if (greenHigh != null) zones.push({ y1: greenHigh, y2: redHigh ?? top, ...alert })
  if (redHigh != null) zones.push({ y1: redHigh, y2: top, ...action })
  // Equal limits (e.g. both 0) leave zero-height bands - they draw nothing and would share a React key.
  return zones.filter((zone) => zone.y1 !== zone.y2)
}

/** A ranking bar chart's value domain - shared by its bars and its pinned axis strip (RankingValueAxis) so the two line up. */
export function rankingValueDomain(rows: BarRow[]): [number, number] {
  return [0, Math.max(0, ...rows.map((row) => row.value ?? 0))]
}

/** A domain's (or the whole page's) critical/warning/onTrack/noThreshold breakdown - shared by
 * SectionSummaryList's per-section chips and DashboardPage's page-level severity badge summary
 * (see DomainTierCounts) so the two totals can never disagree. */
export interface TierCounts {
  critical: number
  warning: number
  onTrack: number
  noThreshold: number
}

export const ZERO_TIER_COUNTS: TierCounts = { critical: 0, warning: 0, onTrack: 0, noThreshold: 0 }

/** Maps a Severity onto its own field of a TierCounts - shared by SeverityBadgeSummary's clickable
 * badges and SectionSummaryList's per-section "does this section contain the selected severity"
 * check (see DashboardPage's selectedSeverity), so both read the same count the same way. */
export function tierCountForSeverity(counts: TierCounts, severity: Severity): number {
  return severity === 'action' ? counts.critical : severity === 'alert' ? counts.warning : counts.onTrack
}

export function sumTierCounts(a: TierCounts, b: TierCounts): TierCounts {
  return {
    critical: a.critical + b.critical,
    warning: a.warning + b.warning,
    onTrack: a.onTrack + b.onTrack,
    noThreshold: a.noThreshold + b.noThreshold,
  }
}

// KPIs whose real shape is a per-category share of a period's total (not a single trending value
// or a whole-range ranking) - routed to their own dedicated card instead of DomainSeverityBoard or
// KpiRankingBarCard - its real data is a raw sample count per priority tier (P1..P4) that always
// sums to the period's total, not one trending value a threshold could colour.
export const STACKED_SHARE_KPI_IDS = new Set(['priority-sample-compliance'])

// KPIs whose real shape is a dimension x period matrix best read as a heatmap, not a trendline or
// ranking - root-cause-consolidated has no single trending value a threshold could colour. Rendered
// as a DomainSeverityBoard "companion" of its paired host id (see SectionSummaryList's
// HEATMAP_COMPANION_HOST_ID), not as its own grid card.
export const HEATMAP_KPI_IDS = new Set(['root-cause-consolidated'])

/** The per-period trend KPIs DomainSeverityBoard actually places into severity tiers for a domain -
 * excludes STACKED_SHARE_KPI_IDS/HEATMAP_KPI_IDS (routed to their own card types) and ranking/column
 * chart types (category totals over a range, not a per-period value a tier applies to). Shared by
 * SectionSummaryList's own card-grid population and DomainTierCounts' seed, so the two populations
 * can never disagree either. */
export function trendKpisForDomain(domain: KpiDomain | undefined, visibleKpiIds: Set<string>): KpiDefinition[] {
  const domainKpis = domain?.kpis.filter((kpi) => visibleKpiIds.has(kpi.id)) ?? []
  return domainKpis.filter(
    (kpi) => kpi.chartType !== 'bar' && kpi.chartType !== 'column' && !STACKED_SHARE_KPI_IDS.has(kpi.id) && !HEATMAP_KPI_IDS.has(kpi.id),
  )
}

/** A STACKED_SHARE_KPI_IDS member has no threshold to colour by design (see its own comment above) -
 * not a missing/stale config the way a real trend KPI's "no threshold" can be. It's still counted in
 * the section header's own tier breakdown (see DomainTierCounts/DomainSeverityBoard's
 * extraNoThreshold), even though PriorityComplianceCard's own stacked-percentage view never goes
 * grey itself - this only affects the header's count, not that card's rendering. */
export function stackedShareNoThresholdCount(domain: KpiDomain | undefined, visibleKpiIds: Set<string>): number {
  return domain?.kpis.filter((kpi) => visibleKpiIds.has(kpi.id) && STACKED_SHARE_KPI_IDS.has(kpi.id)).length ?? 0
}
