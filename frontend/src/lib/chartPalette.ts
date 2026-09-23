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
const GREEN_GRADIENT_DARK: [number, number, number] = [6, 95, 70] // emerald-800

function rgbToHex([r, g, b]: [number, number, number]): string {
  return `#${[r, g, b].map((v) => Math.round(v).toString(16).padStart(2, '0')).join('')}`
}

/** `count` evenly-spaced shades from light to dark green, index 0 = lightest. Used to color an
 * ordered set of tiers (e.g. a priority breakdown P4..P1) on one consistent gradient regardless of
 * how many tiers actually exist, rather than a fixed palette sized for exactly 4. */
export function greenGradient(count: number): string[] {
  if (count <= 1) return [rgbToHex(GREEN_GRADIENT_DARK)]
  return Array.from({ length: count }, (_, i) => {
    const t = i / (count - 1)
    return rgbToHex([
      GREEN_GRADIENT_LIGHT[0] + (GREEN_GRADIENT_DARK[0] - GREEN_GRADIENT_LIGHT[0]) * t,
      GREEN_GRADIENT_LIGHT[1] + (GREEN_GRADIENT_DARK[1] - GREEN_GRADIENT_LIGHT[1]) * t,
      GREEN_GRADIENT_LIGHT[2] + (GREEN_GRADIENT_DARK[2] - GREEN_GRADIENT_LIGHT[2]) * t,
    ])
  })
}

// Status palette (fixed, never themed) - mirrors backend KpiStatus (RED/AMBER/GREEN).
export const STATUS_COLORS: Record<'GREEN' | 'AMBER' | 'RED', string> = {
  GREEN: '#0ca30c',
  AMBER: '#fab219',
  RED: '#d03b3b',
}
