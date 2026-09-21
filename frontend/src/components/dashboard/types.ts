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
