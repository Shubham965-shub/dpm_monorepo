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

// Status palette (fixed, never themed) - mirrors backend KpiStatus (RED/AMBER/GREEN).
export const STATUS_COLORS: Record<'GREEN' | 'AMBER' | 'RED', string> = {
  GREEN: '#0ca30c',
  AMBER: '#fab219',
  RED: '#d03b3b',
}
