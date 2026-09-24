import { z } from 'zod'

// Mirrors backend com.strides.dto.KpiStatus - fixed 3-value enum, nullable when a KPI's
// catalogue threshold has no matching column/scale to evaluate against (see backend/README.md).
export const kpiStatusSchema = z.enum(['RED', 'AMBER', 'GREEN']).nullable()
export type KpiStatus = z.infer<typeof kpiStatusSchema>

// Mirrors backend com.strides.dto.KpiThresholds - the KPI threshold config row this series is
// linked to: band boundaries the row's status was computed against (a null boundary = unbounded
// side; all null = no threshold yet). configKpiId is what the expanded chart uses to fetch the
// KPI's source system / description once (kpiThresholdConfigSchema) - not repeated per data row.
export const kpiThresholdsSchema = z.object({
  configKpiId: z.string(),
  direction: z.string(),
  redLow: z.coerce.number().nullable(),
  greenLow: z.coerce.number().nullable(),
  greenHigh: z.coerce.number().nullable(),
  redHigh: z.coerce.number().nullable(),
})
export type KpiThresholds = z.infer<typeof kpiThresholdsSchema>

// Mirrors backend com.strides.dto.KpiCardResponse - the one uniform shape every KPI GET
// endpoint returns, regardless of domain.
export const kpiCardResponseSchema = z.object({
  kpiId: z.string(),
  name: z.string(),
  value: z.coerce.number().nullable(),
  unit: z.string(),
  status: kpiStatusSchema,
  grain: z.string(),
  periodStart: z.string(),
  // Caveat the UI must surface as-is, e.g. departmentScope=QC's narrowed scope on Investigation
  // TAT (see backend/README.md's "Department scope" section) - null when a KPI has none.
  note: z.string().nullable(),
  // The two already-aggregated quantities `value` was divided from (e.g. on-time count / sample
  // count), for a KPI whose value is a rate/ratio - null for every non-ratio KPI, and also null
  // for a handful of ratio KPIs where the source table doesn't unambiguously identify the pair
  // (see backend InstrumentHealthService.getInstrumentFailureRate). Shown as a tooltip breakdown.
  numerator: z.coerce.number().nullable(),
  denominator: z.coerce.number().nullable(),
  // nullish, not nullable: a backend deployed before this field existed simply omits it.
  thresholds: kpiThresholdsSchema.nullish(),
})
export type KpiCardResponse = z.infer<typeof kpiCardResponseSchema>

export const kpiCardListSchema = z.array(kpiCardResponseSchema)

// The slice of GET /api/kpi-thresholds/{kpiId} (backend KpiThresholdConfigRow) the expanded chart
// shows; zod drops every other column.
export const kpiThresholdConfigSchema = z.object({
  kpiId: z.string(),
  sourceSystem: z.string().nullable(),
  description: z.string().nullable(),
})

// The 5 grains every agg_*/fact_* table is rolled up to (confirmed live against the dpm DB).
export const GRAINS = ['daily', 'weekly', 'monthly', 'quarterly', 'yearly'] as const
export type Grain = (typeof GRAINS)[number]

// Mirrors a KPI's {basePath}/{kpiPath}/filter-options response - one live-queried option list per
// extraFilter param name (see backend AbstractAggRepository.distinctValues / each repository's
// filterOptions method).
export const filterOptionsSchema = z.record(z.string(), z.array(z.string()))
export type FilterOptions = z.infer<typeof filterOptionsSchema>

// Mirrors GET /api/filters/sites - the union of every distinct site/site_name value across every
// agg_* table (see backend SiteOptionsRepository).
export const siteListSchema = z.array(z.string())
