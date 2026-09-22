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
// SeverityKpiCard). SeverityKpiCard computes this from the real fetched value against the mock
// target/alert/action thresholds (see classifySeverity below) - the color is derived, not a fixed
// per-card constant.
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

// The dashed Target/Alert reference lines on SeverityKpiCard's chart - always this fixed
// green/amber pair (confirmed across every card in the Figma source), independent of the card's
// own computed severity accent.
export const TARGET_LINE_HEX = '#15803d'
export const ALERT_LINE_HEX = '#b45309'

/** target < action => lower is better (e.g. Sample TAT hours); target > action => higher is
 * better (e.g. SLA Adherence %) - the three threshold numbers already encode direction
 * consistently, so this doesn't need a separate per-KPI flag. */
export function isLowerBetter(target: number, action: number): boolean {
  return target < action
}

/**
 * Classifies a real value against a KPI's (mocked) target/alert/action thresholds - see
 * mockData.ts's KPI_THRESHOLD_MOCK caveat: the boundary numbers are placeholders, but comparing
 * the real fetched value against them is a real computation, not an invented judgment.
 */
export function classifySeverity(value: number, target: number, alert: number, action: number): Severity {
  if (isLowerBetter(target, action)) {
    if (value <= alert) return 'in-control'
    if (value <= action) return 'alert'
    return 'action'
  }
  if (value >= alert) return 'in-control'
  if (value >= action) return 'alert'
  return 'action'
}
