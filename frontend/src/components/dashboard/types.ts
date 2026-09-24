import type { KpiStatus, KpiThresholds } from '@/lib/kpiSchemas'

export type RagStatus = 'action' | 'alert' | 'in-control' | 'no-data'

export interface HeatmapCell {
  month: string
  status: RagStatus
}

export interface HeatmapRow {
  slug: string
  name: string
  cells: HeatmapCell[]
}

export type Severity = Exclude<RagStatus, 'no-data'>

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

// Neutral grey for a KPI with no configured threshold - it has data, just no colour judgement, so
// it must not borrow a severity's styling (a green border would read as "On Track").
const NO_THRESHOLD_META: SeverityMeta = {
  label: 'No threshold',
  solidHex: '#6b7280',
  paleHex: '#e5e7eb',
  cardBg: '#fafafa',
  bandBg: '#f9fafb',
  pillDotHex: '#d1d5db',
}

export function severityMeta(severity: Severity | null): SeverityMeta {
  return severity ? SEVERITY_META[severity] : NO_THRESHOLD_META
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

/**
 * Every set limit as a chart reference line. The band columns map onto QC's limit terms: the green
 * edges (Band_Green_Low/High) are the Alert limits, the red edges (Band_Red_Low/High) the Action
 * limits - so a value past the alert limit but not the action limit is in the alert zone, and past
 * the action limit is in the action zone. A higher_is_worse row only has the High pair, etc.
 */
export function boundaryLines(thresholds: KpiThresholds | null | undefined): BoundaryLine[] {
  if (!thresholds) return []
  const { greenLow, greenHigh, redLow, redHigh } = thresholds
  const twoSidedAlert = greenLow != null && greenHigh != null
  const twoSidedAction = redLow != null && redHigh != null
  const name = (base: string, twoSided: boolean, side: string) => (twoSided ? `${base} (${side})` : base)
  const lines: BoundaryLine[] = []
  if (greenLow != null) lines.push({ y: greenLow, tone: 'alert', label: `Alert ${greenLow}`, name: name('Alert limit', twoSidedAlert, 'low'), labelPosition: 'insideBottomLeft' })
  if (greenHigh != null) lines.push({ y: greenHigh, tone: 'alert', label: `Alert ${greenHigh}`, name: name('Alert limit', twoSidedAlert, 'high'), labelPosition: 'insideTopRight' })
  if (redLow != null) lines.push({ y: redLow, tone: 'action', label: `Action ${redLow}`, name: name('Action limit', twoSidedAction, 'low'), labelPosition: 'insideTopLeft' })
  if (redHigh != null) lines.push({ y: redHigh, tone: 'action', label: `Action ${redHigh}`, name: name('Action limit', twoSidedAction, 'high'), labelPosition: 'insideBottomRight' })
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
  return zones
}
