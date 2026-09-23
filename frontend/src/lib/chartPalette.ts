// Validated default categorical palette (dataviz skill, references/palette.md) - fixed hue order,
// never cycled/reassigned per filter change. Slots 1-8; a 9th+ series folds into "+N more".
export const CATEGORICAL_PALETTE = [
  '#2a78d6', // 1 blue
  '#eb6834', // 2 orange
  '#1baf7a', // 3 aqua
  '#eda100', // 4 yellow
  '#e87ba4', // 5 magenta
  '#008300', // 6 green
  '#4a3aa7', // 7 violet
  '#e34948', // 8 red
]
export const MAX_SERIES = CATEGORICAL_PALETTE.length

// Recharts' hover Tooltip defaults to z-index:auto, which can end up visually under a legend/status
// row placed right below the chart (both are just siblings in normal flow) - forced above every
// other in-card element so it's never clipped by content underneath it.
export const TOOLTIP_Z_INDEX = 50

const GREEN_GRADIENT_LIGHT: [number, number, number] = [209, 250, 229] // emerald-100
const GREEN_GRADIENT_DARK: [number, number, number] = [6, 78, 59] // emerald-900

function rgbToHex([r, g, b]: [number, number, number]): string {
  return `#${[r, g, b].map((v) => Math.round(v).toString(16).padStart(2, '0')).join('')}`
}

/** `t` in [0,1] -> a color along the light->dark green ramp, index 0 = lightest (emerald-100),
 * 1 = darkest (emerald-900). */
export function sequentialGreen(t: number): string {
  const clamped = Math.min(1, Math.max(0, t))
  return rgbToHex([
    GREEN_GRADIENT_LIGHT[0] + (GREEN_GRADIENT_DARK[0] - GREEN_GRADIENT_LIGHT[0]) * clamped,
    GREEN_GRADIENT_LIGHT[1] + (GREEN_GRADIENT_DARK[1] - GREEN_GRADIENT_LIGHT[1]) * clamped,
    GREEN_GRADIENT_LIGHT[2] + (GREEN_GRADIENT_DARK[2] - GREEN_GRADIENT_LIGHT[2]) * clamped,
  ])
}

/** `count` evenly-spaced shades from light to dark green, index 0 = lightest. Used to color an
 * ordered set of tiers (e.g. a priority breakdown P4..P1) on one consistent gradient regardless of
 * how many tiers actually exist, rather than a fixed palette sized for exactly 4. */
export function greenGradient(count: number): string[] {
  if (count <= 1) return [sequentialGreen(1)]
  return Array.from({ length: count }, (_, i) => sequentialGreen(i / (count - 1)))
}

// Status palette (fixed, never themed) - mirrors backend KpiStatus (RED/AMBER/GREEN).
export const STATUS_COLORS: Record<'GREEN' | 'AMBER' | 'RED', string> = {
  GREEN: '#0ca30c',
  AMBER: '#fab219',
  RED: '#d03b3b',
}

function hexToRgb(hex: string): [number, number, number] {
  const n = parseInt(hex.slice(1), 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

// Same primary-ink tokens as palette.md's "Chart chrome & ink" table - not a separate eyeballed
// black/white pair.
const INK_ON_LIGHT = '#0b0b0b'
const INK_ON_DARK = '#ffffff'
// Perceptual (not linear) luminance threshold for picking readable text over an arbitrary fill -
// a heatmap cell's background can land anywhere along the ramp, so the ink color must follow it.
const READABLE_LUMINANCE_THRESHOLD = 150

export function readableTextColor(bgHex: string): string {
  const [r, g, b] = hexToRgb(bgHex)
  const luminance = 0.299 * r + 0.587 * g + 0.114 * b
  return luminance > READABLE_LUMINANCE_THRESHOLD ? INK_ON_LIGHT : INK_ON_DARK
}
