import { z, type ZodTypeAny } from 'zod'

// Mirrors backend com.strides.model.SampleTatDrilldownRow - Sample TAT's own record-level drill-down
// row (DPM_QC_Drilldown_Schema_v2.xlsx's "Sample TAT" row), the first KPI wired up to this pattern.
export const sampleTatDrilldownRowSchema = z.object({
  siteName: z.string(),
  sampleNumber: z.number(),
  batchCategory: z.string(),
  sampleType: z.string(),
  status: z.string(),
  sampledDate: z.string(),
  completedDate: z.string(),
  tatDays: z.coerce.number(),
})

/** Every KPI except Sample TAT's own bespoke row (see sampleTatDrilldownRowSchema) is served by the
 * backend's GenericDrilldownRepository, which returns a plain String-keyed row with no fixed Java
 * shape - a passthrough schema here matches that: each KPI's own DrilldownColumn.key/format reads
 * out of it narrowly (see drilldownColumns.ts), rather than this shared plumbing validating one
 * true row shape. */
export const genericDrilldownRowSchema = z.record(z.string(), z.unknown())

export interface DrilldownPage<T> {
  rows: T[]
  page: number
  pageSize: number
  totalRows: number
}

/** Mirrors backend com.strides.dto.DrilldownPage<T> - shared by every KPI's drill-down endpoint.
 * rowSchema varies per KPI (see drilldownColumns.ts's DRILLDOWN_CONFIG registry, keyed by kpi.id),
 * so callers only ever get Record<string, unknown> rows back - each KPI's own DrilldownColumn.key/
 * format reads out of that narrowly, rather than this shared plumbing needing one true row type. */
export function drilldownPageSchema(rowSchema: ZodTypeAny): z.ZodType<DrilldownPage<Record<string, unknown>>> {
  return z.object({
    rows: z.array(rowSchema),
    page: z.number(),
    pageSize: z.number(),
    totalRows: z.number(),
  }) as unknown as z.ZodType<DrilldownPage<Record<string, unknown>>>
}
