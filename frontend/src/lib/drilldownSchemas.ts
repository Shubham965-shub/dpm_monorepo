import { z, type ZodTypeAny } from 'zod'

/** Every KPI's drill-down row is served by the backend's GenericDrilldownRepository, which returns
 * a plain String-keyed row with no fixed Java shape - a passthrough schema here matches that: each
 * KPI's own DrilldownColumn.key/format reads out of it narrowly (see drilldownColumns.ts), rather
 * than this shared plumbing validating one true row shape per KPI. */
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

/** Mirrors backend GenericDrilldownRepository.findColumnOptions - a column's own real distinct
 * values (jsonKey -> list), present only for its TEXT columns at or below
 * DrilldownSupport.MAX_DROPDOWN_DISTINCT_VALUES; a column missing here either has a
 * DrilldownColumn.filterType (a range control) or needs a free-text search instead of a dropdown. */
export const drilldownColumnOptionsSchema = z.record(z.string(), z.array(z.string()))
export type DrilldownColumnOptions = z.infer<typeof drilldownColumnOptionsSchema>
